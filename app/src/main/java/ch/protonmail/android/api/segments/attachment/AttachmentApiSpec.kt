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
import ch.protonmail.android.api.models.AttachmentUploadResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.room.messages.Attachment
import okhttp3.RequestBody
import java.io.IOException

interface AttachmentApiSpec {

    fun deleteAttachment(attachmentId: String): ResponseBody

    fun downloadAttachmentBlocking(attachmentId: String, progressListener: ProgressListener): ByteArray

    fun downloadAttachmentBlocking(attachmentId: String): ByteArray

    suspend fun downloadAttachment(attachmentId: String): okhttp3.ResponseBody?

    @Throws(IOException::class)
    fun uploadAttachmentInlineBlocking(
        attachment: Attachment,
        MessageID: String,
        contentID: String,
        KeyPackage: RequestBody,
        DataPackage: RequestBody,
        Signature: RequestBody
    ): AttachmentUploadResponse

    @Throws(IOException::class)
    fun uploadAttachmentBlocking(
        attachment: Attachment,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse

    fun getAttachmentUrl(attachmentId: String): String

    suspend fun uploadAttachment(
        attachment: Attachment,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse

    suspend fun uploadAttachmentInline(
        attachment: Attachment,
        messageID: String,
        contentID: String,
        keyPackage: RequestBody,
        dataPackage: RequestBody,
        signature: RequestBody
    ): AttachmentUploadResponse
}
