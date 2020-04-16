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
package ch.protonmail.android.api.models.enumerations;

/**
 * Created by kaylukas on 01/05/2018.
 */

public enum ContactEncryption {
    CLEARTEXT(0), ENCRYPTED(1), SIGNED(2), ENCRYPTED_AND_SIGNED(3);

    private int value;

    ContactEncryption(int value) {
        this.value = value;
    }

    public boolean isSigned() {
        return (value & SIGNED.value) == SIGNED.value;
    }

    public boolean isEncrypted() {
        return (value & ENCRYPTED.value) == ENCRYPTED.value;
    }

    public static ContactEncryption fromInteger(int val) {
        return ContactEncryption.values()[val];
    }
}
