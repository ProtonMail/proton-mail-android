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

import android.os.Parcel;

import com.google.gson.annotations.SerializedName;

import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.core.Constants;

import static ch.protonmail.android.core.Constants.BEGIN_PGP;
import static ch.protonmail.android.core.Constants.END_PGP;

public class LoginResponse extends SrpResponseBody {

    private String AccessToken;
    private String TokenType;
    private String UID;
    private String UserID; // not used
    private String RefreshToken;
    private int UserStatus; // not used
    private String EventID; // not used
    private int PasswordMode;
    @SerializedName(Fields.Auth.TWOFA)
    private TwoFA twoFA;
    @SerializedName(Fields.Auth.SCOPE)
    private String scope;
    private String PrivateKey;
    private String KeySalt;

    public LoginResponse(Parcel in) {
        super(in);
        AccessToken = in.readString();
        TokenType = in.readString();
        UID = in.readString();
        RefreshToken = in.readString();
        PasswordMode = in.readInt();
        scope = in.readString();
        PrivateKey = in.readString();
        KeySalt = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(AccessToken);
        dest.writeString(TokenType);
        dest.writeString(UID);
        dest.writeString(RefreshToken);
        dest.writeInt(PasswordMode);
        dest.writeString(scope);
        dest.writeString(PrivateKey);
        dest.writeString(KeySalt);
    }

    public boolean isValid() {
        return AccessToken != null &&
                UID != null &&
                RefreshToken != null;
    }

    public boolean isAccessTokenArmored() {
        if (AccessToken == null) return false;
        return AccessToken.trim().startsWith(BEGIN_PGP) && AccessToken.trim().endsWith(END_PGP);
    }

    public String getAccessToken() {
        return AccessToken;
    }

    public String getRefreshToken() {
        return RefreshToken;
    }

    public String getUID() {
        return UID;
    }

    public String getPrivateKey() {
        return PrivateKey;
    }

    public String getKeySalt() {
        return KeySalt;
    }

    public Constants.PasswordMode getPasswordMode() {
        return Constants.PasswordMode.Companion.fromInt(PasswordMode);
    }

    public String getTokenType() {
        return TokenType;
    }

    public TwoFA getTwoFA() {
        return twoFA;
    }

    public String getScope() { return scope; }

    public static final Creator<LoginResponse> CREATOR = new Creator<LoginResponse>() {
        @Override
        public LoginResponse createFromParcel(Parcel in) {
            return new LoginResponse(in);
        }

        @Override
        public LoginResponse[] newArray(int size) {
            return new LoginResponse[size];
        }
    };
}
