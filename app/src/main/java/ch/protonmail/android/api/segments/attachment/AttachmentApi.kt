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
package ch.protonmail.android.api.segments.attachment

import ch.protonmail.android.api.models.AttachmentUploadResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.data.local.model.Attachment
import okhttp3.RequestBody
import java.io.IOException

class AttachmentApi(
    private val basicService: AttachmentService,
    private val downloadService: AttachmentDownloadService,
    private val uploadService: AttachmentUploadService
) : BaseApi(), AttachmentApiSpec {

    @Throws(IOException::class)
    override fun deleteAttachment(attachmentId: String): ResponseBody =
        ParseUtils.parse(basicService.deleteAttachment(attachmentId).execute())

    override suspend fun downloadAttachment(attachmentId: String): okhttp3.ResponseBody? =
        downloadService.downloadAttachment(attachmentId).body()

    @Throws(IOException::class)
    override fun downloadAttachmentBlocking(attachmentId: String): ByteArray =
        downloadService.downloadAttachmentBlocking(attachmentId).execute().body()!!.bytes()

    override suspend fun uploadAttachmentInline(
        attachment: Attachment,
        messageID: String,
        contentID: String,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse {
        val filename = attachment.fileName!!
        val mimeType = attachment.mimeType!!
        return uploadService.uploadAttachment(
            filename,
            messageID,
            contentID,
            mimeType,
            keyPackage,
            dataPackage,
            signature
        )
    }

    override suspend fun uploadAttachment(
        attachment: Attachment,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse {
        val filename = attachment.fileName!!
        val mimeType = attachment.mimeType!!
        val messageId = attachment.messageId
        return uploadService.uploadAttachment(
            filename,
            messageId,
            mimeType,
            keyPackage,
            dataPackage,
            signature
        )
    }

}
