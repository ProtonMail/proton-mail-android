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

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 9/15/16.
 */

public class Auth {
    @SerializedName(Fields.Auth.VERSION)
    private int version;
    @SerializedName(Fields.Auth.MODULUS_ID)
    private String modulusId;
    @SerializedName(Fields.Auth.SALT)
    private String salt;
    @SerializedName(Fields.Auth.VERIFIER)
    private String srpVerifier;

    public Auth(int version, String modulusId, String salt, String srpVerifier) {
        this.version = version;
        this.modulusId = modulusId;
        this.salt = salt;
        this.srpVerifier = srpVerifier;
    }
}
