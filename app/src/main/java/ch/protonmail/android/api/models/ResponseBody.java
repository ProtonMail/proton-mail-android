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

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.Map;

import ch.protonmail.android.api.utils.Fields;

public class ResponseBody implements Parcelable {
    @SerializedName(Fields.Response.CODE)
    private int code;
    @SerializedName(Fields.Response.ERROR)
    private String error;
    @SerializedName(Fields.Response.ERROR_DETAILS)
    private Map<String, Object> details;

    public ResponseBody() {
    }

    public ResponseBody(Parcel in) {
        code = in.readInt();
        error = in.readString();
    }

    public static final Creator<ResponseBody> CREATOR = new Creator<ResponseBody>() {
        @Override
        public ResponseBody createFromParcel(Parcel in) {
            return new ResponseBody(in);
        }

        @Override
        public ResponseBody[] newArray(int size) {
            return new ResponseBody[size];
        }
    };

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
        dest.writeString(error);
    }
}
