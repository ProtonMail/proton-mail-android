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
package ch.protonmail.android.utils

import android.app.NotificationManager
import android.content.ActivityNotFoundException
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.MimeTypeMap
import ch.protonmail.android.core.Constants
import ch.protonmail.android.notifications.presentation.utils.NotificationServer
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class DownloadUtils @Inject constructor(
    private val context: Context
) {

    fun viewAttachment(filename: String?, uri: Uri?) {
        if (uri != null) {
            val mimeType = getMimeType(uri, filename)
            Timber.d("viewAttachment mimeType: $mimeType uri: $uri uriScheme: ${uri.scheme}")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                type = mimeType
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                setDataAndType(uri, mimeType)
            }
            try {
                context.startActivity(intent)
            } catch (notFoundException: ActivityNotFoundException) {
                Timber.i(notFoundException, "Unable to view attachment")
            }
        }
    }

    fun viewAttachmentNotification(
        fileName: String,
        uri: Uri?,
        showNotification: Boolean
    ) {
        if (uri != null) {
            val mimeType = getMimeType(uri, fileName)

            Timber.d("viewAttachmentNotification mimeType: $mimeType uri: $uri")
            val notifyManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notificationServer = NotificationServer(context, notifyManager)
            notificationServer.notifyAboutAttachment(fileName, uri, mimeType, showNotification)
        }
    }

    fun getMimeType(uri: Uri, filename: String?): String? = when {
        ContentResolver.SCHEME_CONTENT == uri.scheme -> {
            val resolver = context.contentResolver
            resolver.getType(uri)
        }
        ContentResolver.SCHEME_FILE == uri.scheme -> {
            val extension = MimeTypeMap.getFileExtensionFromUrl(filename)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase(Locale.ENGLISH))
        }
        else -> {
            Constants.MIME_TYPE_UNKNOWN_FILE
        }
    }
}
