/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.jobs;

import com.birbit.android.jobqueue.Params;

import java.util.List;

import ch.protonmail.android.api.models.ContactEncryptedData;
import ch.protonmail.android.api.models.CreateContactV2BodyItem;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.enumerations.ContactEncryption;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.crypto.UserCrypto;
import ch.protonmail.android.data.local.ContactDao;
import ch.protonmail.android.data.local.ContactDatabase;
import ch.protonmail.android.data.local.model.ContactEmail;
import ch.protonmail.android.data.local.model.FullContactDetails;
import ch.protonmail.android.data.local.model.FullContactDetailsResponse;
import ch.protonmail.android.events.ContactEvent;
import ch.protonmail.android.events.ResignContactEvent;
import ch.protonmail.android.jobs.contacts.GetSendPreferenceJob;
import ch.protonmail.android.utils.AppUtil;

import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED;
import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_ERROR_EMAIL_EXIST;
import static ch.protonmail.android.api.segments.BaseApiKt.RESPONSE_CODE_ERROR_INVALID_EMAIL;

public class ResignContactJob extends ProtonMailEndlessJob {

	private final String mContactEmail;
    private final SendPreference mSendPreference;
    private final GetSendPreferenceJob.Destination mDestination;

    public ResignContactJob(String contactEmail, SendPreference sendPreference, GetSendPreferenceJob.Destination destination) {
        super(new Params(Priority.MEDIUM).requireNetwork().persist().groupBy(Constants.JOB_GROUP_CONTACT));
		mContactEmail = contactEmail;
        mSendPreference = sendPreference;
        mDestination = destination;
    }

    @Override
    public void onAdded() {
        ContactDao contactDao = ContactDatabase.Companion
                .getInstance(getApplicationContext(), getUserId())
                .getDao();
        User user = getUserManager().getUser();
        String contactId = getContactId(contactDao, mContactEmail);
        if (contactId == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }
        FullContactDetails fullContactDetails = contactDao.findFullContactDetailsById(contactId);

        if (user == null || fullContactDetails == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }
        ContactEncryptedData signedCard = getCardByType(fullContactDetails.getEncryptedData(), ContactEncryption.SIGNED);

        if (signedCard == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }

        if (!getQueueNetworkUtil().isConnected()) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.NO_NETWORK, mDestination));
        }
    }

    private ContactEncryptedData getCardByType(List<ContactEncryptedData> cards, ContactEncryption type) {
        for (ContactEncryptedData card : cards) {
            if (card.getEncryptionType() == type) {
                return card;
            }
        }
        return null;
    }


    @Override
    public void onRun() throws Throwable {
        ContactDao contactDao = ContactDatabase.Companion
                .getInstance(getApplicationContext(), getUserId()).getDao();
        String contactId = getContactId(contactDao, mContactEmail);
        if (contactId == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }

        FullContactDetails fullContactDetails = contactDao.findFullContactDetailsById(
                contactId);
        UserCrypto crypto = Crypto.forUser(getUserManager(), getUserManager().requireCurrentUserId());
        ContactEncryptedData signedCard = getCardByType(fullContactDetails.getEncryptedData(), ContactEncryption.SIGNED);

        if (signedCard == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }

        try {
            signedCard.setSignature(crypto.sign(signedCard.getData()));
        } catch (Exception e) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }
        contactDao.insertFullContactDetails(fullContactDetails);

        ContactEncryptedData encCard = getCardByType(fullContactDetails.getEncryptedData(), ContactEncryption.ENCRYPTED_AND_SIGNED);
        CreateContactV2BodyItem body;

        if (encCard == null) {
            body = new CreateContactV2BodyItem(signedCard.getData(), signedCard.getSignature(), null, null);
        } else {
            body = new CreateContactV2BodyItem(signedCard.getData(), signedCard.getSignature(), encCard.getData(), encCard.getSignature());
        }

        getApi().updateContact(contactId, body);
        FullContactDetailsResponse response = getApi().updateContact(contactId, body);

        if (response != null) {
            if (response.getCode() == RESPONSE_CODE_ERROR_EMAIL_EXIST) {
                AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ALREADY_EXIST, mDestination));
            } else if (response.getCode() == RESPONSE_CODE_ERROR_INVALID_EMAIL) {
                AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.INVALID_EMAIL, mDestination));
            } else if (response.getCode() == RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED) {
                AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.DUPLICATE_EMAIL, mDestination));
            } else {
                //TODO this insert is probably not needed as it is already saved some lines above
                contactDao.insertFullContactDetails(fullContactDetails);
                AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.SUCCESS, mDestination));
            }
        }
    }
    //TODO move database to receiver after kotlin
    private String getContactId(ContactDao contactDao, String contactEmailText) {
        ContactEmail contactEmail =  contactDao.findContactEmailByEmail(contactEmailText);
        if (contactEmail == null) {
            return null;
        }
        return contactEmail.getContactId();
    }

}
