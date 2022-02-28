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

import android.text.TextUtils;

import com.google.gson.annotations.SerializedName;

import java.util.List;

import ch.protonmail.android.api.models.base.MultipleResponseBody;
import ch.protonmail.android.api.utils.Fields;

public class MoveToFolderResponse extends MultipleResponseBody {

    @SerializedName(Fields.Response.RESPONSES)
    private List<Responses> responses;

    private class Responses {
        @SerializedName(Fields.Response.ID)
        private String id;
        @SerializedName(Fields.Response.RESPONSE)
        private ResponseBody response;

        public boolean hasError() {
            return !TextUtils.isEmpty(response.getError());
        }

        public String getError() {
            return response.getError();
        }

        public int getCode() {
            return response.getCode();
        }

        public ResponseBody getResponse() {
            return response;
        }
    }
}
