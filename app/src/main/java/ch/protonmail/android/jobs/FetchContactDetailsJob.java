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
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.contacts.FullContactDetails;
import ch.protonmail.android.api.models.room.contacts.server.FullContactDetailsResponse;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.crypto.CipherText;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.crypto.UserCrypto;
import ch.protonmail.android.events.ContactDetailsFetchedEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;

public class FetchContactDetailsJob extends ProtonMailBaseJob {

    private String contactId;

    public FetchContactDetailsJob(String contactId) {
        super(new Params(Priority.MEDIUM).persist().groupBy(Constants.JOB_GROUP_CONTACT));
        this.contactId = contactId;
    }

    @Override
    public void onRun() throws Throwable {
        ContactsDatabase contactsDatabase= ContactsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        FullContactDetails contact = contactsDatabase.findFullContactDetailsById(contactId);
        checkAndParse(contact);
        if (!mQueueNetworkUtil.isConnected()) {
            return;
        } else {
            FullContactDetailsResponse response = mApi.fetchContactDetails(contactId);
            contact = response.getContact();
            contactsDatabase.insertFullContactDetails(contact);
            checkAndParse(contact);
        }
    }

    private void checkAndParse(FullContactDetails contact) throws Exception {
        if (contact != null) {
            parseVCard(contact);
        }
    }

    private void parseVCard(FullContactDetails contact) throws Exception {
        List<ContactEncryptedData> encData = contact.getEncryptedData();
        UserCrypto crypto = Crypto.forUser(mUserManager, mUserManager.getUsername());

        String decryptedVCardType0 = "";
        String decryptedVCardType2 = "";
        String decryptedVCardType3 = "";

        String vCardType2Signature = "";
        String vCardType3Signature = "";
        if (encData != null && encData.size() > 0) {
            for (ContactEncryptedData contactEncryptedData : encData) {
                if (Constants.VCardType.Companion.fromInt(contactEncryptedData.getType()) == Constants.VCardType.SIGNED_ENCRYPTED) {
                    CipherText tct = new CipherText(contactEncryptedData.getData());
                    decryptedVCardType3 = crypto.decrypt(tct).getDecryptedData();
                    vCardType3Signature = contactEncryptedData.getSignature();
                } else if (Constants.VCardType.Companion.fromInt(contactEncryptedData.getType()) == Constants.VCardType.SIGNED) {
                    decryptedVCardType2 = contactEncryptedData.getData();
                    vCardType2Signature = contactEncryptedData.getSignature();
                } else if (Constants.VCardType.Companion.fromInt(contactEncryptedData.getType()) == Constants.VCardType.UNSIGNED) {
                    decryptedVCardType0 = contactEncryptedData.getData();
                }
            }
        }
        AppUtil.postEventOnUi(new ContactDetailsFetchedEvent(Status.SUCCESS, decryptedVCardType0, decryptedVCardType2, decryptedVCardType3, vCardType2Signature, vCardType3Signature));
    }
}
