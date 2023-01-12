/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.api.models.factories;

import android.content.Context;

import androidx.annotation.WorkerThread;

import com.proton.gopenpgp.armor.Armor;
import com.squareup.inject.assisted.Assisted;
import com.squareup.inject.assisted.AssistedInject;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.models.ContactEncryptedData;
import ch.protonmail.android.api.models.MailSettings;
import ch.protonmail.android.api.models.PublicKeyBody;
import ch.protonmail.android.api.models.PublicKeyResponse;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.enumerations.MIMEType;
import ch.protonmail.android.api.models.enumerations.PackageType;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.crypto.Crypto;
import ch.protonmail.android.crypto.UserCrypto;
import ch.protonmail.android.data.local.ContactDao;
import ch.protonmail.android.data.local.ContactDatabase;
import ch.protonmail.android.data.local.model.ContactEmail;
import ch.protonmail.android.data.local.model.FullContactDetails;
import ch.protonmail.android.data.local.model.FullContactDetailsResponse;
import ch.protonmail.android.domain.entity.user.Address;
import ch.protonmail.android.domain.entity.user.AddressKey;
import ch.protonmail.android.domain.entity.user.Addresses;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.VCardUtil;
import ch.protonmail.android.utils.crypto.KeyInformation;
import ch.protonmail.android.utils.crypto.TextVerificationResult;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Key;
import ezvcard.property.RawProperty;
import me.proton.core.domain.entity.UserId;

public class SendPreferencesFactory {

    private final ProtonMailApiManager mApi;
    private final UserManager mUserManager;
    private final MailSettings mailSettings;
    private final UserCrypto crypto;
    private final ContactDao contactDao;
    private final UserId userId;

    @AssistedInject
    public SendPreferencesFactory(
            Context context,
            ProtonMailApiManager api,
            UserManager userManager,
            @Assisted UserId userId
    ) {
        this.mApi = api;
        this.mUserManager = userManager;
        this.userId = userId;
        this.mailSettings = userManager.getMailSettingsBlocking(userId);
        this.crypto = Crypto.forUser(userManager, userId);
		this.contactDao = ContactDatabase.Companion.getInstance(context, userId).getDao();
	}

    @AssistedInject.Factory
    public interface Factory {
        SendPreferencesFactory create(UserId userId);
    }

    @WorkerThread
    public Map<String, SendPreference> fetch(List<String> emails) throws Exception {
        Map<String, PublicKeyResponse> keyMap = getPublicKeys(emails);
        Map<String, FullContactDetails> contactDetailsMap = getContactDetails(emails);

        Map<String, SendPreference> resultMap = new HashMap<>(emails.size());
        for (String email : emails) {
            PublicKeyResponse pubKeyResp = keyMap.get(email);
            FullContactDetails fullContDetails = contactDetailsMap.get(email);
            resultMap.put(email, buildPreferences(email, pubKeyResp, fullContDetails));
        }
        return resultMap;
    }

    @WorkerThread
    private Map<String, PublicKeyResponse> getPublicKeys(List<String> emails) throws Exception {
        Map<String, PublicKeyResponse> keyMap = new HashMap<>();
        List<String> unknownEmails = new ArrayList<>();
        for (String email : emails) {
            Address address = getAddress(email);
            if (address != null) {
                keyMap.put(email, toPublicKeyResponse(address));
                continue;
            }
            unknownEmails.add(email);
        }
        keyMap.putAll(mApi.getPublicKeys(unknownEmails));
        return keyMap;
    }

    private PublicKeyResponse toPublicKeyResponse(Address address) {
        List<PublicKeyBody> sendbodies = new ArrayList<>();
        for (AddressKey key : address.getKeys().getKeys()) {
                sendbodies.add(new PublicKeyBody(key.buildBackEndFlags(), crypto.buildArmoredPublicKeyOrNull(key.getPrivateKey())));
        }
        return new PublicKeyResponse(PublicKeyResponse.RecipientType.INTERNAL.getValue(),
                "text/html" ,
                sendbodies.toArray(new PublicKeyBody[0]));
    }

