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

import android.util.Base64;

import androidx.annotation.NonNull;

import org.apache.commons.lang3.ObjectUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import ch.protonmail.android.api.ProtonMailApiManager;
import ch.protonmail.android.api.models.Auth;
import ch.protonmail.android.api.models.MessageRecipient;
import ch.protonmail.android.api.models.ModulusResponse;
import ch.protonmail.android.api.models.PasswordVerifier;
import ch.protonmail.android.api.models.SendPreference;
import ch.protonmail.android.api.models.enumerations.MIMEType;
import ch.protonmail.android.api.models.enumerations.PackageType;
import ch.protonmail.android.api.models.messages.send.MessageSendAddressBody;
import ch.protonmail.android.api.models.messages.send.MessageSendKey;
import ch.protonmail.android.api.models.messages.send.MessageSendPackage;
import ch.protonmail.android.api.models.room.messages.Attachment;
import ch.protonmail.android.api.models.room.messages.Message;
import ch.protonmail.android.crypto.AddressCrypto;
import ch.protonmail.android.crypto.CipherText;
import ch.protonmail.android.di.CurrentUsername;
import ch.protonmail.android.domain.entity.Id;
import ch.protonmail.android.domain.entity.Name;
import ch.protonmail.android.utils.HTMLToMDConverter;
import ch.protonmail.android.utils.MIME.MIMEBuilder;
import ch.protonmail.android.utils.crypto.EOToken;
import kotlin.text.Charsets;

public class PackageFactory {

    private final ProtonMailApiManager mApi;
    private final AddressCrypto.Factory addressCryptoFactory;
    private final String currentUsername;
    private final HTMLToMDConverter htmlToMDConverter;
    private AddressCrypto crypto;

    @Inject
    public PackageFactory(
            @NonNull ProtonMailApiManager mApi,
            @NonNull AddressCrypto.Factory addressCryptoFactory,
            @CurrentUsername String currentUsername,
            @NonNull HTMLToMDConverter htmlToMDConverter) {
        this.mApi = mApi;
        this.addressCryptoFactory = addressCryptoFactory;
        this.currentUsername = currentUsername;
        this.htmlToMDConverter = htmlToMDConverter;
    }

    public List<MessageSendPackage> generatePackages(
            @NonNull Message message,
            @NonNull List<SendPreference> preferences,
            @NonNull OutsidersPassword outsidersPassword
    ) throws Exception {
        final Map<MIMEType, MessageSendPackage> packageMap = new HashMap<>();
        crypto = addressCryptoFactory.create(
                new Id(message.getAddressID()),
                new Name(currentUsername)
        );

        Set<String> recipients = getMessageRecipients(message);
        for (SendPreference sendPref : preferences) {
            if (!recipients.contains(sendPref.getEmailAddress())) {
                continue;
            }
            MIMEType mime = getMIMEType(message, sendPref, outsidersPassword);
            if (!packageMap.containsKey(mime)) {
                packageMap.put(mime, generateTopLevelPackage(mime, message));
            }

            MessageSendPackage packageModel = packageMap.get(mime);
            addAddress(packageModel, sendPref, message, outsidersPassword);
        }
        // This is apparently even faster than filling in an array matching the map size.
        return Arrays.asList(packageMap.values().toArray(new MessageSendPackage[0]));
    }

    private Set<String> getMessageRecipients(Message message) {
        Set<String> set = new HashSet<>();
        for (MessageRecipient recipient : message.getToList()) {
            set.add(recipient.getEmailAddress());
        }
        for (MessageRecipient recipient : message.getCcList()) {
            set.add(recipient.getEmailAddress());
        }
        for (MessageRecipient recipient : message.getBccList()) {
            set.add(recipient.getEmailAddress());
        }
        return set;
    }

