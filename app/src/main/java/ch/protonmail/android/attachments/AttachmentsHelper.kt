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

package ch.protonmail.android.attachments

import android.app.NotificationManager
import android.content.Context
import android.util.Base64
import androidx.core.app.NotificationCompat
import ch.protonmail.android.R
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.servers.notification.NotificationServer
import okio.buffer
import okio.source
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

private const val NOTIFICATION_ID = 213_412
private const val FULL_PROGRESS = 100
private const val BASE_64 = "base64"

class AttachmentsHelper @Inject constructor(
    private val context: Context,
    private val api: ProtonMailApiManager,
    private val notificationManager: NotificationManager
) {
    private var notificationBuilder: NotificationCompat.Builder? = null

    /**
     * Creates unique filename from original one in given directory.
     */
    fun createUniqueFilename(originalFilename: String, folder: File): String {

        val sanitizedOriginalFilename = originalFilename.replace(" ", "_").replace("/", ":")

        if (!File(folder, sanitizedOriginalFilename).exists()) return sanitizedOriginalFilename

        val name = sanitizedOriginalFilename.substringBeforeLast('.', "")
        val extension = sanitizedOriginalFilename.substringAfterLast('.', "")
        var counter = 0
        do {
            counter++
        } while (File(folder, "$name($counter).$extension").exists())

        return "$name($counter).$extension"
    }

    private fun initializeNotificationBuilder(
        filename: String
    ): NotificationCompat.Builder {
        val channelId = NotificationServer(context, notificationManager).createAttachmentsChannel()

        return NotificationCompat.Builder(context, channelId)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(filename)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentText(context.getString(R.string.download_in_progress))
            .setProgress(FULL_PROGRESS, 0, false)
    }

    /**
     * If fileSize is provided, progress notification will be shown.
     */
    suspend fun getAttachmentData(
        crypto: AddressCrypto,
        mimeData: ByteArray?,
        attachmentId: String,
        key: String?,
        fileSize: Long = -1L,
        uniqueFileName: String? = null
    ): ByteArray? {
        if (mimeData != null) {
            return mimeData
        }

        uniqueFileName?.let {
            notificationBuilder = initializeNotificationBuilder(it)
        }

        return try {
            val response = api.downloadAttachment(
                attachmentId
            )

            response?.byteStream()?.source()?.buffer()?.use { bufferedSource ->
                val byteArray = bufferedSource.readByteArray()
                val keyBytes = Base64.decode(key, Base64.DEFAULT)
                crypto.decryptAttachment(CipherText(keyBytes, byteArray)).decryptedData
            }
        } catch (exception: IOException) {
            Timber.w(exception, "getAttachmentData exception")
            null
        } finally {
            notifyOfProgress(FULL_PROGRESS)
        }
    }

    private fun notifyOfProgress(progress: Int) {
        if (progress == FULL_PROGRESS) {
            notificationManager.cancel(NOTIFICATION_ID)
        } else {
            notificationBuilder?.let { builder ->
                builder.setProgress(FULL_PROGRESS, progress, false)
                notificationManager.notify(NOTIFICATION_ID, builder.build())
            }
        }
    }

    fun fromAttachmentToEmbededImage(
        attachment: Attachment,
        embeddedImagesArray: List<String>
    ): EmbeddedImage? {
        val headers = attachment.headers ?: return null
        val contentDisposition = headers.contentDisposition
        var contentId = if (headers.contentId.isNullOrEmpty()) {
            headers.contentLocation
        } else {
            headers.contentId
        }
        contentId = contentId?.removeSurrounding("<", ">")
        if (contentDisposition != null) {
            if (contentDisposition.isEmpty()) {
                return null
            } else {
                var containsInlineMarker = false

                for (element in contentDisposition) {
                    if (!element.isNullOrEmpty() && element.contains("inline")) {
                        containsInlineMarker = true
                        break
                    }
                }
                if (!containsInlineMarker && !embeddedImagesArray.contains(contentId)) {
                    return null
                }
            }
        }

        if (attachment.attachmentId.isNullOrEmpty()) {
            return null
        }
        val fileName = attachment.fileName
        if (fileName.isNullOrEmpty()) {
            return null
        }
        val encoding = headers.contentTransferEncoding
        val contentType = headers.contentType
        val mimeData = attachment.mimeData
        val embeddedMimeTypes = listOf("image/gif", "image/jpeg", "image/png", "image/bmp")
        return if (!embeddedMimeTypes.contains(attachment.mimeTypeFirstValue?.toLowerCase(Locale.ENGLISH))) {
            null
        } else EmbeddedImage(
            attachment.attachmentId ?: "",
            fileName,
            attachment.keyPackets ?: "",
            if (contentType.isEmpty()) {
                attachment.mimeType ?: ""
            } else {
                contentType
            },
            if (encoding.isEmpty()) BASE_64 else encoding,
            contentId ?: headers.contentLocation,
            mimeData,
            attachment.fileSize,
            attachment.messageId,
            null
        )
    }
}