    @WorkerThread
    private Map<String, FullContactDetails> getContactDetails(List<String> emails) {
        Map<String, String> contactIDs = new HashMap<>();
        for (String email : emails) {
            Address address = getAddress(email);
            ContactEmail contactEmail = contactDao.findContactEmailByEmailBlocking(email);
            if (address != null || contactEmail == null ) {
                continue;
            }
            contactIDs.put(email, contactEmail.getContactId());
        }
        Map<String, FullContactDetailsResponse> contactDetails = null;
        try {
            contactDetails = mApi.fetchContactDetailsBlocking(contactIDs.values());
        } catch (Exception e) {
            //noop
        }

        Map<String, FullContactDetails> result = new HashMap<>();
        for (String email : emails) {
            String contactID = contactIDs.get(email);
            if (contactID == null) {
                result.put(email, null);
                continue;
            }
            FullContactDetails contact = contactDetails.get(contactID).getContact();
            contactDao.insertFullContactDetailsBlocking(contact);
            result.put(email, contact);
        }
        return result;
    }

    private SendPreference buildPreferences(String email, PublicKeyResponse pubKeyResp, FullContactDetails fullContDetails) {
        if (fullContDetails != null) {
            try {
                return buildFromContact(email, pubKeyResp, fullContDetails);
            } catch (Exception e) {
                Logger.doLogException(e);
            }
        }
        return buildUsingDefaults(email, pubKeyResp);
    }

    protected SendPreference buildFromContact(String email, PublicKeyResponse pubKeyResp, FullContactDetails fullContactDetails) throws Exception {
        boolean isInternal = pubKeyResp.getRecipientType() == PublicKeyResponse.RecipientType.INTERNAL;
        Triple<VCard, VCard, Boolean> triple = parseVCard(fullContactDetails);
        VCard clear = triple.getLeft();
        VCard signed = triple.getMiddle();
        boolean isVerified = triple.getRight();

        String group = null;
        try {
            group = VCardUtil.getGroup(clear, signed, email);
        } catch (Exception e) {
            // noop
        }
        if (group == null || group.length() == 0) {
            return buildUsingDefaults(email, pubKeyResp);
        }
        RawProperty signFlag = VCardUtil.findProperty(signed, "x-pm-sign", group);
        RawProperty mimeProp = VCardUtil.findProperty(signed, "x-pm-mimetype", group);

        List<String> contactKeys = getKeys(signed, group);
        boolean hasPinnedKeys = contactKeys.size() > 0;
        // pinnedEncryptionKey is null when sending cleartext or key pinning is enabled but no pinned key is allowed for sending
        String pinnedEncryptionKey = hasPinnedKeys ? findPinnedEncryptionKey(contactKeys, pubKeyResp) : null;
        boolean isEncryptionKeyPinned = pinnedEncryptionKey != null;
        String encryptionKey = pinnedEncryptionKey == null && pubKeyResp.getKeys().length > 0 ? pubKeyResp.getKeys()[0].getPublicKey() : pinnedEncryptionKey;

        boolean encrypt = false;
        boolean sign = mailSettings.getDefaultSign();
        if (signFlag != null){
            sign = !signFlag.getValue().equalsIgnoreCase("false");
        }
        if (isInternal) {
            // Internal keys -> encrypt and sign by default
            encrypt = true;
            sign = true;
        } else  {
            if(isEncryptionKeyPinned){
                // Pinned keys -> look at the flag
                /*
                If flag is not specified in the contact, we encrypt and sign by default
                This is needed for pinned wkd keys because of a bug in contact creation:
                https://jira.protontech.ch/browse/MAILWEB-3305
                 */
                RawProperty encryptFlag = VCardUtil.findProperty(signed, "x-pm-encrypt", group);
                encrypt = encryptFlag == null || !encryptFlag.getValue().equalsIgnoreCase("false");
            } else if(encryptionKey != null) {
                RawProperty useUntrustedKeyFlag =
                        VCardUtil.findProperty(signed, "x-pm-encrypt-untrusted", group);
                /*
                WKD keys:
                encrypt by default, except if the contact has a flag to disable untrusted keys
                 */
                encrypt = useUntrustedKeyFlag == null ||
                        !useUntrustedKeyFlag.getValue().equalsIgnoreCase("false");
            }
        }
        // always sign when encrypting
        sign = sign || encrypt;

        RawProperty schemeProp = VCardUtil.findProperty(signed, "x-pm-scheme", group);
        String schemeString = schemeProp == null ? null : schemeProp.getValue();
        if (schemeString == null) {
            schemeString = mailSettings.getPGPScheme() == PackageType.PGP_MIME ? "pgp-mime" : "pgp-inline";
        }
        PackageType scheme = getEncryption(pubKeyResp, schemeString, encrypt, sign);
        MIMEType mimeType = getMimeType(mimeProp != null ? mimeProp.getValue() : null, scheme, sign);
        boolean isOwnAddress = getAddress(email) != null;

        return new SendPreference(email, encrypt, sign, mimeType, encryptionKey, scheme,
                isEncryptionKeyPinned, hasPinnedKeys, isVerified, isOwnAddress);
    }

