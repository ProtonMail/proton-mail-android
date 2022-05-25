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
package ch.protonmail.android.events;


import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DownloadedAttachmentEvent {

    private final Status status;
    private final String filename;
    private final Uri attachmentUri;
    private final String attachmentId;
    private final String messageId;
    private final boolean offlineLoaded;

    public DownloadedAttachmentEvent(
            Status status,
            @NonNull String filename,
            @Nullable Uri attachmentUri,
            String attachmentId,
            String messageId,
            boolean offlineLoaded
    ){
        this.status = status;
        this.filename = filename;
        this.attachmentUri = attachmentUri;
        this.attachmentId = attachmentId;
        this.messageId = messageId;
        this.offlineLoaded = offlineLoaded;
    }

    public Status getStatus(){
        return status;
    }

    @NonNull
    public String getFilename(){
        return filename;
    }

    @Nullable
    public Uri getAttachmentUri(){
        return attachmentUri;
    }

    public String getAttachmentId() {
        return attachmentId;
    }

    public String getMessageId() {
        return messageId;
    }

    public boolean isOfflineLoaded() {
        return offlineLoaded;
    }
}
