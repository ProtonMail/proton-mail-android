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
package ch.protonmail.android.utils.crypto;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.proton.gopenpgp.armor.Armor;
import com.proton.gopenpgp.constants.Constants;
import com.proton.gopenpgp.crypto.Key;
import com.proton.gopenpgp.crypto.KeyRing;
import com.proton.gopenpgp.crypto.PGPSplitMessage;
import com.proton.gopenpgp.crypto.PlainMessage;
import com.proton.gopenpgp.crypto.SessionKey;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ch.protonmail.android.api.models.KeyExtensionsKt;
import ch.protonmail.android.api.models.Keys;
import ch.protonmail.android.api.models.address.Address;
import ch.protonmail.android.core.UserManager;
import ch.protonmail.android.utils.Logger;
import timber.log.Timber;

import static ch.protonmail.android.core.Constants.LogTags.SENDING_FAILED_TAG;

/**
 * Easy utility class to do some high level encryption / decryption
 */
public abstract class Crypto {

    protected UserManager userManager;
    OpenPGP openPGP;
    protected String addressID;
    private String username;

    protected Crypto(@NonNull UserManager userManager, @NonNull String username, @NonNull OpenPGP openPGP, @Nullable String addressID) {
        if (userManager == null) {
            throw new IllegalArgumentException("userManager can't be null");
        }
        if (TextUtils.isEmpty(username)) {
            throw new IllegalArgumentException("username can't be empty");
        }
        if (openPGP == null) {
            throw new IllegalArgumentException("openPGP can't be null");
        }

        this.userManager = userManager;
        this.username = username;
        this.openPGP = openPGP;
        this.addressID = addressID;
    }

    public static UserCrypto forUser(@NonNull UserManager userManager, @NonNull String username) {
        return new UserCrypto(userManager, username, userManager.getOpenPgp());
    }

    public static AddressCrypto forAddress(@NonNull UserManager userManager, @NonNull String username, @NonNull String addressID) {
        return new AddressCrypto(userManager, username, userManager.getOpenPgp(), addressID);
    }

    /**
     * Returns User Keys.
     */
    private List<Keys> getKeysForUser(@NonNull String username) {
        return userManager.getUser(username).getKeys();
    }

    /**
     * Returns Address Keys for given Address.
     */
    private List<Keys> getKeysForAddress(@NonNull String username, @NonNull String addressID) {
        List<Keys> keysForAddress = userManager.getUser(username).getAddressById(addressID).getKeys();
        if (keysForAddress == null) {
            Timber.e(SENDING_FAILED_TAG, "Keys don't exist");
        }
        return keysForAddress;
    }

    /**
     * Get Keys for decryption, either User or Address Keys.
     */
    private List<Keys> getDecryptionKeys() {
        if (addressID == null) {
            return getKeysForUser(username);
        } else {
            return getKeysForAddress(username, addressID);
        }
    }

    private String getSigningKey() throws Exception {
        return getDecryptionKeys().get(0).getPrivateKey();
    }

    /**
     * Helper method dynamically selecting passphrases for currently set decryption keys. Workaround for UserKey migration.
     */
    private byte[] getPassphraseForSigningKey() throws Exception {
            @Nullable
            byte[] userPassphrase = userManager.getMailboxPassword(username);
            if (userPassphrase == null) {
                Timber.e(SENDING_FAILED_TAG, "Passphrase doesn't exist");
            }
            return KeyExtensionsKt.getKeyPassphrase(getDecryptionKeys().get(0), openPGP, getKeysForUser(username), userPassphrase);
        }

    byte[] getDecryptionKeysAsByteArray() throws Exception {
        System.out.println("decryption keys count: " + getDecryptionKeys().size());
        return combinePrivateKeys(getDecryptionKeys());
    }

    List<byte[]> getVerificationKeys() throws Exception {
        List<byte[]> keys = new ArrayList<>();
        for (Keys k : getDecryptionKeys()) {
            keys.add(com.proton.gopenpgp.crypto.Crypto.newKeyFromArmored(k.getPrivateKey()).getPublicKey());
        }
//        System.out.println("decryption keys count: " + getDecryptionKeys().size());
        return keys;
    }

