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
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.jobs.helper.EmbeddedImage
import okio.buffer
import okio.sink
import okio.source
import timber.log.Timber
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.Locale
import javax.inject.Inject

private const val BASE_64 = "base64"

/**
 * Common methods used by Download attachments components.
 */
class AttachmentsHelper @Inject constructor(
    private val context: Context
) {

    fun fromAttachmentToEmbeddedImage(
        attachment: Attachment,
        embeddedImages: List<String>
    ): EmbeddedImage? {
        if (
            attachment.headers == null ||
            attachment.attachmentId.isNullOrEmpty() ||
            attachment.fileName.isNullOrEmpty()
        ) {
            return null
        }

        val headers = requireNotNull(attachment.headers)
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
                if (!containsInlineMarker && !embeddedImages.contains(contentId)) {
                    return null
                }
            }
        }

        val embeddedMimeTypes = listOf("image/gif", "image/jpeg", "image/jpg", "image/png", "image/bmp")
        return if (!embeddedMimeTypes.contains(attachment.mimeTypeFirstValue?.toLowerCase(Locale.ENGLISH))) {
            Timber.w("Unsupported embedded image format ${attachment.mimeTypeFirstValue}")
            null
        } else
            EmbeddedImage(
                attachment.attachmentId ?: "",
                requireNotNull(attachment.fileName),
                attachment.keyPackets ?: "",
                if (headers.contentType.isEmpty()) {
                    attachment.mimeType ?: ""
                } else {
                    headers.contentType
                },
                if (headers.contentTransferEncoding.isEmpty()) BASE_64 else headers.contentTransferEncoding,
                contentId ?: headers.contentLocation,
                attachment.mimeData,
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
        Timber.v("saveAttachment attachmentMimeType: $attachmentMimeType, newUri: $newUri")

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

    fun isFileAvailable(uri: Uri?): Boolean {
        uri ?: return false
        val doesFileExist = try {
            context.contentResolver.openInputStream(uri)?.use {
                it.close()
                true
            } ?: false
        } catch (fileException: FileNotFoundException) {
            Timber.v(fileException, "Uri not found")
            false
        }
        Timber.d("doesFileExist: $doesFileExist")
        return doesFileExist
    }
}
