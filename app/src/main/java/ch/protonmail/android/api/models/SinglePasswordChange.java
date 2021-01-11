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

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.models.requests.PasswordChange;
import ch.protonmail.android.api.utils.Fields;
import io.sentry.util.Nullable;

/**
 * Created by dkadrikj on 11/8/16.
 */

public class SinglePasswordChange extends PasswordChange {

    @SerializedName(Fields.Auth.KEY_SALT)
    private String keySalt;
    @SerializedName(Fields.Auth.KEYS)
    private PrivateKeyBody[] keys;
    @SerializedName(Fields.Auth.USER_KEYS)
    private PrivateKeyBody[] userKeys;
    @SerializedName(Fields.Auth.ORGANIZATION_KEY)
    private String organizationKey;

    public SinglePasswordChange(
            String keySalt, PrivateKeyBody[] keys, PrivateKeyBody[] userKeys, String organizationKey,
            String srpSession, String clientEphemeral, String clientProof, String twoFactorCode, @Nullable PasswordVerifier newVerifier) {
        super(srpSession, clientEphemeral, clientProof, twoFactorCode, newVerifier);
        this.keySalt = keySalt;
        this.keys = keys;
        this.userKeys = userKeys;
        this.organizationKey = organizationKey;
    }

}
