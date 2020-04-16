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

public class SrpResponseBody extends ResponseBody {
    private String ServerProof;

    public String getServerProof() {
        return ServerProof;
    }

    public SrpResponseBody(Parcel in) {
        super(in);
        ServerProof = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeString(ServerProof);
    }

    public static final Creator<SrpResponseBody> CREATOR = new Creator<SrpResponseBody>() {
        @Override
        public SrpResponseBody createFromParcel(Parcel in) {
            return new SrpResponseBody(in);
        }

        @Override
        public SrpResponseBody[] newArray(int size) {
            return new SrpResponseBody[size];
        }
    };
}
