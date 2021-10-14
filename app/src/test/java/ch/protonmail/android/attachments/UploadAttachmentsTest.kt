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

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
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
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertArrayEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class UploadAttachmentsTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @RelaxedMockK
    private lateinit var pendingActionsDao: PendingActionsDao

    @RelaxedMockK
    private lateinit var attachmentsRepository: AttachmentsRepository

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    private lateinit var cryptoFactory: AddressCrypto.Factory

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var crypto: AddressCrypto

    @InjectMockKs
    private lateinit var uploadAttachments: UploadAttachments

    private val currentUsername = "current username"

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { attachmentsRepository.upload(any(), crypto) } returns AttachmentsRepository.Result.Success("8237423")
        coEvery { messageDetailsRepository.saveMessageLocally(any()) } returns 823L
        every { pendingActionsDao.findPendingUploadByMessageId(any()) } returns null
        every { userManager.username } returns currentUsername
    }

    @Test
    fun workerEnqueuerCreatesOneTimeRequestWorkerWhichIsUniqueForMessageId() {
        runBlockingTest {
            // Given
            val attachmentIds = listOf("attachmentId234", "238")
            val messageId = "2834"
            val messageDbId = 534L
            val addressId = "senderAddressId"
            val message = Message(messageId = messageId).apply {
                this.dbId = messageDbId
                this.addressID = addressId
                this.decryptedBody = decryptedBody
            }
            every { cryptoFactory.create(Id(addressId), Name(currentUsername)) } returns crypto

            // When
            UploadAttachments.Enqueuer(workManager).enqueue(
                attachmentIds,
                messageId,
                true
            )

            // Then
            val requestSlot = slot<OneTimeWorkRequest>()
            verify {
                workManager.enqueueUniqueWork(
                    "uploadAttachmentUniqueWorkName-$messageId",
                    ExistingWorkPolicy.REPLACE,
                    capture(requestSlot)
                )
            }
            val workSpec = requestSlot.captured.workSpec
            val constraints = workSpec.constraints
            val inputData = workSpec.input
            val actualMessageLocalId = inputData.getString(KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID)
            val actualIsMessageSending = inputData.getBoolean(KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING, false)
            val actualAttachmentIds = inputData.getStringArray(KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS)
            assertEquals(message.messageId, actualMessageLocalId)
            assertEquals(true, actualIsMessageSending)
            assertArrayEquals(attachmentIds.toTypedArray(), actualAttachmentIds)
            assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
            assertEquals(BackoffPolicy.LINEAR, workSpec.backoffPolicy)
            assertEquals(10000, workSpec.backoffDelayDuration)
            verify { workManager.getWorkInfoByIdLiveData(any()) }
        }
    }

    @Test
    fun uploadAttachmentsSetsMessageAsPendingToUploadWhenStartingToUpload() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val messageId = "message-id-238237"
            val message = Message(messageId = messageId, addressID = "senderAddress")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message

            uploadAttachments.doWork()

            val pendingUpload = PendingUpload(messageId)
            verify { pendingActionsDao.insertPendingForUpload(pendingUpload) }
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfMessageIsNotFoundInLocalDB() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val messageId = "message-id-238723"
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns null

            val result = uploadAttachments.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to "Message not found")
                ),
                result
            )
            verify(exactly = 0) { pendingActionsDao.findPendingSendByMessageId(any()) }
        }
    }

    @Test
    fun uploadAttachmentsExecutesNormallyWhenUploadWasLeftOngoingForTheGivenMessage() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val messageId = "message-id-123842"
            val message = Message(messageId = messageId, addressID = "senderAddress1")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { pendingActionsDao.findPendingUploadByMessageId(messageId) } returns PendingUpload(messageId)

            val result = uploadAttachments.doWork()

            verify { pendingActionsDao.insertPendingForUpload(any()) }
            assertEquals(ListenableWorker.Result.success(), result)
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
            val messageId = "messageId8234"
            val message = Message(messageId = messageId, addressID = "senderAddress1")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress1"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            val result = uploadAttachments.doWork()

            coVerifyOrder {
                attachment1.setMessage(message)
                attachmentsRepository.upload(attachment1, crypto)
                attachment2.setMessage(message)
                attachmentsRepository.upload(attachment2, crypto)
                pendingActionsDao.deletePendingUploadByMessageId("messageId8234")
            }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun uploadAttachmentsRetriesIfAnyAttachmentFailsToBeUploadedAndMaxRetriesWasNotReached() {
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
            val messageId = "messageId8238"
            val message = Message(messageId = messageId, addressID = "senderAddress2")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress2"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2
            coEvery { attachmentsRepository.upload(attachment2, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload attachment2")
            }
            every { parameters.runAttemptCount } returns 0

            val result = uploadAttachments.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            verify { pendingActionsDao.deletePendingUploadByMessageId("messageId8238") }
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfAnyAttachmentFailsToBeUploadedAndMaxRetriesWereReached() {
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
            val messageId = "messageId8237"
            val message = Message(messageId = messageId, addressID = "senderAddress2")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress2"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2
            coEvery { attachmentsRepository.upload(attachment2, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload attachment2")
            }
            every { parameters.runAttemptCount } returns 4

            val result = uploadAttachments.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to "Failed to upload attachment2")
                ),
                result
            )
            verify { pendingActionsDao.deletePendingUploadByMessageId("messageId8237") }
        }
    }

    @Test
    fun uploadAttachmentsRetriesIfPublicKeyFailsToBeUploadedAndMaxRetriesWereNotReached() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val messageId = "messageId9273584"
            val username = "username"
            val message = Message(messageId = messageId, addressID = "senderAddress13")
            givenFullValidInput(messageId, attachmentIds.toTypedArray(), true)
            every { cryptoFactory.create(Id("senderAddress13"), Name(username)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { userManager.username } returns username
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload public key")
            }
            every { parameters.runAttemptCount } returns 0

            val result = uploadAttachments.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            verify { pendingActionsDao.deletePendingUploadByMessageId("messageId9273584") }
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfPublicKeyFailsToBeUploadedAndMaxRetriesWereReached() {
        runBlockingTest {
            val attachmentIds = listOf("1")
            val messageId = "messageId9273585"
            val username = "username"
            val message = Message(messageId = messageId, addressID = "senderAddress12")
            givenFullValidInput(messageId, attachmentIds.toTypedArray(), true)
            every { cryptoFactory.create(Id("senderAddress12"), Name(username)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { userManager.username } returns username
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload public key")
            }
            every { parameters.runAttemptCount } returns 4

            val result = uploadAttachments.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to "Failed to upload public key")
                ),
                result
            )
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
            val messageId = "messageId9237"
            val message = Message(messageId = messageId, addressID = "senderAddress3")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress3"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { messageDetailsRepository.findAttachmentById("1") } returns null
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments.doWork()

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
            val messageId = "messageId36926543"
            val message = Message(messageId = messageId, addressID = "senderAddress4")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress4"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments.doWork()

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
            val messageId = "messageId0123876"
            val message = Message(messageId = messageId, addressID = "senderAddress5")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress5"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments.doWork()

            coVerify { attachmentsRepository.upload(attachment1, crypto) }
            coVerify(exactly = 0) { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentFileDoesNotExist() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val messageId = "messageId83483"
            val message = Message(messageId = messageId, addressID = "senderAddress6")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress6"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
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

            uploadAttachments.doWork()

            coVerify { attachmentsRepository.upload(attachmentMock1, crypto) }
            coVerify(exactly = 0) { attachmentsRepository.upload(attachmentMock2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsCallRepositoryUploadPublicKeyWhenMailSettingsGetAttachPublicKeyIsTrueAndMessageIsSending() {
        runBlockingTest {
            val username = "username"
            val messageId = "messageId823762"
            val message = Message(messageId = messageId, addressID = "senderAddress6")
            every { userManager.username } returns username
            givenFullValidInput(messageId, isMessageSending = true)
            every { cryptoFactory.create(Id("senderAddress6"), Name(username)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Success("23421")
            }

            uploadAttachments.doWork()

            coVerify { attachmentsRepository.uploadPublicKey(message, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsDeletesLocalFileAfterSuccessfulUpload() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val messageId = "messageId126943"
            val message = Message(messageId = messageId, addressID = "senderAddress7")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress7"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
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

            uploadAttachments.doWork()

            verify { attachmentMock1.deleteLocalFile() }
            verify(exactly = 0) { attachmentMock2.deleteLocalFile() }
        }
    }

    @Test
    fun uploadAttachmentsUpdatesLocalMessageWithUploadedAttachmentWhenUploadSucceeds() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val messageId = "82342"
            val message = Message(messageId = messageId, addressID = "senderAddress8")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress8"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
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

            uploadAttachments.doWork()

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
            val message = Message(messageId = messageId, addressID = "senderAddress9")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress9"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
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

            uploadAttachments.doWork()

            verify(exactly = 1) { messageDetailsRepository.findAttachmentById("1") }
            coVerify(exactly = 0) { messageDetailsRepository.saveMessageLocally(any()) }
        }
    }

    @Test
    fun uploadAttachmentsDoesNotAddUploadedAttachmentToLocalMessageIfAttachmentWasNotInTheMessageAttachments() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val messageId = "82342"
            val message = Message(messageId = messageId, addressID = "senderAddress10")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(Id("senderAddress10"), Name(currentUsername)) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
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

            uploadAttachments.doWork()

            val actualMessage = slot<Message>()
            val expectedAttachments = listOf(attachmentMock1)
            coVerify(exactly = 2) { messageDetailsRepository.saveMessageLocally(capture(actualMessage)) }
            assertEquals(expectedAttachments, actualMessage.captured.Attachments)
        }
    }

    @Test
    fun uploadAttachmentsDoesNotAttachThePublicKeyIfTheMessageIsNotBeingSent() {
        runBlockingTest {
            val messageId = "message-id-123842"
            val message = Message(messageId, addressID = "addressId")
            val username = "username"
            givenFullValidInput(messageId, emptyArray(), isMessageSending = false)
            every { userManager.username } returns username
            every { pendingActionsDao.findPendingUploadByMessageId(messageId) } returns null
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } returns mockk()

            val result = uploadAttachments.doWork()

            coVerify(exactly = 0) { attachmentsRepository.uploadPublicKey(any(), any()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    @Test
    fun uploadAttachmentsAttachesThePublicKeyOnlyWhenTheMessageIsBeingSent() {
        runBlockingTest {
            val messageId = "message-id-123842"
            val message = Message(messageId, addressID = "addressId")
            val username = "username"
            givenFullValidInput(messageId, emptyArray(), isMessageSending = true)
            every { userManager.username } returns username
            every { pendingActionsDao.findPendingUploadByMessageId(messageId) } returns null
            every { userManager.getMailSettings(username)?.getAttachPublicKey() } returns true
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns message
            every { cryptoFactory.create(Id("addressId"), Name(username)) } returns crypto
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Success("82384")
            }

            val result = uploadAttachments.doWork()

            coVerify { attachmentsRepository.uploadPublicKey(any(), any()) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

    private fun givenFullValidInput(
        messageId: String,
        attachments: Array<String> = arrayOf("attId62364"),
        isMessageSending: Boolean = false
    ) {
        every { parameters.inputData.getStringArray(KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS) } answers { attachments }
        every { parameters.inputData.getString(KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID) } answers { messageId }
        every {
            parameters.inputData.getBoolean(KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING, false)
        } answers { isMessageSending }
    }
}