    private MessageSendPackage generateTopLevelPackage(MIMEType mime, Message message) throws Exception {
        Map<String, MessageSendKey> attachmentKeys = new HashMap<>();
        for (Attachment attachment : message.getAttachments()) {
            String attachmentID = attachment.getAttachmentId();
            byte[] keyPackage = Base64.decode(attachment.getKeyPackets(), Base64.DEFAULT);
            byte[] sessionKey = crypto.decryptKeyPacket(keyPackage);
            attachmentKeys.put(attachmentID, new MessageSendKey(crypto.getSessionKey(keyPackage).getAlgo(), sessionKey));
        }

        CipherText encPackage = generateEncryptedBody(message, mime);
        byte[] keyPackage = encPackage.getKeyPacket();
        byte[] sessionKey = crypto.decryptKeyPacket(keyPackage);

        return new MessageSendPackage(Base64.encodeToString(encPackage.getDataPacket(), Base64.NO_WRAP),
                new MessageSendKey(crypto.getSessionKey(keyPackage).getAlgo(), sessionKey),
                mime,
                attachmentKeys);
    }

    private CipherText generateEncryptedBody(Message message, MIMEType mime) throws Exception {
        MIMEType messageMime = MIMEType.fromString(message.getMimeType());
        if (messageMime == mime) {
            return new CipherText(message.getMessageBody());
        }
        if (mime == MIMEType.MIME) {
            return generateEncryptedMIME(message);
        }
        if (mime == MIMEType.PLAINTEXT) {
            return generatePlaintextBody(message);
        }
        // Should not happen
        return new CipherText(message.getMessageBody());
    }

    private CipherText generateEncryptedMIME(Message message) throws Exception {
        MIMEType messageMime = MIMEType.fromString(message.getMimeType());
        MIMEBuilder mimeBuilder = new MIMEBuilder(mApi, crypto);
        String html = messageMime == MIMEType.HTML ? message.getDecryptedHTML() : null;
        String plaintext = messageMime == MIMEType.PLAINTEXT ? message.getDecryptedBody() : null;
        String mimeString = mimeBuilder
                .loadHTML(html)
                .loadPlaintext(plaintext)
                .loadAttachments(message.getAttachments())
                .buildString();

        return crypto.encrypt(mimeString, true);
    }

    private CipherText generatePlaintextBody(Message message) throws Exception {
        String html = message.getDecryptedHTML();
        String plaintext = htmlToMDConverter.convert(html);
        return crypto.encrypt(plaintext, true);
    }

    private void addAddress(
            MessageSendPackage packageModel,
            SendPreference sendPref,
            Message message,
            OutsidersPassword outsidersPassword
    ) throws Exception {
        if (!sendPref.isEncryptionEnabled() && outsidersPassword.getPassword() != null && outsidersPassword.getHint() != null) {
            addEOAddress(packageModel, sendPref, outsidersPassword);
            return;
        }
        if (sendPref.isEncryptionEnabled()) {
            addEncryptAddress(packageModel, sendPref, message);
            return;
        }
        addUnencryptedAddress(packageModel, sendPref);
    }

    private void addEOAddress(
            MessageSendPackage packageModel,
            SendPreference sendPref,
            OutsidersPassword outsidersPassword
    ) throws Exception {
        MessageSendAddressBody messageAddress = new MessageSendAddressBody();
        Map<String, String> attachmentKeys = symEncryptAttachmentKeys(outsidersPassword.getPassword().getBytes(Charsets.UTF_8) /*TODO passphrase*/, packageModel.getAttachmentKeys());
        messageAddress.setAttachmentKeyPackets(attachmentKeys);
        messageAddress.setType(PackageType.EO.getValue());
        messageAddress.setBodyKeyPacket(symEncryptKeyPacket(outsidersPassword.getPassword().getBytes(Charsets.UTF_8) /*TODO passphrase*/, packageModel.getBodyKey()));
        EOToken token = crypto.generateEOToken(outsidersPassword.getPassword().getBytes(Charsets.UTF_8) /*TODO passphrase*/);
        messageAddress.setToken(token.getToken());
        messageAddress.setEncToken(token.getEncryptedToken());
        messageAddress.setPasswordHint(outsidersPassword.getHint());
        messageAddress.setAuth(generateAuth(outsidersPassword));
        packageModel.addAddress(sendPref.getEmailAddress(), messageAddress);
    }

