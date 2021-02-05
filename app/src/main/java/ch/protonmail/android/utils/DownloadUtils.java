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
package ch.protonmail.android.utils;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.servers.notification.INotificationServer;
import ch.protonmail.android.servers.notification.NotificationServer;
import timber.log.Timber;

public class DownloadUtils {

    public static void viewAttachment(Context context, String filename, @Nullable Uri uri) {
        if (uri != null) {
            String mimeType = "";
            final ContentResolver resolver = context.getContentResolver();
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                mimeType = resolver.getType(uri);
                Cursor cursor = resolver.query(uri, null, null, null, null);
                cursor.close();
            } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                String extension = MimeTypeMap.getFileExtensionFromUrl(filename);
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());

                if (mimeType == null) {
                    mimeType = Constants.MIME_TYPE_UNKNOWN_FILE;
                }
            }
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setType(mimeType);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setDataAndType(uri, mimeType);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException notFoundException) {
                Timber.i(notFoundException, "Unable to view attachment");
            }
        }
    }

    public static void viewAttachment(Context context, String fileName, @Nullable Uri uri, boolean showNotification) {
        if (uri != null) {
            String mimeType = "";
            final ContentResolver resolver = context.getContentResolver();
            if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
                mimeType = resolver.getType(uri);
                Cursor cursor = resolver.query(uri, null, null, null, null);
                cursor.close();
            } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
                if (!TextUtils.isEmpty(extension)) {
                    extension = extension.toLowerCase();
                }
                mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());

                if (mimeType == null) {
                    mimeType = Constants.MIME_TYPE_UNKNOWN_FILE;
                }
            }

            NotificationManager notifyManager = (NotificationManager) context.getSystemService(
                    Context.NOTIFICATION_SERVICE);
            INotificationServer notificationServer = new NotificationServer(context, notifyManager);
            notificationServer.notifyAboutAttachment(fileName, uri, mimeType, showNotification);
        }
    }
}
