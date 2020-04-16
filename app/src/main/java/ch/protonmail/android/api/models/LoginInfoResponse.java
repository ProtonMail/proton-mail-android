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
import android.os.Parcelable;
import androidx.annotation.Nullable;

public class LoginInfoResponse extends ResponseBody implements Parcelable {

    private String Modulus;
    private String ServerEphemeral;
    private int Version;
    private String Salt;
    private String SRPSession;

    public LoginInfoResponse() {
    }

    protected LoginInfoResponse(Parcel in) {
        super(in);
        Modulus = in.readString();
        ServerEphemeral = in.readString();
        Version = in.readInt();
        Salt = in.readString();
        SRPSession = in.readString();
    }

    public static final Creator<LoginInfoResponse> CREATOR = new Creator<LoginInfoResponse>() {
        @Override
        public LoginInfoResponse createFromParcel(Parcel in) {
            return new LoginInfoResponse(in);
        }

        @Override
        public LoginInfoResponse[] newArray(int size) {
            return new LoginInfoResponse[size];
        }
    };

    public String getModulus() {
        return Modulus;
    }

    public String getServerEphemeral() {
        return ServerEphemeral;
    }

    public int getAuthVersion() {
        return Version;
    }

    public String getSalt() {
        return Salt;
    }

    public String getSRPSession() {
        return SRPSession;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(Modulus);
        dest.writeString(ServerEphemeral);
        dest.writeInt(Version);
        dest.writeString(Salt);
        dest.writeString(SRPSession);
    }
}