    List<byte[]> getUnarmoredDecryptionKeys() throws Exception {
        List<Keys> decryptionKeys = getDecryptionKeys();
        List<byte[]> unarmoredPrivateKeys = new ArrayList<>(decryptionKeys.size());
        for (Keys k : decryptionKeys) {
            unarmoredPrivateKeys.add(Armor.unarmor(k.getPrivateKey()));
        }
        return unarmoredPrivateKeys;
    }

    /**
     * Helper method dynamically selecting passphrase for User Key.
     */
    private byte[] getPassphraseForDecryptionKey(@NonNull Keys key) throws Exception {
        return KeyExtensionsKt.getKeyPassphrase(key, openPGP, getKeysForUser(username), userManager.getMailboxPassword(username));
    }

    /**
     * Helper method dynamically selecting passphrases for currently set decryption keys. It takes the first UserKey. Workaround for UserKey migration.
     */
    byte[] getPassphraseForDecryptionKeys() throws Exception {
        return getPassphraseForDecryptionKey(getDecryptionKeys().get(0) /*let it throw, the sooner the better*/);
    }

    private String getEncryptionKey() throws Exception {
        return getArmoredPublicKey(getDecryptionKeys().get(0));
    }

    private byte[] combinePrivateKeys(List<Keys> keys) throws Exception {
        byte[][] packets = new byte[keys.size()][];
        for (int i = 0; i < keys.size(); i++) {
            packets[i] = Armor.unarmor(keys.get(i).getPrivateKey());
        }
        return combineKeys(packets);
    }

    byte[] combineKeys(String[] keys) throws Exception {
        byte[][] packets = new byte[keys.length][];
        for (int i = 0; i < keys.length; i++) {
            packets[i] = Armor.unarmor(keys[i]);
        }
        return combineKeys(packets);
    }

    byte[] combineKeys(byte[][] packets) {
        int length = 0;
        for (byte[] packet : packets) {
            length += packet.length;
        }
        byte[] result = new byte[length];
        int pos = 0;
        for (byte[] packet : packets) {
            System.arraycopy(packet, 0, result, pos, packet.length);
            pos += packet.length;
        }

        return result;
    }

    protected TextDecryptionResult verify(String data, String signature, List<byte[]> verifierKeys, long time) throws Exception {
        boolean valid = openPGP.verifyTextSignDetachedBinKey(signature, data, verifierKeys, time);
        return new TextDecryptionResult(data, true, valid);
    }

    protected BinaryDecryptionResult verify(byte[] data, String signature, List<byte[]> verifierKeys, long time) throws Exception {
        boolean valid = openPGP.verifyBinSignDetachedBinKey(signature, data, verifierKeys, time);
        return new BinaryDecryptionResult(data, true, valid);
    }

    public String sign(String data) throws Exception {
        return openPGP.signTextDetached(data, getSigningKey(), getPassphraseForSigningKey());
    }

    public String sign(byte[] data) throws Exception {
        return openPGP.signBinDetached(data, getSigningKey(), getPassphraseForSigningKey());
    }

    public TextDecryptionResult decrypt(TextCiphertext message) throws Exception {
        for (Keys key : getDecryptionKeys()) {
            try {
                return new TextDecryptionResult(openPGP.decryptMessageBinKey(message.getArmored(), Armor.unarmor(key.getPrivateKey()), getPassphraseForDecryptionKey(key)), false, false);
            } catch (Exception e) {
                // this exception says only that one of possibly many keys was incorrect
            }
        }

        Timber.e("error decrypting message in 'decrypt', keys = " + getDecryptionKeys().size() + " addressId null? = " + (addressID == null));
        throw new Exception("error decrypting message, there is no valid decryption key");
    }

