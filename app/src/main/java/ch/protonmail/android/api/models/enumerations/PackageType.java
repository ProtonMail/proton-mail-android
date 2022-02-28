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
package ch.protonmail.android.api.models.enumerations;

/**
 * Created by kaylukas on 01/05/2018.
 */

public enum PackageType {
    PM(1), EO(2), CLEAR(4), PGP_INLINE(8), PGP_MIME(16), MIME(32);

    private final int value;

    PackageType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PackageType fromInteger(int val) {
        for (PackageType type : PackageType.values()) {
            if (type.getValue() == val) {
                return type;
            }
        }

        return null;
    }
}