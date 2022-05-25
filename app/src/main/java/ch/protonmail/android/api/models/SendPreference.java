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
package ch.protonmail.android.api.models;

import java.io.Serializable;

import ch.protonmail.android.api.models.enumerations.MIMEType;
import ch.protonmail.android.api.models.enumerations.PackageType;

public class SendPreference implements Serializable {

    private String email;
    private boolean encrypt;
    private boolean sign;
    private MIMEType mimeType;
    private String publicKey;
    private PackageType scheme;
    private boolean primaryPinned;
    private boolean isVerified;
    private boolean hasPinned;
    private boolean isOwnAddress;

    public SendPreference(String email, boolean encrypt, boolean sign,
                          MIMEType mimeType, String publicKey, PackageType scheme, boolean primaryPinned,
                          boolean hasPinned, boolean isVerified, boolean isOwnAddress) {
        this.email = email;
        this.encrypt = encrypt;
        this.sign = sign;
        this.mimeType = mimeType;
        this.publicKey = publicKey;
        this.scheme = scheme;
        this.primaryPinned = primaryPinned;
        this.hasPinned = hasPinned;
        this.isVerified = isVerified;
        this.isOwnAddress = isOwnAddress;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public boolean isEncryptionEnabled() {
        return encrypt;
    }

    public boolean isSignatureEnabled() {
        return sign;
    }

    public PackageType getEncryptionScheme() {
        return scheme;
    }

    public MIMEType getMimeType() {
        return mimeType;
    }

    public boolean isPrimaryPinned() {
        return primaryPinned;
    }

    public boolean hasPinnedKeys() {
        return hasPinned;
    }

    public boolean isOwnAddress() {
        return isOwnAddress;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public String getEmailAddress() {
        return email;
    }

    public boolean isPGP() {
        return scheme == PackageType.PGP_MIME || scheme == PackageType.PGP_INLINE ||
                (!encrypt && sign);
    }
}
