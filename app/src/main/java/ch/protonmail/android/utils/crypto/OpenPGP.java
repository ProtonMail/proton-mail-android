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

import android.util.Base64;

import androidx.annotation.NonNull;

import com.proton.gopenpgp.armor.Armor;
import com.proton.gopenpgp.constants.Constants;
import com.proton.gopenpgp.crypto.Crypto;
import com.proton.gopenpgp.crypto.Key;
import com.proton.gopenpgp.crypto.KeyRing;
import com.proton.gopenpgp.crypto.MIMECallbacks;
import com.proton.gopenpgp.crypto.PGPMessage;
import com.proton.gopenpgp.crypto.PGPSignature;
import com.proton.gopenpgp.crypto.PlainMessage;
import com.proton.gopenpgp.crypto.SessionKey;
import com.proton.gopenpgp.helper.ExplicitVerifyMessage;
import com.proton.gopenpgp.helper.Helper;
import com.proton.gopenpgp.srp.Srp;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;

import at.favre.lib.crypto.bcrypt.Radix64Encoder;
import ch.protonmail.android.utils.Logger;
import ch.protonmail.android.utils.crypto.Primes.PrimeGenerator;
import timber.log.Timber;

public class OpenPGP {

    public byte[] decryptAttachmentBinKey(byte[] keyPacket, byte[] dataPacket, List<byte[]> privateKeys, byte[] passphrase) throws Exception {
        KeyRing privateKeyRing = buildPrivateKeyRing(privateKeys, passphrase);
        return Helper.decryptAttachment(keyPacket, dataPacket, privateKeyRing).getData();
    }

    public String decryptMessage(String encryptedText, String privateKey, byte[] passphrase) throws Exception {
        return Helper.decryptMessageArmored(privateKey, passphrase, encryptedText);
    }

    public String decryptMessageBinKey(String encryptedText, byte[] privateKey, byte[] passphrase) throws Exception {
        return Helper.decryptMessageArmored(Armor.armorKey(privateKey), passphrase, encryptedText);
    }

    public TextDecryptionResult decryptMessageVerifyBinKeyPrivbinkeys(String encryptedText, List<byte[]> veriferKeys, List<byte[]> privateKeys, byte[] passphrase, long verifyTime) throws Exception {

        KeyRing privateKeyRing = null;
        try {
            privateKeyRing = buildPrivateKeyRing(privateKeys, passphrase);
        } catch (Exception e) {
            System.out.println("1 -> " + e.getMessage());
        }
        KeyRing verifierKeyRing = null;
        try {
            verifierKeyRing = (veriferKeys == null || veriferKeys.size() == 0) ? null : buildKeyRing(veriferKeys);
        } catch (Exception e) {
            System.out.println("2 -> " + e.getMessage());
        }
        ExplicitVerifyMessage verify = Helper.decryptExplicitVerify(new PGPMessage(encryptedText), privateKeyRing, verifierKeyRing, verifyTime);

        return new TextDecryptionResult(verify.getMessage().getString(), verify.getSignatureVerificationError() != null ? verify.getSignatureVerificationError().getStatus() : Constants.SIGNATURE_OK);
    }

    public String encryptMessage(String plainText, String publicKey, String privateKey, byte[] passphrase, boolean trim) throws Exception {
        if (privateKey == null) {
            return Helper.encryptMessageArmored(publicKey, plainText);
        } else {
            return Helper.encryptSignMessageArmored(publicKey, privateKey, passphrase, plainText);
        }
    }

    public String encryptMessageBinKey(String plainText, byte[] publicKey, String privateKey, byte[] passphrase, boolean trim) throws Exception {
        if (privateKey == null) {
            return Helper.encryptMessageArmored(Armor.armorKey(publicKey), plainText);
        } else {
            return Helper.encryptSignMessageArmored(Armor.armorKey(publicKey), privateKey, passphrase, plainText);
        }
    }

    public String encryptMessageWithPassword(String plainText, byte[] password) throws Exception {
        return Crypto.encryptMessageWithPassword(new PlainMessage(plainText), password).getArmored();
    }

