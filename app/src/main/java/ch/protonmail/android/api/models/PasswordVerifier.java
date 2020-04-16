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
package ch.protonmail.android.api.models;

import android.util.Base64;

import com.proton.gopenpgp.crypto.ClearTextMessage;

import java.security.SecureRandom;

import ch.protonmail.android.utils.ConstantTime;
import ch.protonmail.android.utils.PasswordUtils;
import ch.protonmail.android.utils.SRPClient;

public class PasswordVerifier {
    public final int AuthVersion;
    public final String ModulusID;
    public final String Salt;
    public final String SRPVerifier;

    private PasswordVerifier(final int authVersion, final String modulusID, final String salt, final String srpVerifier) {
        AuthVersion = authVersion;
        ModulusID = modulusID;
        Salt = salt;
        SRPVerifier = srpVerifier;
    }

    public static PasswordVerifier calculate(final byte[] password, final ModulusResponse modulusResp) {
        final byte[] salt = new byte[10];
        new SecureRandom().nextBytes(salt);
        final byte[] modulus;
        try {
            modulus = Base64.decode(new ClearTextMessage(modulusResp.getModulus()).getData(), Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        final byte[] hashedPassword = PasswordUtils.hashPassword(PasswordUtils.CURRENT_AUTH_VERSION, password, null, salt, modulus);
        final byte[] verifier = SRPClient.generateVerifier(2048, modulus, hashedPassword);
        return new PasswordVerifier(PasswordUtils.CURRENT_AUTH_VERSION, modulusResp.getModulusID(), ConstantTime.encodeBase64(salt, true), ConstantTime.encodeBase64(verifier, true));
    }
}
