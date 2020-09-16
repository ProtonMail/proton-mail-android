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

import androidx.annotation.NonNull;

/**
 * Created by kaylukas on 18/05/2018.
 */

public class KeyInformation {

    private String fingerprint;
    private boolean isExpired;
    private boolean isValid;
    private byte[] publicKey;
    private byte[] privateKey;
    private boolean compromised;

    @NonNull
    public static KeyInformation EMPTY() {
        return new KeyInformation(
                null,
                null,
                false,
                null,
                true
        );
    }

    public KeyInformation (byte[] publicKey, byte[] privateKey, boolean isValid, String fingerprint, boolean isExpired) {
        this.isValid = isValid;
        this.fingerprint = fingerprint;
        this.isExpired = isExpired;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.compromised = false;
    }

    public boolean isExpired() {
        return isExpired;
    }

    public boolean isValid() {
        return isValid;
    }

    public String getFingerprint() {
        return fingerprint;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void flagAsCompromised() {
        compromised = true;
    }

    public boolean isCompromised() {
        return compromised;
    }
}
