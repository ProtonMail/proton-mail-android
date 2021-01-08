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
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

class UploadAttachmentsTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var pendingActionsDao: PendingActionsDao

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

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { attachmentsRepository.upload(any(), crypto) } returns AttachmentsRepository.Result.Success
        coEvery { attachmentsRepository.upload(any(), crypto) } returns AttachmentsRepository.Result.Success("8237423")
        coEvery { messageDetailsRepository.saveMessageLocally(any()) } returns 823L
        every { pendingActionsDao.findPendingUploadByMessageId(any()) } returns null
    }

    @Test
    fun uploadAttachmentsSetsMessageAsPendingToUploadWhenStartingToUpload() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val messageId = "message-id-238237"
            val message = Message(messageId)

            uploadAttachments(attachmentIds, message, crypto)

            val pendingUpload = PendingUpload(messageId)
            verify { pendingActionsDao.insertPendingForUpload(pendingUpload) }
        }
    }

    @Test
    fun uploadAttachmentsIsNotExecutedAgainWhenUploadAlreadyOngoingForTheGivenMessage() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val messageId = "message-id-123842"
            val message = Message(messageId)
            every { pendingActionsDao.findPendingUploadByMessageId(messageId) } returns PendingUpload(messageId)

            val result = uploadAttachments(attachmentIds, message, crypto)

            verify(exactly = 0) { pendingActionsDao.insertPendingForUpload(any()) }
            assertEquals(UploadAttachments.Result.UploadInProgress, result)
        }
    }

    @Test
    fun uploadAttachmentsCallsRepositoryWhenInputAttachmentsAreValid() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message(messageId = "messageId8234")
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            val result = uploadAttachments(attachmentIds, message, crypto)

            coVerifyOrder {
                pendingActionsDao.findPendingUploadByMessageId("messageId8234")
                attachment1.setMessage(message)
                attachmentsRepository.upload(attachment1, crypto)
                attachment2.setMessage(message)
                attachmentsRepository.upload(attachment2, crypto)
                pendingActionsDao.deletePendingUploadByMessageId("messageId8234")
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
                every { doesFileExist } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message("messageId8237")
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2
            coEvery { attachmentsRepository.upload(attachment2, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload attachment2")
            }

            val result = uploadAttachments(attachmentIds, message, crypto)

            val expected = UploadAttachments.Result.Failure("Failed to upload attachment2")
            assertEquals(expected, result)
            verify { pendingActionsDao.deletePendingUploadByMessageId("messageId8237") }
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfPublicKeyFailsToBeUploaded() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val message = Message("messageId9273585")
            val username = "username"
            every { userManager.username } returns username
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload public key")
            }

            val result = uploadAttachments(attachmentIds, message, crypto)

            val expected = UploadAttachments.Result.Failure("Failed to upload public key")
            assertEquals(expected, result)
            verify { pendingActionsDao.deletePendingUploadByMessageId("messageId9273585") }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentIsNotFoundInMessageRepository() {
        runBlockingTest {
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message("messageId9237")
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
                every { doesFileExist } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message("messageId36926543")
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
                every { doesFileExist } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns true
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message("messageId0123876")
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
            val message = Message("messageId83483")
            val attachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentMock2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns false
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
            val message = Message("messageId823762")
            every { userManager.username } returns username
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Success("23421")
            }

            uploadAttachments(emptyList(), message, crypto)

            coVerify { attachmentsRepository.uploadPublicKey(message, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsDeletesLocalFileAfterSuccessfulUpload() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val message = Message("messageId126943")
            val attachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentMock2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            every { messageDetailsRepository.findAttachmentById("1") } returns attachmentMock1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachmentMock2
            coEvery { attachmentsRepository.upload(attachmentMock1, crypto) } answers {
                AttachmentsRepository.Result.Success("234423")
            }
            coEvery { attachmentsRepository.upload(attachmentMock2, crypto) } answers {
                AttachmentsRepository.Result.Failure("failed")
            }

            uploadAttachments(attachmentIds, message, crypto)

            verify { attachmentMock1.deleteLocalFile() }
            verify(exactly = 0) { attachmentMock2.deleteLocalFile() }
        }
    }

    @Test
    fun uploadAttachmentsUpdatesLocalMessageWithUploadedAttachmentWhenUploadSucceeds() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val messageId = "82342"
            val message = Message(messageId = messageId)
            val uploadedAttachmentMock1Id = "823472"
            val uploadedAttachmentMock2Id = "234092"
            val attachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { fileName } returns "att1FileName"
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val uploadedAttachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { fileName } returns "att1FileName"
                every { attachmentId } returns uploadedAttachmentMock1Id
                every { filePath } returns "filePath1"
                every { isUploaded } returns true
                every { doesFileExist } returns true
            }
            val attachmentMock2 = mockk<Attachment>(relaxed = true) {
                every { fileName } returns "att2FileName"
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val uploadedAttachmentMock2 = mockk<Attachment>(relaxed = true) {
                every { fileName } returns "att2FileName"
                every { attachmentId } returns uploadedAttachmentMock2Id
                every { filePath } returns "filePath2"
                every { keyPackets } returns "uploadedPackets"
                every { isUploaded } returns true
                every { doesFileExist } returns true
            }
            message.setAttachmentList(listOf(attachmentMock1, attachmentMock2))
            every { messageDetailsRepository.findAttachmentById("1") } returns attachmentMock1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachmentMock2
            every { messageDetailsRepository.findAttachmentById(uploadedAttachmentMock1Id) } returns uploadedAttachmentMock1
            every { messageDetailsRepository.findAttachmentById(uploadedAttachmentMock2Id) } returns uploadedAttachmentMock2
            coEvery { attachmentsRepository.upload(attachmentMock1, crypto) } answers {
                AttachmentsRepository.Result.Success(uploadedAttachmentMock1Id)
            }
            coEvery { attachmentsRepository.upload(attachmentMock2, crypto) } answers {
                AttachmentsRepository.Result.Success(uploadedAttachmentMock2Id)
            }

            uploadAttachments(attachmentIds, message, crypto)

            val actualMessage = slot<Message>()
            val expectedAttachments = listOf(uploadedAttachmentMock1, uploadedAttachmentMock2)
            verify { messageDetailsRepository.findAttachmentById(uploadedAttachmentMock2Id) }
            coVerify { messageDetailsRepository.saveMessageLocally(capture(actualMessage)) }
            assertEquals(expectedAttachments, actualMessage.captured.Attachments)
        }
    }

    @Test
    fun uploadAttachmentsDoesNotUpdateLocalMessageWithUploadedAttachmentWhenUploadFails() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val messageId = "82342"
            val message = Message(messageId = messageId)
            val attachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { fileName } returns "att1FileName"
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            message.setAttachmentList(listOf(attachmentMock1))
            every { messageDetailsRepository.findAttachmentById("1") } returns attachmentMock1
            coEvery { attachmentsRepository.upload(attachmentMock1, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed uploading attachment!")
            }

            uploadAttachments(attachmentIds, message, crypto)

            verify(exactly = 1) { messageDetailsRepository.findAttachmentById("1") }
            coVerify(exactly = 0) { messageDetailsRepository.saveMessageLocally(any()) }
        }
    }

    @Test
    fun uploadAttachmentsDoesNotAddUploadedAttachmentToLocalMessageIfAttachmentWasNotInTheMessageAttachments() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val messageId = "82342"
            val message = Message(messageId = messageId)
            val attachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { fileName } returns "att1FileName"
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentMock2 = mockk<Attachment>(relaxed = true) {
                every { fileName } returns "att2FileName"
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val uploadedAttachment1Id = "82372"
            val uploadedAttachment2Id = "24832"
            message.setAttachmentList(listOf(attachmentMock1))
            every { messageDetailsRepository.findAttachmentById("1") } returns attachmentMock1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachmentMock2
            // Reusing the same mock for the success just to avoid creating new ones
            // In production, this would be a different attachment with another ID
            every { messageDetailsRepository.findAttachmentById(uploadedAttachment1Id) } returns attachmentMock1
            every { messageDetailsRepository.findAttachmentById(uploadedAttachment2Id) } returns attachmentMock2
            coEvery { attachmentsRepository.upload(attachmentMock1, crypto) } answers {
                AttachmentsRepository.Result.Success(uploadedAttachment1Id)
            }
            coEvery { attachmentsRepository.upload(attachmentMock2, crypto) } answers {
                AttachmentsRepository.Result.Success(uploadedAttachment2Id)
            }

            uploadAttachments(attachmentIds, message, crypto)

            val actualMessage = slot<Message>()
            val expectedAttachments = listOf(attachmentMock1)
            coVerify(exactly = 2) { messageDetailsRepository.saveMessageLocally(capture(actualMessage)) }
            assertEquals(expectedAttachments, actualMessage.captured.Attachments)
        }
    }
}


