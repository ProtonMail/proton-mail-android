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
package ch.protonmail.android.utils;

import android.content.Context;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;

import ch.protonmail.android.core.Constants;
import kotlin.Deprecated;
import timber.log.Timber;

public class FileUtils {

    public static void createDownloadsDir(Context context) {
        File attachmentFile = new File(context.getFilesDir() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS);
        if (!attachmentFile.exists()) {
            attachmentFile.mkdirs();
        }
    }

    @Deprecated(message = "Scheduled for deletion. Please use kolin serialization instead")
    public static String toString(Object value) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(out)) {
            objectOutputStream.writeObject(value);
        } catch (IOException e) {
            Timber.e(e, "Serialization of recipients failed ");
        }

        return Base64.encodeToString(out.toByteArray(), Base64.DEFAULT);
    }
}
