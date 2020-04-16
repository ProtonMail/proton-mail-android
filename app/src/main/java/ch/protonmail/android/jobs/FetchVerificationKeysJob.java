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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.models.PublicKeyBody;
import ch.protonmail.android.api.models.PublicKeyResponse;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.enumerations.KeyFlag;
import ch.protonmail.android.api.models.room.contacts.ContactEmail;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory;
import ch.protonmail.android.api.models.room.contacts.FullContactDetails;
import ch.protonmail.android.api.models.room.contacts.server.FullContactDetailsResponse;
import ch.protonmail.android.core.Constants;
import ch.protonmail.android.events.FetchVerificationKeysEvent;
import ch.protonmail.android.events.Status;
import ch.protonmail.android.utils.AppUtil;
import ch.protonmail.android.utils.crypto.Crypto;
import ch.protonmail.android.utils.crypto.KeyInformation;
import ch.protonmail.android.utils.crypto.UserCrypto;

public class FetchVerificationKeysJob extends ProtonMailBaseJob {

    private String email;
    private boolean mRetry;

    private FetchVerificationKeysJob(String email, boolean retry) {
        super(new Params(Priority.HIGH).requireNetwork().groupBy(Constants.JOB_GROUP_MISC).removeTags().persist());
        mRetry = retry;
        this.email = email;
    }

    public FetchVerificationKeysJob(String email) {
        this(email, false);
    }

    @Override
    public void onRun() throws Throwable {
        ContactsDatabase contactsDatabase = ContactsDatabaseFactory.Companion.getInstance(getApplicationContext()).getDatabase();
        UserCrypto crypto = Crypto.forUser(mUserManager, mUserManager.getUsername());
        for (Address address : mUserManager.getUser().getAddresses()) {
            if (address.getEmail().equals(email)) {
                List<KeyInformation> publicKeys = new ArrayList<>();
                for (Keys key : address.getKeys()) {
                    KeyInformation keyInfo = crypto.deriveKeyInfo(crypto.getArmoredPublicKey(key));
                    if (!KeyFlag.fromInteger(key.getFlags()).contains(KeyFlag.VERIFICATION_ENABLED)) {
                        keyInfo.flagAsCompromised();
                    }
                    publicKeys.add(keyInfo);
                }
                AppUtil.postEventOnUi(new FetchVerificationKeysEvent(Status.SUCCESS, publicKeys, mRetry));
                return;
            }
        }
		ContactEmail contactEmail = contactsDatabase.findContactEmailByEmail(email);
        if (contactEmail == null) {
            // turned off
            AppUtil.postEventOnUi(new FetchVerificationKeysEvent(Status.SUCCESS, Collections.emptyList(), mRetry));
            return;
        }
        FullContactDetailsResponse contRespons = mApi.fetchContactDetails(contactEmail.getContactId());
        FullContactDetails fullContactDetails = contRespons.getContact();
        contactsDatabase.insertFullContactDetails(fullContactDetails);
        List<String> trustedKeys = fullContactDetails.getPublicKeys(crypto, email);
        PublicKeyResponse response = mApi.getPublicKeys(email);
        if (response.hasError()) {
            throw new Exception(response.getError());
        }
        List<KeyInformation> verKeys = filterVerificationKeys(crypto, Arrays.asList(response.getKeys()), trustedKeys);
        AppUtil.postEventOnUi(new FetchVerificationKeysEvent(Status.SUCCESS, verKeys, mRetry));
    }

    private List<KeyInformation> filterVerificationKeys(UserCrypto crypto, List<PublicKeyBody> publicKeyBodies, List<String> trustedKeys) {
        // ArrayList is faster for our usage than using a set.
        Collection<String> bannedFingerprints = new ArrayList<>();
        for (PublicKeyBody body : publicKeyBodies) {
            if (!body.isAllowedForVerification()) {
                KeyInformation keyInfo = crypto.deriveKeyInfo(body.getPublicKey());
                bannedFingerprints.add(keyInfo.getFingerprint());
            }
        }

        List<KeyInformation> keys = new ArrayList<>();
        for(String pubKey : trustedKeys) {
            KeyInformation keyInfo = crypto.deriveKeyInfo(pubKey);
            if (bannedFingerprints.contains(keyInfo.getFingerprint())) {
                keyInfo.flagAsCompromised();
            }
            keys.add(keyInfo);
        }
        return Collections.unmodifiableList(keys);
    }

}
