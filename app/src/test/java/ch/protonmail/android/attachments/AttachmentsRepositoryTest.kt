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
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.utils.crypto.BinaryDecryptionResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import okhttp3.MediaType
import okhttp3.RequestBody
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AttachmentsRepositoryTest : CoroutinesTest {

    @MockK
    private lateinit var userManager: UserManager

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

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        val successResponse = mockk<AttachmentUploadResponse> {
            every { code } returns Constants.RESPONSE_CODE_OK
            every { attachmentID } returns "default success attachment ID"
            every { attachment.keyPackets } returns null
            every { attachment.signature } returns null
            every { attachment.headers } returns null
            every { attachment.fileSize } returns 823742L
        }
        coEvery { apiManager.uploadAttachmentInline(any(), any(), any(), any(), any(), any()) } returns successResponse
        coEvery { apiManager.uploadAttachment(any(), any(), any(), any()) } returns successResponse
    }

    @Test
    fun uploadCallsUploadAttachmentInlineApiWhenAttachmentIsInline() {
        runBlockingTest {
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
                contentId,
                "contentLocation",
                "contentEncryption"
            )
            val attachment = mockk<Attachment>(relaxed = true) {
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
            coVerifySequence {
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
    }

    @Test
    fun uploadCallsUploadAttachmentInlineApiPassingContentIdFormatted() {
        runBlockingTest {
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
                contentId,
                "contentLocation",
                "contentEncryption"
            )
            val attachment = mockk<Attachment>(relaxed = true) {
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
            coVerify {
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
    }

    @Test
    fun uploadCallsUploadAttachmentApiWhenAttachmentIsNotInline() {
        runBlockingTest {
            val messageId = "messageId"
            val mimeType = "image/jpeg"
            val fileContent = "attachment content".toByteArray()
            val fileName = "picture.jpg"
            val signedFileContent = "signedFileContent"
            val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
            val attachment = mockk<Attachment>(relaxed = true) {
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
            coVerifySequence {
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
    }

    @Test
    fun uploadSavesUpdatedAttachmentToMessageRepositoryAndReturnSuccessWhenRequestSucceeds() {
        runBlockingTest {
            val apiAttachmentId = "456"
            val apiKeyPackets = "apiKeyPackets"
            val apiSignature = "apiSignature"
            val headers = AttachmentHeaders()
            val fileSize = 1234L
            val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
            val attachment = mockk<Attachment>(relaxed = true)
            val successResponse = mockk<AttachmentUploadResponse>(relaxed = true) {
                every { code } returns Constants.RESPONSE_CODE_OK
                every { attachmentID } returns apiAttachmentId
                every { this@mockk.attachment.keyPackets } returns apiKeyPackets
                every { this@mockk.attachment.signature } returns apiSignature
                every { this@mockk.attachment.headers } returns headers
                every { this@mockk.attachment.fileSize } returns fileSize
            }
            coEvery { apiManager.uploadAttachment(any(), any(), any(), any()) } returns successResponse
            every { armorer.unarmor(any()) } returns unarmoredSignedFileContent

            val result = repository.upload(attachment, crypto)

            coVerify {
                attachment.attachmentId = apiAttachmentId
                attachment.keyPackets = apiKeyPackets
                attachment.signature = apiSignature
                attachment.isUploaded = true
                attachment.headers = headers
                attachment.fileSize = fileSize
                messageDetailsRepository.saveAttachment(attachment)
            }
            val expected = AttachmentsRepository.Result.Success(apiAttachmentId)
            assertEquals(expected, result)
        }
    }

    @Test
    fun uploadReturnsFailureWhenUploadAttachmentToApiFails() {
        runBlockingTest {
            val errorMessage = "Attachment Upload Failed"
            val failureResponse = mockk<AttachmentUploadResponse> {
                every { code } returns 400
                every { error } returns errorMessage
            }
            val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
            val attachment = mockk<Attachment>(relaxed = true)
            every { armorer.unarmor(any()) } returns unarmoredSignedFileContent
            coEvery { apiManager.uploadAttachment(any(), any(), any(), any()) } returns failureResponse

            val result = repository.upload(attachment, crypto)

            coVerify(exactly = 0) { messageDetailsRepository.saveAttachment(any()) }
            val expectedResult = AttachmentsRepository.Result.Failure(errorMessage)
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun uploadPublicKeyCallsUploadAttachmentApiWithPublicKeyAttachment() {
        runBlockingTest {
            val username = "username"
            val message = Message(messageId = "messageId")
            val privateKey = mockk<PgpField.PrivateKey>()
            val unarmoredSignedFileContent = "unarmoredSignedFileContent".toByteArray()
            val address = mockk<Address> {
                every { keys.primaryKey?.privateKey } returns privateKey
                every { email } returns EmailAddress("message@email.com")
            }
            every { userManager.username } returns username
            every { userManager.getUser(username).getAddressById(message.addressID).toNewAddress() } returns address
            every { crypto.buildArmoredPublicKey(any()) } returns "PublicKeyString"
            every { crypto.getFingerprint("PublicKeyString") } returns "PublicKeyStringFingerprint"
            every { armorer.unarmor(any()) } returns unarmoredSignedFileContent

            val result = repository.uploadPublicKey(message, crypto)

            val keyPackageSlot = slot<RequestBody>()
            val dataPackageSlot = slot<RequestBody>()
            val signatureSlot = slot<RequestBody>()
            val expectedAttachment = Attachment(
                fileName = "publickey - EmailAddress(s=message@email.com) - 0xPUBLICKE.asc",
                mimeType = "application/pgp-keys",
                messageId = message.messageId!!,
                attachmentId = "default success attachment ID",
                isUploaded = true,
                headers = null,
                fileSize = 823742L
            )
            coVerifySequence {
                apiManager.uploadAttachment(
                    expectedAttachment,
                    capture(keyPackageSlot),
                    capture(dataPackageSlot),
                    capture(signatureSlot)
                )
            }
            assertEquals(MediaType.parse("application/pgp-keys"), keyPackageSlot.captured.contentType())
            assertEquals(MediaType.parse("application/pgp-keys"), dataPackageSlot.captured.contentType())
            assertEquals(MediaType.parse("application/octet-stream"), signatureSlot.captured.contentType())
            val expected = AttachmentsRepository.Result.Success("default success attachment ID")
            assertEquals(expected, result)
        }
    }

    @Test
    fun uploadReturnsFailureWhenApiCallFailsBecauseOfTimeout() {
        runBlockingTest {
            val errorMessage = "Upload attachment request failed"
            val unarmoredSignedFileContent = byteArrayOf()
            val attachment = mockk<Attachment>(relaxed = true)
            every { armorer.unarmor(any()) } returns unarmoredSignedFileContent
            coEvery { apiManager.uploadAttachment(any(), any(), any(), any()) } throws SocketTimeoutException("Call timed out")

            val result = repository.upload(attachment, crypto)

            coVerify(exactly = 0) { messageDetailsRepository.saveAttachment(any()) }
            val expectedResult = AttachmentsRepository.Result.Failure(errorMessage)
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun uploadLogsAndReThrowsCancellationExceptions() {
        runBlockingTest {
            val errorMessage = "Upload attachments work was cancelled"
            val unarmoredSignedFileContent = byteArrayOf()
            val attachment = mockk<Attachment>(relaxed = true)
            every { armorer.unarmor(any()) } returns unarmoredSignedFileContent
            coEvery {
                apiManager.uploadAttachment(any(), any(), any(), any())
            } throws CancellationException("Call was cancelled")

            try {
                repository.upload(attachment, crypto)
            } catch (exception: CancellationException) {
                assertEquals("Call was cancelled", exception.message)
            }

            coVerify(exactly = 0) { messageDetailsRepository.saveAttachment(any()) }
        }
    }

    @Test
    fun uploadReturnsFailureWhenAnyRequiredAttachmentFieldIsNull() {
        runBlockingTest {
            val fileContent = "file content".toByteArray()
            val attachment = mockk<Attachment>(relaxed = true) {
                every { getFileContent() } returns fileContent
                every { mimeType } returns null
            }

            val result = repository.upload(attachment, crypto)

            val expectedResult = AttachmentsRepository.Result.Failure("This attachment name / type is invalid. Please retry")
            assertEquals(expectedResult, result)
        }
    }


    @Test
    fun verifyThatAttachmentsBytesCanBeDownloadedSuccessfully() = runBlockingTest {
        // given
        val attachmentId = "Ida1"
        val key = "zeKey1"
        val content = "content1234"
        val decryptedContent = "decryptedContent1234".encodeToByteArray()
        val decryptedResult = mockk<BinaryDecryptionResult> {
            every { decryptedData } returns decryptedContent
        }
        every { crypto.decryptAttachment(any(), any()) } returns decryptedResult
        val testResponseBody = okhttp3.ResponseBody.create(MediaType.parse("image/jpg"), content)
        coEvery { apiManager.downloadAttachment(attachmentId) } returns testResponseBody

        // when
        val result = repository.getAttachmentDataOrNull(crypto, attachmentId, key)

        // then
        assertEquals(decryptedContent, result)
    }
}
