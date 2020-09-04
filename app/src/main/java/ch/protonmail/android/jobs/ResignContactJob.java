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
import ch.protonmail.android.api.models.room.contacts.ContactEmail;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.contacts.FullContactDetails;
import ch.protonmail.android.api.models.room.contacts.server.FullContactDetailsResponse;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.crypto.UserCrypto;
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
        ContactsDatabase contactsDatabase= ContactsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        User user = mUserManager.getUser();
        String contactId = getContactId(contactsDatabase, mContactEmail);
        if (contactId == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }
        FullContactDetails fullContactDetails =contactsDatabase.findFullContactDetailsById(contactId);

        if (user == null || fullContactDetails == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }
        ContactEncryptedData signedCard = getCardByType(fullContactDetails.getEncryptedData(), ContactEncryption.SIGNED);

        if (signedCard == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }

        if (!mQueueNetworkUtil.isConnected()) {
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
        ContactsDatabase contactsDatabase = ContactsDatabaseFactory.Companion.getInstance(
                getApplicationContext()).getDatabase();
        String contactId = getContactId(contactsDatabase, mContactEmail);
        if (contactId == null) {
            AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ERROR, mDestination));
            return;
        }

        FullContactDetails fullContactDetails = contactsDatabase.findFullContactDetailsById(
                contactId);
        UserCrypto crypto = Crypto.forUser(mUserManager, mUserManager.getUsername());
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
        contactsDatabase.insertFullContactDetails(fullContactDetails);

        ContactEncryptedData encCard = getCardByType(fullContactDetails.getEncryptedData(), ContactEncryption.ENCRYPTED_AND_SIGNED);
        CreateContactV2BodyItem body;

        if (encCard == null) {
            body = new CreateContactV2BodyItem(signedCard.getData(), signedCard.getSignature(), null, null);
        } else {
            body = new CreateContactV2BodyItem(signedCard.getData(), signedCard.getSignature(), encCard.getData(), encCard.getSignature());
        }

        mApi.updateContact(contactId, body);
        FullContactDetailsResponse response = mApi.updateContact(contactId, body);

        if (response != null) {
            if (response.getCode() == RESPONSE_CODE_ERROR_EMAIL_EXIST) {
                AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.ALREADY_EXIST, mDestination));
            } else if (response.getCode() == RESPONSE_CODE_ERROR_INVALID_EMAIL) {
                AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.INVALID_EMAIL, mDestination));
            } else if (response.getCode() == RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED) {
                AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.DUPLICATE_EMAIL, mDestination));
            } else {
                //TODO this insert is probably not needed as it is already saved some lines above
                contactsDatabase.insertFullContactDetails(fullContactDetails);
                AppUtil.postEventOnUi(new ResignContactEvent(mSendPreference, ContactEvent.SUCCESS, mDestination));
            }
        }
    }
    //TODO move database to receiver after kotlin
    private String getContactId(ContactsDatabase contactsDatabase, String contactEmailText) {
        ContactEmail contactEmail =  contactsDatabase.findContactEmailByEmail(contactEmailText);
        if (contactEmail == null) {
            return null;
        }
        return contactEmail.getContactId();
    }

}