    private Address getAddress(String email) {
        ch.protonmail.android.domain.entity.user.User user = mUserManager.getUserBlocking(userId);
        Addresses addresses = user.getAddresses();
        for (Address address : addresses.sorted()) {
            if (email.equalsIgnoreCase(address.getEmail().getS())) {
                return address;
            }
        }
        return null;
    }

    private MIMEType getMimeType(String mimeString, PackageType scheme, boolean sign) {
        if (scheme == PackageType.PGP_MIME || scheme == PackageType.MIME) {
            return MIMEType.MIME;
        }
        if (scheme == PackageType.PGP_INLINE || (PackageType.CLEAR == scheme && sign)) {
            return MIMEType.PLAINTEXT;
        }
        if (mimeString != null) {
            return mimeString.equalsIgnoreCase(MIMEType.PLAINTEXT.toString()) ? MIMEType.PLAINTEXT : MIMEType.HTML;
        }
        return MIMEType.HTML;
    }

    private PackageType getEncryption(PublicKeyResponse pubKeyResp, String schemeString, boolean encrypt, boolean sign) {
        if (pubKeyResp.getRecipientType() == PublicKeyResponse.RecipientType.INTERNAL) {
            return PackageType.PM;
        }
        if (schemeString.equals("pgp-mime") && !encrypt && sign) {
            return PackageType.MIME;
        }
        if (schemeString.equals("pgp-mime") && encrypt) {
            return PackageType.PGP_MIME;
        }
        if (schemeString.equals("pgp-inline") && encrypt) {
            return PackageType.PGP_INLINE;
        }
        return PackageType.CLEAR;
    }

    /**
     * Return cleartext and signed parts of the contact data.
     * For signed part, also return whether the corresponding contact signature could be verified.
     */
    private Triple<VCard, VCard, Boolean> parseVCard(FullContactDetails fullContactDetails) {
        // Signed data must be there as we only call this function if default = 0
        String signedData = "";
        String clearData = null;
        boolean isContactSignatureVerified = false;
        List<ContactEncryptedData> cards = fullContactDetails.getEncryptedData();
        for (ContactEncryptedData card : cards) {
            switch (card.getEncryptionType()) {
                case SIGNED:
                    TextVerificationResult tvr = crypto.verify(card.getData(), card.getSignature());
                    signedData = tvr.getData();
                    isContactSignatureVerified = tvr.isSignatureValid();
                    break;
                case CLEARTEXT:
                    clearData = card.getData();
                    break;
            }
        }


        VCard signed = Ezvcard.parse(signedData).first();
        VCard clear = clearData == null ? new VCard() : Ezvcard.parse(clearData).first();
        return new ImmutableTriple<>(clear, signed, isContactSignatureVerified);
    }

