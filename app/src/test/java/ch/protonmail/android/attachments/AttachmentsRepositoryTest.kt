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

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.AttachmentHeaders
import ch.protonmail.android.api.models.AttachmentUploadResponse
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.core.Constants
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifySequence
import okhttp3.MediaType
import okhttp3.RequestBody
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.assertEquals

@ExtendWith(MockKExtension::class)
class AttachmentsRepositoryTest {

    @MockK
    private lateinit var armorer: Armorer

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    private lateinit var mockCipherText: CipherText

    @RelaxedMockK
    private lateinit var crypto: AddressCrypto

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @InjectMockKs
    private lateinit var repository: AttachmentsRepository


    @Test
    fun uploadCallsUploadAttachmentInlineApiWhenAttachmentIsInline() {
        val messageId = "messageId"
        val contentId = "contentId"
        val mimeType = "image/jpeg"
        val fileContent = "attachment content".toByteArray()
        val fileName = "picture.jpg"
        val signedFileContent = "signedFileContent"
        val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
        val headers = AttachmentHeaders(
            mimeType,
            "contentTransferEncoding",
            listOf("inline"),
            listOf(contentId),
            "contentLocation",
            "contentEncryption"
        )
        val attachment = mockk<Attachment> {
            every { this@mockk.headers } returns headers
            every { this@mockk.fileName } returns fileName
            every { this@mockk.messageId } returns messageId
            every { this@mockk.mimeType } returns mimeType
            every { this@mockk.getFileContent() } returns fileContent
        }
        every { crypto.encrypt(fileContent, fileName) } returns mockCipherText
        every { crypto.sign(fileContent) } returns signedFileContent
        every { armorer.unarmor(signedFileContent) } returns unarmoredSignedFileContent

        repository.upload(attachment, crypto)

        val keyPackageSlot = slot<RequestBody>()
        val dataPackageSlot = slot<RequestBody>()
        val signatureSlot = slot<RequestBody>()
        verifySequence {
            apiManager.uploadAttachmentInline(
                attachment,
                messageId,
                contentId,
                capture(keyPackageSlot),
                capture(dataPackageSlot),
                capture(signatureSlot)
            )
        }
        assertEquals(MediaType.parse("image/jpeg"), keyPackageSlot.captured.contentType())
        assertEquals(MediaType.parse("image/jpeg"), dataPackageSlot.captured.contentType())
        assertEquals(MediaType.parse("application/octet-stream"), signatureSlot.captured.contentType())
    }

    @Test
    fun uploadCallsUploadAttachmentInlineApiPassingContentIdFormatted() {
        val messageId = "messageId"
        val contentId = "ignoreFirst<content>Id<split<last< "
        val mimeType = "image/jpeg"
        val fileContent = "attachment content".toByteArray()
        val fileName = "picture.jpg"
        val signedFileContent = "signedFileContent"
        val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
        val headers = AttachmentHeaders(
            mimeType,
            "contentTransferEncoding",
            listOf("inline"),
            listOf(contentId),
            "contentLocation",
            "contentEncryption"
        )
        val attachment = mockk<Attachment> {
            every { this@mockk.headers } returns headers
            every { this@mockk.fileName } returns fileName
            every { this@mockk.messageId } returns messageId
            every { this@mockk.mimeType } returns mimeType
            every { this@mockk.getFileContent() } returns fileContent
        }
        every { crypto.encrypt(fileContent, fileName) } returns mockCipherText
        every { crypto.sign(fileContent) } returns signedFileContent
        every { armorer.unarmor(signedFileContent) } returns unarmoredSignedFileContent

        repository.upload(attachment, crypto)

        val expectedContentId = "contentId"
        verify {
            apiManager.uploadAttachmentInline(
                attachment,
                messageId,
                expectedContentId,
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun uploadCallsUploadAttachmentApiWhenAttachmentIsNotInline() {
        val messageId = "messageId"
        val mimeType = "image/jpeg"
        val fileContent = "attachment content".toByteArray()
        val fileName = "picture.jpg"
        val signedFileContent = "signedFileContent"
        val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
        val attachment = mockk<Attachment> {
            every { this@mockk.headers } returns null
            every { this@mockk.fileName } returns fileName
            every { this@mockk.messageId } returns messageId
            every { this@mockk.mimeType } returns mimeType
            every { this@mockk.getFileContent() } returns fileContent
        }
        every { crypto.encrypt(fileContent, fileName) } returns mockCipherText
        every { crypto.sign(fileContent) } returns signedFileContent
        every { armorer.unarmor(signedFileContent) } returns unarmoredSignedFileContent

        repository.upload(attachment, crypto)

        val keyPackageSlot = slot<RequestBody>()
        val dataPackageSlot = slot<RequestBody>()
        val signatureSlot = slot<RequestBody>()
        verifySequence {
            apiManager.uploadAttachment(
                attachment,
                capture(keyPackageSlot),
                capture(dataPackageSlot),
                capture(signatureSlot)
            )
        }
        assertEquals(MediaType.parse("image/jpeg"), keyPackageSlot.captured.contentType())
        assertEquals(MediaType.parse("image/jpeg"), dataPackageSlot.captured.contentType())
        assertEquals(MediaType.parse("application/octet-stream"), signatureSlot.captured.contentType())
    }

    @Test
    fun uploadSavesUpdatedAttachmentToMessageRepositoryAndReturnSuccessWhenRequestSucceeds() {
        val apiAttachmentId = "456"
        val apiKeyPackets = "apiKeyPackets"
        val apiSignature = "apiSignature"
        val successResponse = mockk<AttachmentUploadResponse> {
            every { code } returns Constants.RESPONSE_CODE_OK
            every { attachmentID } returns apiAttachmentId
            every { attachment.keyPackets } returns apiKeyPackets
            every { attachment.signature } returns apiSignature
        }
        val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
        val attachment = mockk<Attachment>(relaxed = true)
        every { armorer.unarmor(any()) } returns unarmoredSignedFileContent
        every { apiManager.uploadAttachment(any(), any(), any(), any()) } returns successResponse

        val result = repository.upload(attachment, crypto)

        verify {
            attachment.attachmentId = apiAttachmentId
            attachment.keyPackets = apiKeyPackets
            attachment.signature = apiSignature
            attachment.isUploaded = true
            messageDetailsRepository.saveAttachment(attachment)
        }
        assertEquals(AttachmentsRepository.Result.Success, result)
    }

    @Test
    fun uploadReturnsFailureWhenUploadAttachmentToApiFails() {
        val errorMessage = "Attachment Upload Failed"
        val failureResponse = mockk<AttachmentUploadResponse> {
            every { code } returns 400
            every { error } returns errorMessage
        }
        val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
        val attachment = mockk<Attachment>(relaxed = true)
        every { armorer.unarmor(any()) } returns unarmoredSignedFileContent
        every { apiManager.uploadAttachment(any(), any(), any(), any()) } returns failureResponse

        val result = repository.upload(attachment, crypto)

        verify(exactly = 0) { messageDetailsRepository.saveAttachment(any()) }
        val expectedResult = AttachmentsRepository.Result.Failure(errorMessage)
        assertEquals(expectedResult, result)
    }
}
