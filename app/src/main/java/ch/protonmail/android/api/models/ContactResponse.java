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

import com.google.gson.annotations.SerializedName;

import java.util.List;

import ch.protonmail.android.api.models.base.MultipleResponseBody;
import ch.protonmail.android.api.utils.Fields;
import ch.protonmail.android.data.local.model.FullContactDetails;
import ch.protonmail.android.data.local.model.FullContactDetailsFactory;
import ch.protonmail.android.data.local.model.ServerFullContactDetails;

public class ContactResponse extends MultipleResponseBody {

    @SerializedName(Fields.Response.RESPONSES)
    private List<Responses> responses;

    public List<ContactResponse.Responses> getResponses() {
        return responses;
    }

    public String getContactId() {
        if (responses == null) {
            return null;
        }
        Responses response = responses.get(0);
        if (response == null) {
            return null;
        }
        return response.getResponse().getContactId();
    }

    public int getResponseErrorCode() {
        return responses.get(0).getResponse().getCode();
    }

    public class Responses {
        @SerializedName(Fields.Response.INDEX)
        private int index;
        @SerializedName(Fields.Response.RESPONSE)
        private Response response;

        public String getError() {
            return response.getError();
        }

        public int getCode() {
            return response.getCode();
        }

        public Response getResponse() {
            return response;
        }
    }

    public class Response extends ResponseBody {
        @SerializedName(Fields.Response.CONTACT)
        private ServerFullContactDetails contact;

        public String getContactId() {
            return contact != null ? contact.getId() : "";
        }

        public FullContactDetails getContact() {
            FullContactDetailsFactory fullContactDetailsFactory=new FullContactDetailsFactory();
            return fullContactDetailsFactory.createFullContactDetails(contact);
        }
    }
}
