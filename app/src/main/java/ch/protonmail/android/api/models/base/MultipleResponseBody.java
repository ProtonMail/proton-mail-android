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
package ch.protonmail.android.api.models.base;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

import ch.protonmail.android.api.utils.Fields;

public class MultipleResponseBody implements Parcelable {
    @SerializedName(Fields.Response.CODE)
    private int code;
    @SerializedName(Fields.Response.ERROR)
    private String error;
    @SerializedName(Fields.Response.ERROR_DETAILS)
    private Map<String, String> details;

    public MultipleResponseBody() {
    }

    private MultipleResponseBody(Parcel in) {
        code = in.readInt();
    }

    public static final Creator<MultipleResponseBody> CREATOR = new Creator<MultipleResponseBody>() {
        @Override
        public MultipleResponseBody createFromParcel(Parcel in) {
            return new MultipleResponseBody(in);
        }

        @Override
        public MultipleResponseBody[] newArray(int size) {
            return new MultipleResponseBody[size];
        }
    };

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
    }
}
