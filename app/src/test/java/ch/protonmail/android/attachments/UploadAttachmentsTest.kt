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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UploadAttachmentsTest : CoroutinesTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var attachmentsRepository: AttachmentsRepository

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    private lateinit var crypto: AddressCrypto

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @InjectMockKs
    private lateinit var uploadAttachments: UploadAttachments

    @BeforeEach
    fun setUp() {
        coEvery { attachmentsRepository.upload(any(), crypto) } returns AttachmentsRepository.Result.Success
    }

    @Test
    fun uploadAttachmentsCallsRepositoryWhenInputAttachmentsAreValid() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message(messageId = "messageId")
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            val result = uploadAttachments(attachmentIds, message, crypto)

            coVerifyOrder {
                attachment1.setMessage(message)
                attachmentsRepository.upload(attachment1, crypto)
                attachment2.setMessage(message)
                attachmentsRepository.upload(attachment2, crypto)
            }
            assertEquals(UploadAttachments.Result.Success, result)
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfAnyAttachmentFailsToBeUploaded() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2
            coEvery { attachmentsRepository.upload(attachment2, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload attachment2")
            }

            val result = uploadAttachments(attachmentIds, message, crypto)

            val expected = UploadAttachments.Result.Failure("Failed to upload attachment2")
            assertEquals(expected, result)
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfPublicKeyFailsToBeUploaded() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val message = Message()
            val username = "username"
            every { userManager.username } returns username
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload public key")
            }

            val result = uploadAttachments(attachmentIds, message, crypto)

            val expected = UploadAttachments.Result.Failure("Failed to upload public key")
            assertEquals(expected, result)
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentIsNotFoundInMessageRepository() {
        runBlockingTest {
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns null
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments(attachmentIds, message, crypto)

            coVerifySequence { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentFilePathIsNull() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns null
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments(attachmentIds, message, crypto)

            coVerify(exactly = 0) { attachmentsRepository.upload(attachment1, crypto) }
            coVerify { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentWasAlreadyUploaded() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns true
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments(attachmentIds, message, crypto)

            coVerify { attachmentsRepository.upload(attachment1, crypto) }
            coVerify(exactly = 0) { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentFileDoesNotExist() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val message = Message()
            val attachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentMock2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns false
            }
            every { messageDetailsRepository.findAttachmentById("1") } returns attachmentMock1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachmentMock2

            uploadAttachments(attachmentIds, message, crypto)

            coVerify { attachmentsRepository.upload(attachmentMock1, crypto) }
            coVerify(exactly = 0) { attachmentsRepository.upload(attachmentMock2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsCallRepositoryUploadPublicKeyWhenMailSettingsGetAttachPublicKeyIsTrue() {
        runBlockingTest {
            val username = "username"
            val message = Message()
            every { userManager.username } returns username
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Success
            }

            uploadAttachments(emptyList(), message, crypto)

            coVerify { attachmentsRepository.uploadPublicKey(message, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsDeletesLocalFileAfterSuccessfulUpload() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val message = Message()
            val attachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentMock2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            every { messageDetailsRepository.findAttachmentById("1") } returns attachmentMock1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachmentMock2
            coEvery { attachmentsRepository.upload(attachmentMock1, crypto) } answers {
                AttachmentsRepository.Result.Success
            }
            coEvery { attachmentsRepository.upload(attachmentMock2, crypto) } answers {
                AttachmentsRepository.Result.Failure("failed")
            }

            uploadAttachments(attachmentIds, message, crypto)

            verify { attachmentMock1.deleteLocalFile() }
            verify(exactly = 0) { attachmentMock2.deleteLocalFile() }
        }
    }

}


