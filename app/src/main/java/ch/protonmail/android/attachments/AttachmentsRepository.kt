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

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.AttachmentHeaders
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.crypto.AddressCrypto
import okhttp3.MediaType
import okhttp3.RequestBody
import javax.inject.Inject

class AttachmentsRepository @Inject constructor(
    private val crypto: AddressCrypto,
    private val apiManager: ProtonMailApiManager,
    private val armorer: Armorer
) {

    fun upload(attachment: Attachment) {
        val headers = attachment.headers
        val mimeType = requireNotNull(attachment.mimeType)
        val fileContent = attachment.getFileContent()
        val filename = requireNotNull(attachment.fileName)

        val encryptedAttachment = crypto.encrypt(fileContent, filename)
        val keyPackage = RequestBody.create(MediaType.parse(mimeType), encryptedAttachment.keyPacket)
        val dataPackage = RequestBody.create(MediaType.parse(mimeType), encryptedAttachment.dataPacket)
        val signedFileContent = armorer.unarmor(crypto.sign(fileContent))
        val signature = RequestBody.create(MediaType.parse("application/octet-stream"), signedFileContent)

        if (isAttachmentInline(headers)) {
            requireNotNull(headers)

            apiManager.uploadAttachmentInline(
                attachment,
                attachment.messageId,
                contentIdFormatted(headers),
                keyPackage,
                dataPackage,
                signature
            )
        } else {
            apiManager.uploadAttachment(attachment, attachment.messageId, keyPackage, dataPackage, signature)
        }

//        if (response.code == Constants.RESPONSE_CODE_OK) {
//            attachmentId = response.attachmentID
//            keyPackets = response.attachment.keyPackets
//            this.signature = response.attachment.signature
//            isUploaded = true
//            messageDetailsRepository.saveAttachment(this)
//        }
//        return attachmentId
    }

    private fun contentIdFormatted(headers: AttachmentHeaders): String {
        val contentId = headers.contentId
        val parts = contentId.split("<").dropLastWhile { it.isEmpty() }.toTypedArray()
        if (parts.size > 1) {
            return parts[1].replace(">", "")
        }
        return contentId
    }

    private fun isAttachmentInline(headers: AttachmentHeaders?) =
        headers != null &&
            headers.contentDisposition.contains("inline") &&
            headers.contentId != null
}
