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
package ch.protonmail.android.jobs.helper

import android.text.TextUtils
import ch.protonmail.android.api.models.room.messages.Attachment
import java.util.*

// region constants
private const val BASE_64 = "base64"
// endregion

/**
 * Created by dkadrikj on 7/16/16.
 */

class EmbeddedImage private constructor(
        val attachmentId: String,
        fileName: String,
        val key: String,
        val contentType: String,
        val encoding: String,
        val contentId: String,
        val mimeData: ByteArray?,
        val size: Long,
        val messageId: String,
        var localFileName: String?) {
    var fileName: String? = null

    init {
        this.fileName = fileName.replace(" ", "_")
    }

    companion object {

        fun fromAttachment(attachment: Attachment, embeddedImagesArray: List<String>): EmbeddedImage? {
            val headers = attachment.headers ?: return null
            val contentDisposition = headers.contentDisposition
            var contentId = headers.contentId
            if (TextUtils.isEmpty(contentId)) {
                contentId = headers.contentLocation
            }
            if (!TextUtils.isEmpty(contentId)) {
                contentId = contentId.removeSurrounding("<", ">")
            }
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

            if (TextUtils.isEmpty(attachment.attachmentId)) {
                return null
            }
            val fileName = attachment.fileName
            if (TextUtils.isEmpty(fileName)) {
                return null
            }
            val encoding = headers.contentTransferEncoding
            val contentType = headers.contentType
            val mimeData = attachment.mimeData
            val embeddedMimeTypes = Arrays.asList("image/gif", "image/jpeg", "image/png", "image/bmp")
            return if (!embeddedMimeTypes.contains(attachment.mimeTypeFirstValue?.toLowerCase())) {
                null
            } else EmbeddedImage(attachment.attachmentId ?: "",
                    fileName!!,
                    attachment.keyPackets ?: "",
                    if (TextUtils.isEmpty(contentType)) {
                        attachment.mimeType ?: ""
                    } else {
                        contentType
                    },
                    if (TextUtils.isEmpty(encoding)) BASE_64 else encoding,
                    contentId,
                    mimeData,
                    attachment.fileSize,
                    attachment.messageId,
                    null)
        }
    }
}
