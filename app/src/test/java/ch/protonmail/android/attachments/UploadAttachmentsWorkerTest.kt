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
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.pendingaction.data.model.PendingUpload
import ch.protonmail.android.settings.domain.usecase.GetMailSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.user.domain.entity.AddressId
import org.junit.Assert.assertArrayEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [UploadAttachments]
 */
class UploadAttachmentsWorkerTest :
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val context: Context = mockk(relaxed = true)

    private val parameters: WorkerParameters = mockk(relaxed = true)

    private val workManager: WorkManager = mockk(relaxed = true)

    private val pendingActionDao: PendingActionDao = mockk(relaxed = true) {
        every { findPendingUploadByMessageIdBlocking(any()) } returns null
    }

    private val databaseProvider: DatabaseProvider = mockk {
        every { providePendingActionDao(any()) } returns pendingActionDao
    }

    private val attachmentsRepository: AttachmentsRepository = mockk(relaxed = true) {
        coEvery { upload(any(), any()) } returns AttachmentsRepository.Result.Success("8237423")
    }

    private val messageDetailsRepository: MessageDetailsRepository = mockk(relaxed = true) {
        coEvery { saveMessage(any()) } returns 823L
    }

    private val cryptoFactory: AddressCrypto.Factory = mockk(relaxed = true)

    private val testUserId = UserId("id")
    private val userManager: UserManager = mockk {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
        coEvery { getMailSettings(any()) } returns mockk(relaxed = true)
    }

    private val mailSettings: MailSettings = mockk(relaxed = true)
    private val getMailSettings: GetMailSettings = mockk {
        every { this@mockk(any()) } returns flowOf(GetMailSettings.Result.Success(mailSettings))
    }

    private val crypto: AddressCrypto = mockk(relaxed = true)

    private lateinit var uploadAttachmentsWorker: UploadAttachmentsWorker

    @BeforeTest
    fun setUp() {
        uploadAttachmentsWorker = UploadAttachmentsWorker(
            context = context,
            params = parameters,
            dispatchers = dispatchers,
            attachmentsRepository = attachmentsRepository,
            databaseProvider = databaseProvider,
            messageDetailsRepository = messageDetailsRepository,
            addressCryptoFactory = cryptoFactory,
            userManager = userManager,
            getMailSettings = getMailSettings,
        )
    }

    @Test
    fun workerEnqueuerCreatesOneTimeRequestWorkerWhichIsUniqueForMessageId() {
        runTest(dispatchers.Main) {
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
            every { cryptoFactory.create(testUserId, AddressId(addressId)) } returns crypto

            // When
            UploadAttachmentsWorker.Enqueuer(workManager).enqueue(
                attachmentIds,
                messageId,
                true
            )

            // Then
            val requestSlot = slot<OneTimeWorkRequest>()
            verify {
                workManager.enqueueUniqueWork(
                    "uploadAttachmentUniqueWorkName-$messageId",
                    ExistingWorkPolicy.APPEND_OR_REPLACE,
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
        runTest(dispatchers.Main) {
            val attachmentIds = listOf("1")
            val messageId = "message-id-238237"
            val message = Message(messageId = messageId, addressID = "senderAddress")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)

            uploadAttachmentsWorker.doWork()

            val pendingUpload = PendingUpload(messageId)
            verify { pendingActionDao.insertPendingForUpload(pendingUpload) }
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfMessageIsNotFoundInLocalDB() {
        runTest(dispatchers.Main) {
            val attachmentIds = listOf("1")
            val messageId = "message-id-238723"
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(null)

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(
                        KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to "Message not found",
                    )
                ),
                result
            )
            verify(exactly = 0) { pendingActionDao.findPendingSendByMessageIdBlocking(any()) }
        }
    }

    @Test
    fun uploadAttachmentsExecutesNormallyWhenUploadWasLeftOngoingForTheGivenMessage() {
        runTest(dispatchers.Main) {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1")
            val messageId = "message-id-123842"
            val message = Message(messageId = messageId, addressID = "senderAddress1")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(testUserId, AddressId("senderAddress1")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { pendingActionDao.findPendingUploadByMessageIdBlocking(messageId) } returns PendingUpload(messageId)
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            coEvery { attachmentsRepository.upload(attachment1, crypto) } answers {
                AttachmentsRepository.Result.Success("1")
            }

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            verify { pendingActionDao.insertPendingForUpload(any()) }
        }
    }

    @Test
    fun uploadAttachmentsCallsRepositoryWhenInputAttachmentsAreValid() {
        runTest(dispatchers.Main) {
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
            every { cryptoFactory.create(testUserId, AddressId("senderAddress1")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerifyOrder {
                attachment1.setMessage(message)
                attachmentsRepository.upload(attachment1, crypto)
                attachment2.setMessage(message)
                attachmentsRepository.upload(attachment2, crypto)
                pendingActionDao.deletePendingUploadByMessageId("messageId8234")
            }
        }
    }

    @Test
    fun uploadAttachmentsRetriesIfAnyAttachmentFailsToBeUploadedAndMaxRetriesWasNotReached() {
        runTest(dispatchers.Main) {
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
            every { cryptoFactory.create(testUserId, AddressId("senderAddress2")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2
            coEvery { attachmentsRepository.upload(attachment2, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload attachment2")
            }
            every { parameters.runAttemptCount } returns 0

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            verify { pendingActionDao.deletePendingUploadByMessageId("messageId8238") }
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfAnyAttachmentFailsToBeUploadedAndMaxRetriesWereReached() {
        runTest(dispatchers.Main) {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { fileName } returns "Attachment_1.jpg"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { fileName } returns "Attachment_2.jpg"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1", "2")
            val messageId = "messageId8237"
            val message = Message(messageId = messageId, addressID = "senderAddress2")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(testUserId, AddressId("senderAddress2")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2
            coEvery { attachmentsRepository.upload(attachment2, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload attachment2")
            }
            every { parameters.runAttemptCount } returns 4

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(
                        KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to "Failed to upload attachment2",
                    )
                ),
                result
            )
            verify { pendingActionDao.deletePendingUploadByMessageId("messageId8237") }
        }
    }

    @Test
    fun uploadAttachmentsRetriesIfPublicKeyFailsToBeUploadedAndMaxRetriesWereNotReached() {
        runTest(dispatchers.Main) {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1")
            val messageId = "messageId9273584"
            val userId = UserId("id")
            val message = Message(messageId = messageId, addressID = "senderAddress13")
            givenFullValidInput(messageId, attachmentIds.toTypedArray(), true)
            every { cryptoFactory.create(userId, AddressId("senderAddress13")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { userManager.currentUserId } returns userId
            every { userManager.requireCurrentUserId() } returns userId
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { mailSettings.attachPublicKey } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload public key")
            }
            every { parameters.runAttemptCount } returns 0

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(ListenableWorker.Result.retry(), result)
            verify { pendingActionDao.deletePendingUploadByMessageId("messageId9273584") }
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfPublicKeyFailsToBeUploadedAndMaxRetriesWereReached() {
        runTest(dispatchers.Main) {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1")
            val messageId = "messageId9273585"
            val userId = UserId("id")
            val message = Message(messageId = messageId, addressID = "senderAddress12")
            givenFullValidInput(messageId, attachmentIds.toTypedArray(), true)
            every { cryptoFactory.create(userId, AddressId("senderAddress12")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { userManager.currentUserId } returns userId
            every { userManager.requireCurrentUserId() } returns userId
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { mailSettings.attachPublicKey } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload public key")
            }
            every { parameters.runAttemptCount } returns 4

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(
                        KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to "Failed to upload public key",
                    )
                ),
                result
            )
            verify { pendingActionDao.deletePendingUploadByMessageId("messageId9273585") }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentIsNotFoundInMessageRepository() {
        runTest(dispatchers.Main) {
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
            every { cryptoFactory.create(testUserId, AddressId("senderAddress3")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { messageDetailsRepository.findAttachmentById("1") } returns null
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachmentsWorker.doWork()

            coVerifySequence { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsFailsWorkerIfAttachmentFilePathIsNull() {
        runTest(dispatchers.Main) {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { fileName } returns "Attachment_1.jpg"
                every { filePath } returns null
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { fileName } returns "Attachment_2.jpg"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { doesFileExist } returns true
            }
            val attachmentIds = listOf("1", "2")
            val messageId = "messageId36926543"
            val message = Message(messageId = messageId, addressID = "senderAddress4")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(testUserId, AddressId("senderAddress4")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { context.getString(R.string.attachment_failed_message_drafted) } returns
                "Please remove and re-attach \"%s\" and send the message again. " +
                "Your message can be found in the \"Drafts\" folder."
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            val result = uploadAttachmentsWorker.doWork()

            coVerify(exactly = 0) { attachmentsRepository.upload(attachment1, crypto) }
            coVerify(exactly = 0) { attachmentsRepository.upload(attachment2, crypto) }
            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(
                        KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to
                            "Please remove and re-attach \"Attachment_1.jpg\" and send the message again. " +
                            "Your message can be found in the \"Drafts\" folder.",
                    )
                ),
                result
            )
            verify { pendingActionDao.deletePendingUploadByMessageId("messageId36926543") }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentWasAlreadyUploaded() {
        runTest(dispatchers.Main) {
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
            every { cryptoFactory.create(testUserId, AddressId("senderAddress5")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachmentsWorker.doWork()

            coVerify { attachmentsRepository.upload(attachment1, crypto) }
            coVerify(exactly = 0) { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentFileDoesNotExist() {
        runTest(dispatchers.Main) {
            val attachmentIds = listOf("1", "2")
            val messageId = "messageId83483"
            val message = Message(messageId = messageId, addressID = "senderAddress6")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(testUserId, AddressId("senderAddress6")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
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

            uploadAttachmentsWorker.doWork()

            coVerify { attachmentsRepository.upload(attachmentMock1, crypto) }
            coVerify(exactly = 0) { attachmentsRepository.upload(attachmentMock2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsCallRepositoryUploadPublicKeyWhenMailSettingsGetAttachPublicKeyIsTrueAndMessageIsSending() {
        runTest(dispatchers.Main) {
            val userId = UserId("id")
            val messageId = "messageId823762"
            val message = Message(messageId = messageId, addressID = "senderAddress6")
            every { userManager.currentUserId } returns userId
            every { userManager.requireCurrentUserId() } returns userId
            givenFullValidInput(messageId, attachments = emptyArray(), isMessageSending = true)
            every { cryptoFactory.create(userId, AddressId("senderAddress6")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { mailSettings.attachPublicKey } returns true
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Success("23421")
            }

            uploadAttachmentsWorker.doWork()

            coVerify { attachmentsRepository.uploadPublicKey(message, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsDeletesLocalFileAfterSuccessfulUpload() {
        runTest(dispatchers.Main) {
            val attachmentIds = listOf("1", "2")
            val messageId = "messageId126943"
            val message = Message(messageId = messageId, addressID = "senderAddress7")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(testUserId, AddressId("senderAddress7")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
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

            uploadAttachmentsWorker.doWork()

            verify { attachmentMock1.deleteLocalFile() }
            verify(exactly = 0) { attachmentMock2.deleteLocalFile() }
        }
    }

    @Test
    fun uploadAttachmentsUpdatesLocalMessageWithUploadedAttachmentWhenUploadSucceeds() {
        runTest(dispatchers.Main) {
            val attachmentIds = listOf("1", "2")
            val messageId = "82342"
            val message = Message(messageId = messageId, addressID = "senderAddress8")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(testUserId, AddressId("senderAddress8")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
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
            every { messageDetailsRepository.findAttachmentById(uploadedAttachmentMock1Id) } returns
                uploadedAttachmentMock1
            every { messageDetailsRepository.findAttachmentById(uploadedAttachmentMock2Id) } returns
                uploadedAttachmentMock2
            coEvery { attachmentsRepository.upload(attachmentMock1, crypto) } answers {
                AttachmentsRepository.Result.Success(uploadedAttachmentMock1Id)
            }
            coEvery { attachmentsRepository.upload(attachmentMock2, crypto) } answers {
                AttachmentsRepository.Result.Success(uploadedAttachmentMock2Id)
            }

            uploadAttachmentsWorker.doWork()

            val actualMessages = mutableListOf<Message>()
            val expectedAttachments = listOf(uploadedAttachmentMock1, uploadedAttachmentMock2)
            verify { messageDetailsRepository.findAttachmentById(uploadedAttachmentMock2Id) }
            coVerify(exactly = 2) { messageDetailsRepository.saveMessage(capture(actualMessages)) }
            assertEquals(expectedAttachments, actualMessages.last().attachments)
        }
    }

    @Test
    fun uploadAttachmentsDoesNotUpdateLocalMessageWithUploadedAttachmentWhenUploadFails() {
        runTest(dispatchers.Main) {
            val attachmentIds = listOf("1")
            val messageId = "82342"
            val message = Message(messageId = messageId, addressID = "senderAddress9")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(testUserId, AddressId("senderAddress9")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
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

            uploadAttachmentsWorker.doWork()

            coVerify(exactly = 0) { messageDetailsRepository.saveMessage(any()) }
            verify(exactly = 1) { messageDetailsRepository.findAttachmentById("1") }
        }
    }

    @Test
    fun uploadAttachmentsDoesNotAddUploadedAttachmentToLocalMessageIfAttachmentWasNotInTheMessageAttachments() {
        runTest(dispatchers.Main) {
            val attachmentIds = listOf("1", "2")
            val messageId = "82342"
            val message = Message(messageId = messageId, addressID = "senderAddress10")
            givenFullValidInput(messageId, attachmentIds.toTypedArray())
            every { cryptoFactory.create(testUserId, AddressId("senderAddress10")) } returns crypto
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
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

            uploadAttachmentsWorker.doWork()

            val actualMessages = mutableListOf<Message>()
            val expectedAttachments = listOf(attachmentMock1)
            coVerify(exactly = 2) { messageDetailsRepository.saveMessage(capture(actualMessages)) }
            assertEquals(expectedAttachments, actualMessages.last().attachments)
        }
    }

    @Test
    fun uploadAttachmentsDoesNotAttachThePublicKeyIfTheMessageIsNotBeingSent() {
        runTest(dispatchers.Main) {
            val messageId = "message-id-123842"
            val message = Message(messageId, addressID = "addressId")
            val userId = UserId("id")
            givenFullValidInput(messageId, emptyArray(), isMessageSending = false)
            every { userManager.currentUserId } returns userId
            every { userManager.requireCurrentUserId() } returns userId
            every { pendingActionDao.findPendingUploadByMessageIdBlocking(messageId) } returns null
            every { mailSettings.attachPublicKey } returns true
            coEvery {
                messageDetailsRepository.findMessageById(
                    messageId
                )
            } returns flowOf(message)
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } returns mockk()

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify(exactly = 0) { attachmentsRepository.uploadPublicKey(any(), any()) }
        }
    }

    @Test
    fun uploadAttachmentsAttachesThePublicKeyOnlyWhenTheMessageIsBeingSent() {
        runTest(dispatchers.Main) {
            val messageId = "message-id-123842"
            val message = Message(messageId, addressID = "addressId")
            val userId = UserId("id")
            givenFullValidInput(messageId, emptyArray(), isMessageSending = true)
            every { userManager.currentUserId } returns userId
            every { userManager.requireCurrentUserId() } returns userId
            every { pendingActionDao.findPendingUploadByMessageIdBlocking(messageId) } returns null
            every { mailSettings.attachPublicKey } returns true
            coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
            every { cryptoFactory.create(userId, AddressId("addressId")) } returns crypto
            coEvery { attachmentsRepository.uploadPublicKey(message, crypto) } answers {
                AttachmentsRepository.Result.Success("82384")
            }

            val result = uploadAttachmentsWorker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            coVerify { attachmentsRepository.uploadPublicKey(any(), any()) }
        }
    }

    private fun givenFullValidInput(
        messageId: String,
        attachments: Array<String> = arrayOf("attId62364"),
        isMessageSending: Boolean = false
    ) {
        every { parameters.inputData.getStringArray(KEY_INPUT_UPLOAD_ATTACHMENTS_ATTACHMENT_IDS) } answers
            { attachments }
        every { parameters.inputData.getString(KEY_INPUT_UPLOAD_ATTACHMENTS_MESSAGE_ID) } answers { messageId }
        every {
            parameters.inputData.getBoolean(KEY_INPUT_UPLOAD_ATTACHMENTS_IS_MESSAGE_SENDING, false)
        } answers { isMessageSending }
    }
}
