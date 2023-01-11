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
package ch.protonmail.android.utils.crypto;

import androidx.annotation.NonNull;

import java.util.Arrays;
import java.util.Objects;

/**
 * Created by kaylukas on 18/05/2018.
 */

public class KeyInformation {

    private String fingerprint;
    private boolean isExpired;
    private boolean isValid;
    private boolean canEncrypt;
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
                true,
                false
        );
    }

    public KeyInformation (byte[] publicKey, byte[] privateKey, boolean isValid, String fingerprint, boolean isExpired, boolean canEncrypt) {
        this.isValid = isValid;
        this.fingerprint = fingerprint;
        this.isExpired = isExpired;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.compromised = false;
        this.canEncrypt = canEncrypt;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KeyInformation that = (KeyInformation) o;
        return isExpired == that.isExpired &&
                isValid == that.isValid &&
                compromised == that.compromised &&
                Objects.equals(fingerprint, that.fingerprint) &&
                Arrays.equals(publicKey, that.publicKey) &&
                Arrays.equals(privateKey, that.privateKey);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(fingerprint, isExpired, isValid, compromised);
        result = 31 * result + Arrays.hashCode(publicKey);
        result = 31 * result + Arrays.hashCode(privateKey);
        return result;
    }

    public boolean canEncrypt() {
        return canEncrypt;
    }
}