    /**
     * Special method used for decryption of data for given username. Crypto is very tightly coupled with UserManager and this is workaround
     * just for this use case. Has no side effects.
     */
    public TextDecryptionResult decryptForUser(@NonNull TextCiphertext message, @NonNull String username) throws Exception {
        List<Keys> userKeys = getKeysForUser(username);
        for (Keys key : userKeys) {
            try {
                return new TextDecryptionResult(openPGP.decryptMessageBinKey(message.getArmored(), Armor.unarmor(key.getPrivateKey()), KeyExtensionsKt.getKeyPassphrase(key, openPGP, userKeys, userManager.getMailboxPassword(username))), false, false);
            } catch (Exception e) {
                // this exception says only that one of possibly many keys was incorrect
            }
        }

        Timber.e("error decrypting message in 'decryptForUser', keys = " + userKeys.size() + " addressId = " + addressID);
        throw new Exception("error decrypting message, there is no valid decryption key");
    }

    protected TextDecryptionResult decrypt(TextCiphertext message, List<byte[]> publicKeys, long time) throws Exception {
        for (Keys key : getDecryptionKeys()) {
            try {
                return openPGP.decryptMessageVerifyBinKeyPrivbinkeys(message.getArmored(), publicKeys, new ArrayList<byte[]>() {{
                    add(Armor.unarmor(key.getPrivateKey()));
                }}, getPassphraseForDecryptionKey(key), time);
            } catch (Exception e) {
                // this exception says only that one of possibly many keys was incorrect
            }
        }

        Timber.e("error decrypting message in 'decrypt with bin keys', keys = " + getDecryptionKeys().size() + " addressId = " + addressID);
        throw new Exception("error decrypting message, there is no valid decryption key");
    }

    protected BinaryDecryptionResult decrypt(BinaryCiphertext message) throws Exception {
        for (Keys key : getDecryptionKeys()) {
            try {
                byte[] data = openPGP.decryptAttachmentBinKey(message.getKeyPacket(), message.getDataPacket(), new ArrayList<byte[]>() {{
                    add(Armor.unarmor(key.getPrivateKey()));
                }}, getPassphraseForDecryptionKey(key));
                return new BinaryDecryptionResult(data, false, false);
            } catch (Exception e) {
                // this exception says only that one of possibly many keys was incorrect
            }
        }

        Timber.e("error decrypting message in 'decrypt attachment bin keys', keys = " + getDecryptionKeys().size() + " addressId = " + addressID);
        throw new Exception("error decrypting attachment, there is no valid decryption key");

    }

    public byte[] decryptKeyPacket(byte[] keyPacket) throws Exception {
        Address address = userManager.getUser(username).getAddressById(addressID);
        return createAndUnlockAddressKeyRing(address).decryptSessionKey(keyPacket).getKey();
    }

    /*TODO this method exists only for logging purpose, remove later*/
    public byte[] decryptKeyPacketWithMessageId(byte[] keyPacket, String originalMessageAddressId) throws Exception {
        try {
            return decryptKeyPacket(keyPacket);
        } catch (Exception e) {
            Timber.e(e, "error decrypting SessionKey, addressId used = %s, original message addressId = %s", userManager.getUser(username).getAddressById(addressID).getID(), originalMessageAddressId);
            throw e;
        }
    }

    protected byte[] encryptKeyPacket(byte[] sessionKey, byte[] publicKey) throws Exception {
        KeyRing keyRing = openPGP.buildKeyRing(publicKey);
        SessionKey symmetricKey = new SessionKey(sessionKey, Constants.AES256);
        return keyRing.encryptSessionKey(symmetricKey);
    }

    protected byte[] encryptKeyPacket(byte[] sessionKey, String publicKey) throws Exception {
        return encryptKeyPacket(sessionKey, Armor.unarmor(publicKey));
    }

    public byte[] encryptKeyPacketWithPassword(byte[] sessionKey, byte[] password) throws Exception {
        SessionKey symmetricKey = new SessionKey(sessionKey, Constants.AES256);
        return com.proton.gopenpgp.crypto.Crypto.encryptSessionKeyWithPassword(symmetricKey, password);
    }