    private void addUnencryptedAddress(MessageSendPackage packageModel, SendPreference sendPref) {
        MessageSendAddressBody messageAddress = new MessageSendAddressBody();
        messageAddress.setType(sendPref.getEncryptionScheme().getValue());
        messageAddress.setSignature(sendPref.isSignatureEnabled() ? 1 : 0);
        packageModel.addAddress(sendPref.getEmailAddress(), messageAddress);
    }

    private void addEncryptAddress(MessageSendPackage packageModel, SendPreference sendPref, Message message) throws Exception {
        MessageSendAddressBody messageAddress = new MessageSendAddressBody();
        Map<String, String> attachmentKeys = encryptAttachmentKeys(sendPref.getPublicKey(), packageModel.getAttachmentKeys());
        messageAddress.setAttachmentKeyPackets(attachmentKeys);
        messageAddress.setType(sendPref.getEncryptionScheme().getValue());
        messageAddress.setBodyKeyPacket(encryptKeyPacket(sendPref.getPublicKey(), packageModel.getBodyKey()));
        boolean sign = true;
        for (Attachment attachment : message.getAttachments()) {
            if (attachment.getSignature() == null) {
                sign = false;
            }
        }
        messageAddress.setSignature(sign ? 1 : 0);
        packageModel.addAddress(sendPref.getEmailAddress(), messageAddress);
    }

    private Auth generateAuth(OutsidersPassword outsidersPassword) {
        final ModulusResponse modulus = mApi.randomModulus();
        final PasswordVerifier verifier = PasswordVerifier.calculate(outsidersPassword.getPassword().getBytes(Charsets.UTF_8) /*TODO passphrase*/, modulus);
        return new Auth(verifier.AuthVersion, verifier.ModulusID, verifier.Salt, verifier.SRPVerifier);
    }

    private Map<String, String> encryptAttachmentKeys(String publicKey, Map<String, MessageSendKey> attachmentKeys) throws Exception {
        Map<String, String> encAttachmentKeys = new HashMap<>();
        for(Map.Entry<String, MessageSendKey> entry : attachmentKeys.entrySet()) {
            encAttachmentKeys.put(entry.getKey(), encryptKeyPacket(publicKey, entry.getValue()));
        }
        return encAttachmentKeys;
    }

    private Map<String, String> symEncryptAttachmentKeys(byte[] password, Map<String, MessageSendKey> attachmentKeys) throws Exception {
        Map<String, String> encAttachmentKeys = new HashMap<>();
        for(Map.Entry<String, MessageSendKey> entry : attachmentKeys.entrySet()) {
            encAttachmentKeys.put(entry.getKey(), symEncryptKeyPacket(password, entry.getValue()));
        }
        return encAttachmentKeys;
    }

    private String encryptKeyPacket(String publicKey, MessageSendKey bodyKey) {
        byte[] sessionKey = Base64.decode(bodyKey.getKey(), Base64.DEFAULT);
        byte[] encSessionKey = crypto.encryptKeyPacket(sessionKey, publicKey);
        return Base64.encodeToString(encSessionKey, Base64.NO_WRAP);
    }

    private String symEncryptKeyPacket(byte[] password, MessageSendKey bodyKey) {
        byte[] sessionKey = Base64.decode(bodyKey.getKey(), Base64.DEFAULT);
        byte[] encSessionKey = crypto.encryptKeyPacketWithPassword(sessionKey, password);
        return Base64.encodeToString(encSessionKey, Base64.NO_WRAP);
    }

    @NonNull
    private MIMEType getMIMEType(Message message, SendPreference sendPref, OutsidersPassword outsidersPassword) {
        MIMEType input = sendPref.getMimeType();
        if ((!sendPref.isEncryptionEnabled() && sendPref.isSignatureEnabled() &&
                outsidersPassword.getPassword() != null && outsidersPassword.getHint() != null) || input == MIMEType.HTML) {
            return ObjectUtils.firstNonNull(MIMEType.fromString(message.getMimeType()), MIMEType.HTML);
        }
        return input;
    }

}
