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

import ch.protonmail.android.api.models.room.contacts.ContactEmail;
import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 8/22/16.
 */
public class ContactEmailsResponseV2 extends ResponseBody {

    @SerializedName(Fields.Contact.TOTAL)
    private int total;
    @SerializedName(Fields.Contact.CONTACT_EMAILS)
    private List<ContactEmail> contactEmails;

    public List<ContactEmail> getContactEmails() {
        return contactEmails;
    }

    public int getTotal() {
        return total;
    }
}
