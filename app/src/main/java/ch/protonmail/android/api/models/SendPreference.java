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
    private boolean isContactSignatureVerified;
    // NB: pinned key information can only be trusted if `isContactSignatureVerified` is true.
    // After a password reset, it can happen that `isContactSignatureVerified` will be false, but we still have
    // pinned key information.
    private boolean isPublicKeyPinned;
    private boolean hasPinnedKeys;
    private boolean isOwnAddress;

    public SendPreference(String email, boolean encrypt, boolean sign,
                          MIMEType mimeType, String publicKey, PackageType scheme, boolean isPublicKeyPinned,
                          boolean hasPinnedKeys, boolean isContactSignatureVerified, boolean isOwnAddress) {
        this.email = email;
        this.encrypt = encrypt;
        this.sign = sign;
        this.mimeType = mimeType;
        this.publicKey = publicKey;
        this.scheme = scheme;
        this.isPublicKeyPinned = isPublicKeyPinned;
        this.hasPinnedKeys = hasPinnedKeys;
        this.isContactSignatureVerified = isContactSignatureVerified;
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

    /**
     * @return whether `publicKey` comes from the user's pinned keys.
     * NB: this info can only be trusted if `this.isVerified()` is also true.
     */
    public boolean isPublicKeyPinned() {
        return isPublicKeyPinned;
    }

    /**
     * @return whether the user has pinned keys for the given contact's address.
     * NB: this info can only be trusted if `this.isVerified()` is true.
     */
    public boolean hasPinnedKeys() {
        return hasPinnedKeys;
    }

    public boolean isOwnAddress() {
        return isOwnAddress;
    }

    /**
     * @return whether the send preference information is trusted, as it could be cryptographically verified.
     */
    public boolean isVerified() {
        return isContactSignatureVerified;
    }

    public String getEmailAddress() {
        return email;
    }

    public boolean isPGP() {
        return scheme == PackageType.PGP_MIME || scheme == PackageType.PGP_INLINE ||
                (!encrypt && sign);
    }
}
