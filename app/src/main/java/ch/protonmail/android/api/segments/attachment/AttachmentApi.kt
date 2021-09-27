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
package ch.protonmail.android.api.segments.attachment

import ch.protonmail.android.api.ProgressListener
import ch.protonmail.android.api.interceptors.ProtonMailAttachmentRequestInterceptor
import ch.protonmail.android.api.models.AttachmentUploadResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.core.ProtonMailApplication
import okhttp3.RequestBody
import java.io.IOException

class AttachmentApi(
    private val basicService: AttachmentService,
    private val downloadService: AttachmentDownloadService,
    private val requestInterceptor: ProtonMailAttachmentRequestInterceptor,
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

    @Throws(IOException::class)
    override fun downloadAttachmentBlocking(attachmentId: String, progressListener: ProgressListener): ByteArray {
        // This works concurrently: nextProgressListener will block if the last one hasn't been consumed yet
        requestInterceptor.nextProgressListener(progressListener)
        return downloadService.downloadAttachmentBlocking(attachmentId).execute().body()!!.bytes()
    }

    @Throws(IOException::class)
    override fun uploadAttachmentInlineBlocking(
        attachment: Attachment,
        MessageID: String,
        contentID: String,
        KeyPackage: RequestBody,
        DataPackage: RequestBody,
        Signature: RequestBody
    ): AttachmentUploadResponse {
        val filename = attachment.fileName!!
        val mimeType = attachment.mimeType!!
        return ParseUtils.parse(
            uploadService.uploadAttachmentBlocking(
                filename, MessageID, contentID, mimeType, KeyPackage, DataPackage, Signature
            )
                .execute()
        )
    }

    @Throws(IOException::class)
    override fun uploadAttachmentBlocking(
        attachment: Attachment,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse {
        val filename = attachment.fileName!!
        val mimeType = attachment.mimeType!!
        val messageId = attachment.messageId
        return ParseUtils.parse(
            uploadService.uploadAttachmentBlocking(
                filename,
                messageId,
                mimeType,
                keyPackage,
                dataPackage,
                signature
            ).execute()
        )
    }

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

    override fun getAttachmentUrl(attachmentId: String): String {
        // return Constants.ENDPOINT_URI + "/attachments/" + attachmentId
        val prefs = ProtonMailApplication.getApplication().defaultSharedPreferences
        val apiUrl = Proxies.getInstance(null, prefs).getCurrentWorkingProxyDomain()
        return "$apiUrl/mail/v4/attachments/$attachmentId"
    }

}
