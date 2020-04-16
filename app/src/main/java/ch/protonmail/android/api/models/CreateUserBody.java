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

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 17.7.15.
 */
public class CreateUserBody {

    private String Username;
    private int News;
    private String Token;
    private String TokenType;
    private String Salt;
    // We'd like to just use PasswordVerifier here, but Gson is problematic
    @SerializedName(Fields.Auth.AUTH)
    private Auth auth;
    @SerializedName(Fields.Auth.PAYLOAD)
    private Payload payload;

    /**
     * Constructor.
     */
    public CreateUserBody(String username, final PasswordVerifier verifier, int news, String tokenType, String token, String salt, final String jwsResult) {
        Username = username;
        News = news;
        TokenType = tokenType;
        Token = token;
        auth = new Auth(verifier.AuthVersion, verifier.ModulusID, verifier.Salt, verifier.SRPVerifier);
        Salt = salt;
        payload = new Payload(jwsResult);
    }
}
