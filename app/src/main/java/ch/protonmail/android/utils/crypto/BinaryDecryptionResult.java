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

/**
 * Created by kaylukas on 17/05/2018.
 */

public class BinaryDecryptionResult extends AbstractDecryptionResult {

    private byte[] decrypted;

    public BinaryDecryptionResult(byte[] decrypted, boolean hasSignature, boolean signatureIsValid) {
        super(hasSignature, signatureIsValid);
        this.decrypted = decrypted;
    }

    public BinaryDecryptionResult(byte[] decrypted, long verificationResult) {
        super(verificationResult);
        this.decrypted = decrypted;
    }

    public byte[] getDecryptedData() {
        return decrypted;
    }
}
