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

import java.util.List;

import ch.protonmail.android.api.utils.Fields;

public class DeleteContactResponse {

    @SerializedName(Fields.Response.CODE)
    private int code;
    @SerializedName(Fields.Response.RESPONSES)
    private List<Response> responses;

    public int getCode() {
        return code;
    }

    public List<Response> getResponses() {
        return responses;
    }

    public class Response {
        @SerializedName(Fields.Response.ID)
        private String id;
        @SerializedName(Fields.Response.RESPONSE)
        private ResponseBody responseBody;

        public String getId() {
            return id;
        }

        public ResponseBody getResponseBody() {
            return responseBody;
        }
    }
}
