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

import ch.protonmail.android.api.models.messages.receive.AttachmentFactory;
import ch.protonmail.android.api.models.messages.receive.ServerAttachment;
import ch.protonmail.android.api.models.room.messages.Attachment;
import ch.protonmail.android.api.utils.Fields;

public class AttachmentUploadResponse extends ResponseBody {

    @SerializedName(Fields.Attachment.ATTACHMENT_ID)
    private String attachmentId;
    @SerializedName(Fields.Attachment.ATTACHMENT)
    private ServerAttachment attachment;
    @SerializedName(Fields.Attachment.SIZE)
    private long Size;

    public String getAttachmentID() {
        return attachment.getID();
    }

    public Attachment getAttachment() {
        return new AttachmentFactory().createAttachment(attachment);
    }

    public long getSize() {
        return Size;
    }
}
