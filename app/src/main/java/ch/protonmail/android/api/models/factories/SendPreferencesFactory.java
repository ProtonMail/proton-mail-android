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
package ch.protonmail.android.api.models.factories;

import androidx.annotation.WorkerThread;

import com.proton.gopenpgp.armor.Armor;

import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.models.ContactEncryptedData;
import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.models.MailSettings;
import ch.protonmail.android.api.models.PublicKeyBody;
import ch.protonmail.android.api.models.PublicKeyResponse;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.User;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.api.models.enumerations.MIMEType;
import ch.protonmail.android.api.models.enumerations.PackageType;
import ch.protonmail.android.api.models.room.contacts.ContactEmail;
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase;
import ch.protonmail.android.api.models.room.contacts.FullContactDetails;
import ch.protonmail.android.api.models.room.contacts.server.FullContactDetailsResponse;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.VCardUtil;
import ch.protonmail.android.utils.crypto.Crypto;
import ch.protonmail.android.utils.crypto.KeyInformation;
import ch.protonmail.android.utils.crypto.TextDecryptionResult;
import ch.protonmail.android.utils.crypto.UserCrypto;
import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.property.Key;
import ezvcard.property.RawProperty;

public class SendPreferencesFactory {

    private ProtonMailApiManager mApi;
    private UserManager mUserManager;
    private MailSettings mailSettings;
    private UserCrypto crypto;
    private final ContactsDatabase contactsDatabase;
    private String username;