    TextCiphertext[] encryptObject(String text, Object[] publicKeys, boolean sign) throws Exception {
        if (publicKeys.length == 0) {
            throw new IllegalArgumentException("No public key to encrypted with provided");
        }
        boolean stringKeys = publicKeys instanceof String[];
        TextCiphertext cipherText;
        if (stringKeys) {
            cipherText = this.encrypt(text, (String) publicKeys[0], sign);
        } else {
            cipherText = this.encrypt(text, (byte[]) publicKeys[0], sign);
        }

        TextCiphertext[] result = new TextCiphertext[publicKeys.length];
        result[0] = cipherText;

        if (result.length == 1) {
            return result;
        }

        byte[] keyPacket = cipherText.getKeyPacket();
        byte[] dataPacket = cipherText.getDataPacket();
        SessionKey sessionSplit = getSessionFromKeyPacketBinkeys(keyPacket);

        for (int i = 1; i < result.length; i++) {
            byte[] newKeyPacket;
            if (stringKeys) {
                newKeyPacket = openPGP.keyPacketWithPublicKey(sessionSplit, (String) publicKeys[i]);
            } else {
                newKeyPacket = openPGP.keyPacketWithPublicKeyBin(sessionSplit, (byte[]) publicKeys[i]);
            }
            result[i] = TextCiphertext.fromPackets(newKeyPacket, dataPacket);
        }
        return result;
    }

    protected TextCiphertext encrypt(String text, String publicKey, boolean sign) throws Exception {
        String message = openPGP.encryptMessage(text, publicKey, sign ? getSigningKey() : null, getPassphraseForSigningKey(), true);
        return TextCiphertext.fromArmor(message);
    }

    protected TextCiphertext encrypt(String text, byte[] publicKey, boolean sign) throws Exception {
        String message = openPGP.encryptMessageBinKey(text, publicKey, sign ? getSigningKey() : null, getPassphraseForSigningKey(), true);
        return TextCiphertext.fromArmor(message);
    }

    public TextCiphertext encrypt(String text, boolean sign) throws Exception {
        return encrypt(text, getEncryptionKey(), sign);
    }

    BinaryCiphertext[] encryptObject(byte[] text, String filename, Object[] publicKeys) throws Exception {
        if (publicKeys.length == 0) {
            throw new IllegalArgumentException("No public key to encrypted with provided");
        }
        boolean stringKeys = publicKeys instanceof String[];
        BinaryCiphertext cipherText;
        if (stringKeys) {
            cipherText = this.encrypt(text, filename, (String) publicKeys[0]);
        } else {
            cipherText = this.encrypt(text, filename, (byte[]) publicKeys[0]);
        }

        BinaryCiphertext[] result = new BinaryCiphertext[publicKeys.length];
        result[0] = cipherText;

        if (result.length == 1) {
            return result;
        }

        byte[] keyPacket = cipherText.getKeyPacket();
        byte[] dataPacket = cipherText.getDataPacket();
        SessionKey sessionSplit = getSessionFromKeyPacketBinkeys(keyPacket);

        for (int i = 1; i < result.length; i++) {
            byte[] newKeyPacket;
            if (stringKeys) {
                newKeyPacket = openPGP.keyPacketWithPublicKey(sessionSplit, (String) publicKeys[i]);
            } else {
                newKeyPacket = openPGP.keyPacketWithPublicKeyBin(sessionSplit, (byte[]) publicKeys[i]);
            }
            result[i] = BinaryCiphertext.fromPackets(newKeyPacket, dataPacket);
        }
        return result;
    }

    protected BinaryCiphertext encrypt(byte[] data, String filename, String publicKey) throws Exception {
        return encrypt(data, filename, Armor.unarmor(publicKey));
    }

    protected BinaryCiphertext encrypt(byte[] data, String filename, byte[] publicKey) throws Exception {
        PGPSplitMessage pgpSplitMessage = openPGP.buildKeyRing(publicKey).encryptAttachment(new PlainMessage(data), filename);
        return BinaryCiphertext.fromPackets(pgpSplitMessage.getKeyPacket(), pgpSplitMessage.getDataPacket());
    }

