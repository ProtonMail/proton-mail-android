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
package ch.protonmail.android.api.models.messages.send;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

import ch.protonmail.android.api.utils.Fields;

/**
 * Created by dino on 3/15/18.
 */

public class MessageSendBody implements Serializable {

    @SerializedName(Fields.Message.Send.EXPIRES_IN)
    private Long expiresIn;
    @SerializedName(Fields.Message.Send.AUTO_SAVE_CONTACTS)
    private int autoSaveContacts;
    @SerializedName(Fields.Message.Send.PACKAGES)
    private List<MessageSendPackage> packages;

    public MessageSendBody(List<MessageSendPackage> packages, Long expiresIn, int autoSaveContacts) {
        this.packages = packages;
        this.expiresIn = expiresIn;
        this.autoSaveContacts = autoSaveContacts;
    }
}