    @Inject
    public SendPreferencesFactory(
            ProtonMailApiManager api,
            UserManager userManager,
            @Named("username" /* TODO: use `CURRENT_USERNAME` const, unsupported in Java */) String username,
            MailSettings mailSettings,
            ContactsDatabase contactsDatabase
    ) {
        this.mApi = api;
        this.mUserManager = userManager;
        this.username = username;
        this.mailSettings = mailSettings;
        this.crypto = Crypto.forUser(userManager, username);
		this.contactsDatabase=contactsDatabase;
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

    private PublicKeyResponse toPublicKeyResponse(Address alias) {
        List<PublicKeyBody> sendbodies = new ArrayList<>();
        List<PublicKeyBody> verbodies = new ArrayList<>();
        for (Keys key : alias.getKeys()) {
            if (crypto.isAllowedForSending(key)) {
                sendbodies.add(new PublicKeyBody(key.getFlags(), crypto.getArmoredPublicKey(key)));
            } else {
                verbodies.add(new PublicKeyBody(key.getFlags(), crypto.getArmoredPublicKey(key)));
            }
        }
        sendbodies.addAll(verbodies);
        return new PublicKeyResponse(PublicKeyResponse.RecipientType.INTERNAL.getValue(),
                "text/html" ,
                sendbodies.toArray(new PublicKeyBody[0]));
    }

    @WorkerThread
    private Map<String, FullContactDetails> getContactDetails(List<String> emails) {
        Map<String, String> contactIDs = new HashMap<>();
        for (String email : emails) {
            Address address = getAddress(email);
            ContactEmail contactEmail = contactsDatabase.findContactEmailByEmail(email);
            if (address != null || contactEmail == null ) {
                continue;
            }
            contactIDs.put(email, contactEmail.getContactId());
        }
        Map<String, FullContactDetailsResponse> contactDetails = null;
        try {
            contactDetails = mApi.fetchContactDetails(contactIDs.values());
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
            contactsDatabase.insertFullContactDetails(contact);
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

    private SendPreference buildFromContact(String email, PublicKeyResponse pubKeyResp, FullContactDetails fullContactDetails) throws Exception {
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
        RawProperty encryptFlag = VCardUtil.findProperty(signed, "x-pm-encrypt", group);
        RawProperty signFlag = VCardUtil.findProperty(signed, "x-pm-sign", group);
        RawProperty mimeProp = VCardUtil.findProperty(signed, "x-pm-mime", group);

        List<String> contactKeys = getKeys(signed, group);
        // primaryKey is null when sending cleartext or key pinning is enabled but no pinned key is allowed for sending
        String primaryKey = findPrimaryKey(contactKeys, pubKeyResp);
        boolean primaryPinned = primaryKey != null;
        primaryKey = primaryKey == null && pubKeyResp.getKeys().length > 0 ? pubKeyResp.getKeys()[0].getPublicKey() : primaryKey;
        boolean encrypt = encryptFlag != null ? !encryptFlag.getValue().equalsIgnoreCase("false") : false;
        boolean sign = signFlag != null ? !signFlag.getValue().equalsIgnoreCase("false") : mailSettings.getDefaultSign();
        if (isInternal) {
            encrypt = true;
            sign = true;
        } else if (primaryKey == null) {
            encrypt = false;
        }
        // always sign when encrypting
        sign = sign || encrypt;

        boolean hasPinned = contactKeys.size() > 0 && primaryKey != null;

        RawProperty schemeProp = VCardUtil.findProperty(signed, "x-pm-scheme", group);
        String schemeString = schemeProp == null ? null : schemeProp.getValue();
        if (schemeString == null) {
            schemeString = mailSettings.getPGPScheme() == PackageType.PGP_MIME ? "pgp-mime" : "pgp-inline";
        }
        PackageType scheme = getEncryption(pubKeyResp, schemeString, encrypt, sign);
        MIMEType mimeType = getMimeType(mimeProp != null ? mimeProp.getValue() : null, scheme, sign);
        boolean isOwnAddress = getAddress(email) != null;

        return new SendPreference(email, encrypt, sign, mimeType, primaryKey, scheme,
                primaryPinned, hasPinned, isVerified, isOwnAddress);
    }

    private Address getAddress(String email) {
        User user = (username != null) ? mUserManager.getUser(username) : mUserManager.getUser();
        List<Address> addresses = user.getAddresses();
        for (Address address : addresses) {
            if (email.equalsIgnoreCase(address.getEmail())) {
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

    private Triple<VCard, VCard, Boolean> parseVCard(FullContactDetails fullContactDetails) {
        // Signed data must be there as we only call this function if default = 0
        String signedData = "";
        String clearData = null;
        boolean verified = true;

        List<ContactEncryptedData> cards = fullContactDetails.getEncryptedData();
        for (ContactEncryptedData card : cards) {
            switch (card.getEncryptionType()) {
                case SIGNED:
                    try {
                        TextDecryptionResult tdr = crypto.verify(card.getData(), card.getSignature());
                        signedData = tdr.getDecryptedData();
                        verified = tdr.isSignatureValid();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case CLEARTEXT:
                    clearData = card.getData();
                    break;
            }
        }


        VCard signed = Ezvcard.parse(signedData).first();
        VCard clear = clearData == null ? new VCard() : Ezvcard.parse(clearData).first();
        return new ImmutableTriple<>(clear, signed, verified);
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

    private String findPrimaryKey(List<String> keys, PublicKeyResponse pubKeyResp) throws Exception {
        if (keys.size() == 0) {
            return findPrimaryKey(pubKeyResp);
        }
        List<String> fingerprints = getSendFingerprints(pubKeyResp.getKeys());
        for (String key : keys) {
            KeyInformation ki = crypto.deriveKeyInfo(key);
            if (ki.isValid() && ki.isExpired()) {
                continue;
            }
            if (pubKeyResp.getRecipientType() == PublicKeyResponse.RecipientType.INTERNAL &&
                !fingerprints.contains(ki.getFingerprint())) {
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

        if (isInternal && pubKey != null) {
            return new SendPreference(email, true, true,
                    MIMEType.HTML, pubKey, PackageType.PM,
                    true, false, true, isOwnAddress);
        }
        PackageType defaultPGPScheme = mailSettings.getPGPScheme();
        defaultPGPScheme = defaultPGPScheme == PackageType.PGP_MIME ? PackageType.MIME : PackageType.CLEAR;
        MIMEType defaultPGPMime = defaultPGPScheme == PackageType.MIME ? MIMEType.MIME : MIMEType.PLAINTEXT;
        boolean globalSign = mailSettings.getDefaultSign();

        return new SendPreference(email, false, globalSign,
                globalSign ? defaultPGPMime : MIMEType.HTML, null,
                globalSign ? defaultPGPScheme : PackageType.CLEAR,
                true, false, true, false);
    }
}
