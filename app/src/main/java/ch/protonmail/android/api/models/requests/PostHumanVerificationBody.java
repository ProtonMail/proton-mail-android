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
package ch.protonmail.android.api.models.requests;

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

/**
 * Created by dino on 12/25/16.
 */

public class PostHumanVerificationBody {
    @SerializedName(Fields.User.TOKEN)
    private String token;
    @SerializedName(Fields.User.TOKEN_TYPE)
    private String tokenType;

    public PostHumanVerificationBody(String token, Constants.TokenType tokenType) {
        this.token = token;
        this.tokenType = tokenType.getTokenTypeValue();
    }
}