    public String generateKey(String userName, String domain, byte[] passphrase, KeyType keyType, int bits) throws Exception {

        if (passphrase == null || passphrase.length == 0) {
            throw new Exception("passphrase for generating key can't be empty");
        }

        if (keyType == KeyType.RSA) {
            // Generate some primes as the go library is quite slow. On android we can use SSL + multithreading
            // This reduces the generation time from 3 minutes to 1 second.
            PrimeGenerator generator = new PrimeGenerator();
            BigInteger[] primes = generator.generatePrimes(bits / 2, 4);
            String username = userName + "@" + domain;
            return generateRSAKeyWithPrimes(username, username, passphrase, bits,
                    primes[0].toByteArray(), primes[1].toByteArray(),
                    primes[2].toByteArray(), primes[3].toByteArray());
        }
        return Helper.generateKey(userName, userName, passphrase, keyType.toString(), bits);
    }

    /**
     * Crypto Helper doesn't have this method, so we have our own implementation
     */
    private String generateRSAKeyWithPrimes(String name, String email, byte[] passphrase, int bits,
                                            byte[] prime1, byte[] prime2, byte[] prime3, byte[] prime4) throws Exception {

        Key key = Crypto.generateRSAKeyWithPrimes(name, email, bits, prime1, prime2, prime3, prime4);
        Key lockedKey = key.lock(passphrase);
        if (lockedKey.isUnlocked()) {
            throw new Exception("could not lock newly generated key");
        }
        key.clearPrivateParams();
        return lockedKey.armor();
    }

    public long getTime() {
        return com.proton.gopenpgp.crypto.Crypto.getUnixTime();
    }

    public boolean isKeyExpired(String publicKey) throws Exception {
        return Crypto.newKeyFromArmored(publicKey).isExpired();
    }

    public String signBinDetached(byte[] plainData, String privateKey, byte[] passphrase) throws Exception {
        KeyRing privateKeyRing = buildPrivateKeyRingArmored(privateKey, passphrase);
        return privateKeyRing.signDetached(new PlainMessage(plainData)).getArmored();
    }

    public String signTextDetached(String plainText, String privateKey, byte[] passphrase) throws Exception {
        KeyRing privateKeyRing = buildPrivateKeyRingArmored(privateKey, passphrase);
        return privateKeyRing.signDetached(new PlainMessage(plainText)).getArmored();
    }

    public String updatePrivateKeyPassphrase(String privateKey, byte[] oldPassphrase, byte[] newPassphrase) throws Exception {
        return Helper.updatePrivateKeyPassphrase(privateKey, oldPassphrase, newPassphrase);
    }

    public byte[] generateMailboxPassword(String encodedSalt, byte[] password) {

        byte[] decodedKeySalt = Base64.decode(encodedSalt, Base64.DEFAULT);
        try {
            String generatedMailboxPassword = Srp.mailboxPassword(new String(password), decodedKeySalt);
            return generatedMailboxPassword.replace("$2y$10$", "").replace(new String(new Radix64Encoder.Default().encode(decodedKeySalt)), "").getBytes();

        } catch (Exception e) {
            Timber.e(e, "Generating mailbox password failed");
        }
        return null;
    }

    public void updateTime(long newTime) {
        com.proton.gopenpgp.crypto.Crypto.updateTime(newTime);
    }

    public boolean verifyBinSignDetachedBinKey(String signature, byte[] plainData, List<byte[]> publicKeys, long verifyTime) throws Exception {
        KeyRing signingKeyRing = buildKeyRing(publicKeys);
        signingKeyRing.verifyDetached(new PlainMessage(plainData), new PGPSignature(signature), verifyTime);
        return true;
    }

    public boolean verifyTextSignDetached(String signature, String plainText, String publicKey, long verifyTime) throws Exception {
        KeyRing signingKeyRing = buildKeyRingArmored(publicKey);
        signingKeyRing.verifyDetached(new PlainMessage(plainText), new PGPSignature(signature), verifyTime);
        return true;
    }

    public boolean verifyTextSignDetachedBinKey(String signature, String plainText, List<byte[]> publicKeys, long verifyTime) throws Exception {
        KeyRing signingKeyRing = buildKeyRing(publicKeys);
        signingKeyRing.verifyDetached(new PlainMessage(plainText), new PGPSignature(signature), verifyTime);
        return true;
    }

    public SessionKey getSessionFromKeyPacketBinkeys(byte[] keyPackage, byte[] privateKey, byte[] passphrase) throws Exception {
        return buildPrivateKeyRing(privateKey, passphrase).decryptSessionKey(keyPackage);
    }

