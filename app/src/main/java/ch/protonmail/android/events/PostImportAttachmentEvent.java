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
package ch.protonmail.android.events;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;

public class PostImportAttachmentEvent implements Serializable {

    public final String uri;
    public final String displayName;
    public final long size;
    public final String mimeType;
    @Nullable public final String composerInstanceId;

    public PostImportAttachmentEvent(@NonNull Uri uri, @NonNull String displayName, long size, @NonNull String mimeType, String composerInstanceId) {
        this.uri = uri.toString();
        this.displayName = displayName;
        this.size = size;
        this.mimeType = mimeType;
        this.composerInstanceId = composerInstanceId;
    }
}
