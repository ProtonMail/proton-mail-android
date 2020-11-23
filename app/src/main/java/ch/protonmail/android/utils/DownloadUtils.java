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
import android.os.Environment;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.core.content.FileProvider;

import java.io.File;

import ch.protonmail.android.core.Constants;
import ch.protonmail.android.servers.notification.INotificationServer;
import ch.protonmail.android.servers.notification.NotificationServer;

/**
 * Created by dino on 1/19/17.
 */

public class DownloadUtils {

    public static void viewAttachment(Context context, String filename) {
        String ext = filename.substring(filename.lastIndexOf(".") + 1);
        filename = filename.replace(filename.substring(filename.lastIndexOf(".") + 1), ext.toLowerCase());
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + Constants.DIR_ATTACHMENT_DOWNLOADS, filename);
        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        String mimeType = "";
        final ContentResolver resolver = context.getContentResolver();

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            mimeType = resolver.getType(uri);
            Cursor cursor = resolver.query(uri, null, null, null, null);
            cursor.close();
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(file.getName());
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
        } catch (ActivityNotFoundException e) {
            // NOOP
        }
    }

    public static void viewAttachment(Context context, String filename, boolean showNotification) {
        String ext = filename.substring(filename.lastIndexOf(".") + 1);
        filename = filename.replace(filename.substring(filename.lastIndexOf(".") + 1), ext.toLowerCase());
        File file = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) + Constants.DIR_ATTACHMENT_DOWNLOADS, filename);
        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        String mimeType = "";
        final ContentResolver resolver = context.getContentResolver();

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            mimeType = resolver.getType(uri);
            Cursor cursor = resolver.query(uri, null, null, null, null);
            cursor.close();
        } else if (ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            String fileName = file.getName();
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
        notificationServer.notifyAboutAttachment(filename, uri, mimeType, showNotification);
    }

    public static void viewCachedAttachmentFile(Context context, String filename, String localLocation) {
        String extension = filename.substring(filename.lastIndexOf(".") + 1);

        File file = new File(context.getApplicationContext().getFilesDir().toString() + Constants.DIR_EMB_ATTACHMENT_DOWNLOADS, localLocation);
        Uri uri = FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);

        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());

        if (mimeType == null) {
            ContentResolver resolver = context.getContentResolver();
            mimeType = resolver.getType(uri);
            Cursor cursor = resolver.query(uri, null, null, null, null);
            cursor.close();
        }

        if (mimeType == null) {
            mimeType = Constants.MIME_TYPE_UNKNOWN_FILE;
        }

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType(mimeType);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.setDataAndType(uri, mimeType);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // NOOP
        }
    }
}
