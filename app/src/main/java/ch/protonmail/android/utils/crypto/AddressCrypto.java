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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import ch.protonmail.android.core.UserManager;

/**
 * Crypto helper set up with keys from Address that belongs to User.
 */
public class AddressCrypto extends Crypto implements Serializable {

    protected AddressCrypto(@NonNull UserManager userManager, @NonNull  String username, @NonNull  OpenPGP openPGP, @NonNull String addressID) {
        super(userManager, username, openPGP, addressID);
    }

//    public TextDecryptionResult verify(String data, String signature, String[] publicKeys, long time) throws Exception {
//        return super.verify(data, signature, combineKeys(publicKeys), time);
//    }

    public TextDecryptionResult decrypt(TextCiphertext message, List<byte[]> publicKeys, long time) throws Exception {
        return super.decrypt(message, publicKeys, time);
    }

    public BinaryDecryptionResult decrypt(BinaryCiphertext message) throws Exception {
        return super.decrypt(message);
    }

    public byte[] encryptKeyPacket(byte[] keyPacket, byte[] publicKey) throws Exception {
        return super.encryptKeyPacket(keyPacket, publicKey);
    }

    public byte[] encryptKeyPacket(byte[] keyPacket, String publicKey) throws Exception {
        return super.encryptKeyPacket(keyPacket, publicKey);
    }

    public List<TextCiphertext> encrypt(String text, String[] publicKeys, boolean sign) throws Exception {
        return Arrays.asList(encryptObject(text, publicKeys, sign));
    }

    public List<TextCiphertext> encrypt(String text, byte[][] publicKeys, boolean sign) throws Exception {
        return Arrays.asList(encryptObject(text, publicKeys, sign));
    }

    protected TextCiphertext encrypt(String text, String publicKey, boolean sign) throws Exception {
        return super.encrypt(text, publicKey, sign);
    }

    protected TextCiphertext encrypt(String text, byte[] publicKey, boolean sign) throws Exception {
        return super.encrypt(text, publicKey, sign);
    }

    public List<BinaryCiphertext> encrypt(byte[] data, String filename, String[] publicKeys) throws Exception {
        return Arrays.asList(encryptObject(data, filename, publicKeys));
    }

    public List<BinaryCiphertext> encrypt(byte[] data, String filename, byte[][] publicKeys) throws Exception {
        return Arrays.asList(encryptObject(data, filename, publicKeys));
    }

    protected BinaryCiphertext encrypt(byte[] data, String filename, String publicKey) throws Exception {
        return super.encrypt(data, filename, publicKey);
    }

    protected BinaryCiphertext encrypt(byte[] data, String filename, byte[] publicKey) throws Exception {
        return super.encrypt(data, filename, publicKey);
    }

    public EOToken generateEOToken(byte[] password) throws Exception {
        // generate a 256 bit token.
        byte[] randomToken = openPGP.randomToken();
        String base64token = Base64.encodeToString(randomToken, android.util.Base64.NO_WRAP);
        String encToken = openPGP.encryptMessageWithPassword(base64token, password);
        return new EOToken(base64token, encToken);
    }

    public MimeDecryptor decryptMime(TextCiphertext message) throws Exception {
        return new MimeDecryptor(message.armored, openPGP, getUnarmoredDecryptionKeys(), new String(getPassphraseForDecryptionKeys()) /*TODO gopenpgp*/);
    }
}
