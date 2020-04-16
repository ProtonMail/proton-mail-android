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
package ch.protonmail.android.api.models.address;

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 4/27/16.
 */
public class CondensedAddress {

    @SerializedName(Fields.Addresses.DISPLAY_NAME)
    private String displayName; // optional, use null to use default (from user object)
    @SerializedName(Fields.Addresses.SIGNATURE)
    private String signature; // optional, use null to use default (from user object)

    public CondensedAddress(String displayName, String signature) {
        this.displayName = displayName;
        this.signature = signature;
    }
}