    public BinaryCiphertext encrypt(byte[] data, String filename) throws Exception {
        return encrypt(data, filename, getEncryptionKey());
    }

    public KeyInformation deriveKeyInfo(String key) {
        try {
            String fingerprint = openPGP.getFingerprint(key);
            boolean isExpired = openPGP.isKeyExpired(key);
            byte[] privateKey = Armor.unarmor(key);
            byte[] publicKey = openPGP.getPublicKey(key);
            privateKey = Arrays.equals(privateKey, publicKey) ? null : privateKey;
            return new KeyInformation(publicKey, privateKey, true, fingerprint, isExpired);
        } catch (Exception e) {
            return new KeyInformation(null, null, false, null, true);
        }
    }

    public String getFingerprint(String key) throws Exception {
        return openPGP.getFingerprint(key);
    }

    public SessionKey getSessionKey(byte[] keyPacket) throws Exception {
        return getSessionFromKeyPacketBinkeys(keyPacket);
    }

    private SessionKey getSessionFromKeyPacketBinkeys(byte[] keyPacket) throws Exception {
        for (Keys key : getDecryptionKeys()) {
            try {
                return openPGP.getSessionFromKeyPacketBinkeys(keyPacket, Armor.unarmor(key.getPrivateKey()), getPassphraseForDecryptionKey(key));
            } catch (Exception e) {
                //Logger.doLogException("Error getting session with key " + key.getID(), e);
            }
        }

        throw new Exception("error getting Session, there is no valid decryption key");
    }

    public boolean isAllowedForSending(@NonNull Keys key) {
        return openPGP.checkPassphrase(key.getPrivateKey(), userManager.getMailboxPassword(username));
    }

    @Nullable
    public String getArmoredPublicKey(@NonNull Keys key) {
        Key privateKey = null;
        try {
            privateKey = com.proton.gopenpgp.crypto.Crypto.newKeyFromArmored(key.getPrivateKey());
            return privateKey.getArmoredPublicKey();
        } catch (Exception e) {
            Logger.doLogException(e);
            return null;
        } finally {
            if (privateKey != null) privateKey.clearPrivateParams();
        }
    }

    @Nullable
    private KeyRing createAndUnlockUserKeyRing() {
        try {
            return openPGP.buildPrivateKeyRingArmored(userManager.getTokenManager(username).getEncPrivateKey(), userManager.getMailboxPassword(username));
        } catch (Exception e) {
            Logger.doLogException(e);
        }
        return null;
    }

    @Nullable
    private KeyRing createAndUnlockAddressKeyRing(@NonNull Address address) {

        try {
            // build AddressKeyRing using all AddressKeys
            KeyRing addressKeyRing = com.proton.gopenpgp.crypto.Crypto.newKeyRing(null);

            boolean unlockedAtLeastOnce = false;

            // try to unlock as many keys as possible, using their respective passphrases
            for (Keys key : address.getKeys()) {
                try {
                    byte[] addressKeyPassphrase = KeyExtensionsKt.getKeyPassphrase(key, openPGP, getKeysForUser(username), userManager.getMailboxPassword(username));
                    Key addressKey = com.proton.gopenpgp.crypto.Crypto.newKeyFromArmored(key.getPrivateKey());
                    addressKey = addressKey.unlock(addressKeyPassphrase); // method `unlock()` RETURNS AN UNLOCKED COPY of original key
                    addressKeyRing.addKey(addressKey);
                    unlockedAtLeastOnce = true;
                } catch (Exception e) {
                    // don't fail here, because we still have other keys to try
                    Logger.doLogException(e);
                }
            }

            if (unlockedAtLeastOnce) {
                return addressKeyRing;
            } else {
                Timber.d("could not unlock single address key for " + address.getEmail());
                return null;
            }

        } catch (Exception e) {
            Timber.d(e, "could not create address keyring for " + address.getEmail());
            return null;
        }

    }

}