    public byte[] keyPacketWithPublicKeyBin(SessionKey sessionSplit, byte[] publicKey) throws Exception {
        return buildKeyRing(publicKey).encryptSessionKey(sessionSplit);
    }

    public byte[] keyPacketWithPublicKey(SessionKey sessionSplit, String publicKey) throws Exception {
        KeyRing keyRing = buildKeyRingArmored(publicKey);
        return keyRing.encryptSessionKey(sessionSplit);
    }

    public String getFingerprint(String key) throws Exception {
        return Crypto.newKeyFromArmored(key).getFingerprint();
    }

    public byte[] getPublicKey(String key) throws Exception {
        return Crypto.newKeyFromArmored(key).getPublicKey();
    }

    public byte[] randomToken() throws Exception {
        return com.proton.gopenpgp.crypto.Crypto.randomToken(32); // 32 bytes
    }

    /**
     * @return 16-byte, base64-ed random salt as String, without newline character
     */
    public String createNewKeySalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        String keySalt = Base64.encodeToString(salt, Base64.DEFAULT);
        keySalt = keySalt.substring(0, keySalt.length() - 1); // truncate newline character
        return keySalt;
    }

    public void decryptMIMEMessage(String encryptedText, byte[] verifierKey,
                                   List<byte[]> privateKeys, byte[] passphrase,
                                   MIMECallbacks callbacks, long verifyTime) throws Exception {

        KeyRing privateKeyRing = buildPrivateKeyRing(privateKeys, passphrase);
        if (verifierKey == null || verifierKey.length == 0) {
            privateKeyRing.decryptMIMEMessage(new PGPMessage(encryptedText), null, callbacks, verifyTime);
        } else {
            KeyRing keyRing = null;
            try {
                keyRing = buildKeyRing(verifierKey);
            } catch (Exception e) {
                callbacks.onError(e);
            }
            privateKeyRing.decryptMIMEMessage(new PGPMessage(encryptedText), keyRing, callbacks, verifyTime);
        }
    }

    public boolean checkPassphrase(@NonNull String armoredKey, @NonNull byte[] passphrase) {
        try {
            Crypto.newKeyFromArmored(armoredKey).unlock(passphrase);
            return true;
        } catch (Exception e) {
            Logger.doLogException(e);
        }
        return false;
    }

    /**
     * Builds KeyRing from single armored Private Key and unlocks it with provided passphrase.
     */
    KeyRing buildPrivateKeyRingArmored(String key, byte[] passphrase) throws Exception {
        return Crypto.newKeyRing(Crypto.newKeyFromArmored(key).unlock(passphrase));
    }

    /**
     * Builds KeyRing from single unarmored Private Key and unlocks it with provided passphrase.
     */
    KeyRing buildPrivateKeyRing(byte[] key, byte[] passphrase) throws Exception {
        return Crypto.newKeyRing(Crypto.newKey(key).unlock(passphrase));
    }

    /**
     * Builds KeyRing from list of unarmored Private Keys and unlocks it with provided passphrase.
     */
    private KeyRing buildPrivateKeyRing(List<byte[]> keys, byte[] passphrase) throws Exception {
        KeyRing keyRing = Crypto.newKeyRing(null);
        for (byte[] k : keys) {
            try {
                keyRing.addKey(Crypto.newKey(k).unlock(passphrase));
            } catch (Exception e) {
                return keyRing;
            }
        }
        return keyRing;
    }

    /**
     * Builds KeyRing from single armored Public Key.
     */
    KeyRing buildKeyRingArmored(String key) throws Exception {
        return Crypto.newKeyRing(Crypto.newKeyFromArmored(key));
    }

    /**
     * Builds KeyRing from single Public Key.
     */
    KeyRing buildKeyRing(byte[] key) throws Exception {
        return Crypto.newKeyRing(Crypto.newKey(key));
    }

    /**
     * Builds KeyRing from list of unarmored Public Keys.
     */
    KeyRing buildKeyRing(List<byte[]> keys) throws Exception {
        KeyRing keyRing = Crypto.newKeyRing(null);
        for (byte[] k : keys) {
            keyRing.addKey(Crypto.newKey(k));
        }
        return keyRing;
    }

}

