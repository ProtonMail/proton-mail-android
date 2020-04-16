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
package ch.protonmail.android.utils;

import com.proton.gopenpgp.srp.Srp;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import timber.log.Timber;

public class PasswordUtils {
    public static final int CURRENT_AUTH_VERSION = 4;

    public static String cleanUserName(String username) {
        return username.replaceAll("_|\\.|-", "").toLowerCase();
    }

    public static byte[] expandHash(final byte[] input) throws NoSuchAlgorithmException {
        final byte[] output = new byte[2048 / 8];
        final MessageDigest digest = MessageDigest.getInstance("SHA-512");

        digest.update(input);
        digest.update((byte) 0);
        System.arraycopy(digest.digest(), 0, output, 0, 512 / 8);
        digest.reset();

        digest.update(input);
        digest.update((byte) 1);
        System.arraycopy(digest.digest(), 0, output, 512 / 8, 512 / 8);
        digest.reset();

        digest.update(input);
        digest.update((byte) 2);
        System.arraycopy(digest.digest(), 0, output, 1024 / 8, 512 / 8);
        digest.reset();

        digest.update(input);
        digest.update((byte) 3);
        System.arraycopy(digest.digest(), 0, output, 1536 / 8, 512 / 8);
        digest.reset();

        return output;
    }

    public static byte[] hashPassword(final int authVersion, final byte[] password, final String username, final byte[] salt, final byte[] modulus) {

        try {
            return Srp.hashPassword(authVersion, new String(password), username, salt, modulus);
        } catch (Exception e) {
            Timber.e(e, "Hashing password failed");
        }
        return null;
    }
}
