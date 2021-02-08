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

import android.annotation.TargetApi
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.jobs.helper.EmbeddedImage
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import javax.inject.Inject

private const val BASE_64 = "base64"

class AttachmentsHelper @Inject constructor(
    private val api: ProtonMailApiManager
) {

    suspend fun getAttachmentData(
        crypto: AddressCrypto,
        attachmentId: String,
        key: String?
    ): ByteArray? = try {
        val responseBody = api.downloadAttachment(attachmentId)

        responseBody?.byteStream()?.source()?.buffer()?.use { bufferedSource ->
            val byteArray = bufferedSource.readByteArray()
            val keyBytes = Base64.decode(key, Base64.DEFAULT)
            crypto.decryptAttachment(CipherText(keyBytes, byteArray)).decryptedData
        }
    } catch (exception: IOException) {
        Timber.w(exception, "getAttachmentData exception")
        null
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

    @TargetApi(Build.VERSION_CODES.Q)
    fun saveAttachmentInMediaStore(
        contentResolver: ContentResolver,
        filename: String,
        attachmentMimeType: String?,
        inputStream: InputStream
    ): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, attachmentMimeType)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val newUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)

        newUri?.let {
            contentResolver.openOutputStream(newUri)?.use { outputStream ->
                outputStream.sink().buffer().apply {
                    writeAll(inputStream.source())
                    close()
                }
                Timber.v("Stored Q file: $filename type: $attachmentMimeType uri: $newUri")
            }

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            contentResolver.update(newUri, values, null, null)
        } ?: throw IllegalStateException("MediaStore insert has failed")
        return newUri
    }
}
