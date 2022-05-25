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
package ch.protonmail.android.api.models.messages.send;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import ch.protonmail.android.api.utils.Fields;

public class MessageSendBody implements Serializable {

    private static final long serialVersionUID = -2606714802157598016L;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageSendBody that = (MessageSendBody) o;
        return autoSaveContacts == that.autoSaveContacts &&
                Objects.equals(expiresIn, that.expiresIn) &&
                Objects.equals(packages, that.packages);
    }

    @Override
    public int hashCode() {
        return Objects.hash(expiresIn, autoSaveContacts, packages);
    }
}