    private List<String> getKeys(VCard vCard, String group) throws Exception {
        List<Key> keyProps = vCard.getKeys();
        List<String> publicKeys = new ArrayList<>();
        for (Key key : keyProps) {
            if (!key.getGroup().equalsIgnoreCase(group)) {
                continue;
            }
            publicKeys.add(Armor.armorKey(key.getData()));
        }
        return publicKeys;
    }

    private String findPrimaryKey(PublicKeyResponse pubKeyResp) {
        if (pubKeyResp == null) {
            return null;
        }
        PublicKeyBody[] keys = pubKeyResp.getKeys();
        if (keys == null) {
            return null;
        }
        for (PublicKeyBody body : pubKeyResp.getKeys()) {
            if (body.isAllowedForSending()) {
                return body.getPublicKey();
            }
        }
        return null;
    }

    /**
     * @return a pinned key that can be used for sending.
     * For internal recipients, we only consider pinned keys that match (by fingerprint) those fetched from the API.
     */
    private String findPinnedEncryptionKey(List<String> pinnedKeys, PublicKeyResponse pubKeyResp) {
        List<String> apiFingerprints = getSendFingerprints(pubKeyResp.getKeys());
        for (String key : pinnedKeys) {
            KeyInformation ki = crypto.deriveKeyInfo(key);
            if (!ki.isValid() || ki.isExpired() || !ki.canEncrypt()) {
                continue;
            }
            // TODO: Compare full binary keys, fingerprints is not enough to detect subkey changes
            if ((pubKeyResp.getRecipientType() == PublicKeyResponse.RecipientType.INTERNAL || !apiFingerprints.isEmpty()) &&
                !apiFingerprints.contains(ki.getFingerprint())) {
                continue;
            }
            return key;
        }
        return null;
    }

    private List<String> getSendFingerprints(PublicKeyBody[] bodies) {
        List<String> fingerprints = new ArrayList<>();
        for (PublicKeyBody body : bodies) {
            if (body.isAllowedForSending()) {
                KeyInformation ki = crypto.deriveKeyInfo(body.getPublicKey());
                fingerprints.add(ki.getFingerprint());
            }
        }
        return fingerprints;
    }

    private SendPreference buildUsingDefaults(String email, PublicKeyResponse pubKeyResp) {
        boolean isInternal = pubKeyResp.getRecipientType() == PublicKeyResponse.RecipientType.INTERNAL;
        boolean isOwnAddress = getAddress(email) != null;
        String pubKey = findPrimaryKey(pubKeyResp);
        PackageType defaultPGPScheme = mailSettings.getPGPScheme();
        if (pubKey != null) {
            if(isInternal){
                return new SendPreference(email, true, true,
                        MIMEType.HTML, pubKey, PackageType.PM,
                        false, false, false, isOwnAddress);
            } else {
                MIMEType mimeType = getMimeType(
                        null,
                        defaultPGPScheme,
                        true
                );
                return new SendPreference(email, true, true,
                        mimeType, pubKey, defaultPGPScheme,
                        false, false, false, isOwnAddress);
            }
        }
        boolean globalSign = mailSettings.getDefaultSign();
        defaultPGPScheme = defaultPGPScheme == PackageType.PGP_MIME ? PackageType.MIME : PackageType.CLEAR;
        MIMEType defaultPGPMime = defaultPGPScheme == PackageType.MIME ? MIMEType.MIME : MIMEType.PLAINTEXT;
        return new SendPreference(email, false, globalSign,
                globalSign ? defaultPGPMime : MIMEType.HTML, null,
                globalSign ? defaultPGPScheme : PackageType.CLEAR,
                false, false, false, false);
    }
}
