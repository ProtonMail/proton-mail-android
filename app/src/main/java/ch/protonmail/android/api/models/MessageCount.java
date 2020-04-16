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

import java.io.Serializable;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dkadrikj on 18.8.15.
 */
public class MessageCount implements Serializable {
    @SerializedName(Fields.Message.LABEL_ID)
    private String labelId;
    @SerializedName(Fields.Message.TOTAL)
    private int total;
    @SerializedName(Fields.Unread.UNREAD)
    private int unread;

    public String getLabelId() {
        return labelId;
    }

    public int getTotal() {
        return total;
    }

    public int getUnread() {
        return unread;
    }
}
