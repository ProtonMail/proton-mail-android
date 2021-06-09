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

package ch.protonmail.android.worker

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.messages.ParsedHeaders
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.NONE
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.*
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.utils.base64.Base64Encoder
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.worker.drafts.CreateDraftWorker
import ch.protonmail.android.worker.drafts.CreateDraftWorkerErrors
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_ACTION_TYPE
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_MSG_DB_ID
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_USER_ID
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test

class CreateDraftWorkerTest : CoroutinesTest {

    private val testUserId = Id("id")

    private val userNotifier: UserNotifier = mockk(relaxed = true)

    private val parameters: WorkerParameters = mockk(relaxed = true) {
        every { inputData.getString(KEY_INPUT_SAVE_DRAFT_USER_ID) } returns testUserId.s
    }

    private val messageFactory: MessageFactory = mockk(relaxed = true)

    private val messageDetailsRepository: MessageDetailsRepository = mockk {
        coEvery { saveMessage(any()) } returns 0
    }

    private val workManager: WorkManager = mockk(relaxed = true)

    private val userManager: UserManager = mockk(relaxed = true) {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
        coEvery { getUser(testUserId) } returns mockk(relaxed = true)
    }

    private val addressCryptoFactory: AddressCrypto.Factory = mockk(relaxed = true)

    private val base64: Base64Encoder = mockk(relaxed = true)

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val databaseProvider: DatabaseProvider = mockk(relaxed = true)

    private val worker = CreateDraftWorker(
        context = mockk(),
        parameters,
        messageDetailsRepository,
        messageFactory,
        userManager,
        addressCryptoFactory,
        base64,
        apiManager,
        databaseProvider,
        userNotifier
    )

    @BeforeTest
    fun setUp() {
        coEvery { apiManager.createDraft(any()) } returns mockk(relaxed = true)
        coEvery { messageDetailsRepository.findAttachmentsByMessageId(any()) } returns emptyList()
    }

    @Test
    fun workerEnqueuerCreatesOneTimeRequestWorkerWhichIsUniqueForMessageId() {
        runBlockingTest {
            // Given
            val messageParentId = "98234"
            val messageId = "2834"
            val messageDbId = 534L
            val messageActionType = REPLY_ALL
            val message = Message(messageId = messageId)
            message.dbId = messageDbId
            val previousSenderAddressId = "previousSenderId82348"

            // When
            CreateDraftWorker.Enqueuer(workManager).enqueue(
                userId = testUserId,
                message = message,
                parentId = messageParentId,
                actionType = messageActionType,
                previousSenderAddressId = previousSenderAddressId
            )

            // Then
            val requestSlot = slot<OneTimeWorkRequest>()
            verify {
                workManager.enqueueUniqueWork(
                    "saveDraftUniqueWork-$messageId",
                    ExistingWorkPolicy.REPLACE,
                    capture(requestSlot)
                )
            }
            val workSpec = requestSlot.captured.workSpec
            val constraints = workSpec.constraints
            val inputData = workSpec.input
            val actualMessageDbId = inputData.getLong(KEY_INPUT_SAVE_DRAFT_MSG_DB_ID, -1)
            val actualMessageLocalId = inputData.getString(KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID)
            val actualMessageParentId = inputData.getString(KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID)
            val actualMessageActionType = inputData.getInt(KEY_INPUT_SAVE_DRAFT_ACTION_TYPE, -1)
            val actualPreviousSenderAddress = inputData.getString(KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID)
            assertEquals(message.dbId, actualMessageDbId)
            assertEquals(message.messageId, actualMessageLocalId)
            assertEquals(messageParentId, actualMessageParentId)
            assertEquals(messageActionType.messageActionTypeValue, actualMessageActionType)
            assertEquals(previousSenderAddressId, actualPreviousSenderAddress)
            assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
            assertEquals(BackoffPolicy.EXPONENTIAL, workSpec.backoffPolicy)
            assertEquals(20000, workSpec.backoffDelayDuration)
            verify { workManager.getWorkInfoByIdLiveData(any()) }
        }
    }

    @Test
    fun workerReturnsMessageNotFoundErrorWhenMessageDetailsRepositoryDoesNotReturnAValidMessage() {
        runBlockingTest {
            // Given
            val messageDbId = 345L
            givenMessageIdInput(messageDbId)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(null)

            // When
            val result = worker.doWork()

            // Then
            val error = CreateDraftWorkerErrors.MessageNotFound
            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerSetsParentIdAndActionTypeOnCreateDraftRequestWhenParentIdIsGivenAndDraftIsBeingCreated() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val actionType = FORWARD
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c27-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId"
                messageBody = "messageBody"
            }
            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(actionType)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            verify { apiDraftMessage.parentID = parentId }
            verify { apiDraftMessage.action = 2 }
            // Always get parent message from messageDetailsDB, never from searchDB
            // ignoring isTransient property as the values in the two DB appears to be the same
            verify { messageDetailsRepository.findMessageById(parentId) }
        }
    }

    @Test
    fun workerSetsSenderAndMessageBodyOnCreateOrUpdateDraftRequest() {
        runBlockingTest {
            // Given
            val messageDbId = 345L
            val addressId = "addressId"
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c24-c3d9-4f3a-9188-02dea1321cc6"
                addressID = addressId
                messageBody = "messageBody"
            }
            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            val address = Address(
                Id(addressId),
                null,
                EmailAddress("sender@email.it"),
                Name("senderName"),
                null,
                true,
                Address.Type.ORIGINAL,
                allowedToSend = true,
                allowedToReceive = false,
                keys = AddressKeys(null, emptyList())
            )
            givenMessageIdInput(messageDbId)
            givenActionTypeInput()
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { userManager.currentUserId } returns testUserId
            coEvery { userManager.getUser(testUserId) } returns mockk {
                every { findAddressById(Id(addressId)) } returns address
            }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById("") } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            val messageSender = MessageSender("senderName", "sender@email.it")
            verify { apiDraftMessage.setSender(messageSender) }
            verify { apiDraftMessage.setMessageBody("messageBody") }
        }
    }

    @Test
    fun workerUsesMessageSenderToRequestDraftCreationWhenMessageIsBeingSentByAlias() {
        runBlockingTest {
            // Given
            val messageDbId = 89234L
            val addressId = "addressId234"
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c30-c3d9-4f3a-9188-02dea1321cc6"
                addressID = addressId
                messageBody = "messageBody2341"
                sender = MessageSender("sender by alias", "sender+alias@pm.me")
            }
            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenActionTypeInput()
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById("") } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            val messageSender = MessageSender("sender by alias", "sender+alias@pm.me")
            verify { apiDraftMessage.setSender(messageSender) }
        }
    }

    @Test
    fun workerAddsReEncryptedParentAttachmentsToRequestWhenActionIsForwardAndSenderAddressChanged() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val actionType = FORWARD
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c26-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId835"
                messageBody = "messageBody"
            }
            val attachment = Attachment("attachment1", "pic.jpg", "image/jpeg", keyPackets = "somePackets")
            val previousSenderAddressId = "previousSenderId82348"
            val privateKey = PgpField.PrivateKey(NotBlankString("current sender private key"))
            val senderPublicKey = "new sender public key"
            val decodedPacketsBytes = "decoded attachment packets".toByteArray()
            val encryptedKeyPackets = "re-encrypted att key packets".toByteArray()

            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            val senderAddress = mockk<Address>(relaxed = true) {
                every { keys.primaryKey?.privateKey } returns privateKey
            }
            val addressCrypto = mockk<AddressCrypto>(relaxed = true) {
                val sessionKey = "session key".toByteArray()
                every { buildArmoredPublicKey(privateKey) } returns senderPublicKey
                every { decryptKeyPacket(decodedPacketsBytes) } returns sessionKey
                every { encryptKeyPacket(sessionKey, senderPublicKey) } returns encryptedKeyPackets
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(actionType)
            givenPreviousSenderAddress(previousSenderAddressId)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)
            every { userManager.currentUserId } returns testUserId
            coEvery { userManager.getUser(testUserId) } returns mockk {
                every { findAddressById(Id("addressId835")) } returns senderAddress
            }
            every { addressCryptoFactory.create(testUserId, Id(previousSenderAddressId)) } returns addressCrypto
            every { addressCrypto.buildArmoredPublicKey(privateKey) } returns senderPublicKey
            every { base64.decode(attachment.keyPackets!!) } returns decodedPacketsBytes
            every { base64.encode(encryptedKeyPackets) } returns "encrypted encoded packets"

            // When
            worker.doWork()

            // Then
            val attachmentReEncrypted = attachment.copy(keyPackets = "encrypted encoded packets")
            coVerify { parentMessage.attachments(databaseProvider.provideMessageDao(testUserId)) }
            verify { addressCrypto.buildArmoredPublicKey(privateKey) }
            verify { apiDraftMessage.addAttachmentKeyPacket("attachment1", attachmentReEncrypted.keyPackets!!) }
        }
    }

    @Test
    fun workerSkipsNonInlineParentAttachmentsWhenActionIsReplyAllAndSenderAddressChanged() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c25-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId835"
                messageBody = "messageBody"
            }
            val attachment = Attachment("attachment1", "pic.jpg", "image/jpeg", keyPackets = "somePackets", inline = true)
            val attachment2 = Attachment("attachment2", "pic2.jpg", keyPackets = "somePackets2", inline = false)
            val previousSenderAddressId = "previousSenderId82348"

            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment, attachment2)
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(REPLY_ALL)
            givenPreviousSenderAddress(previousSenderAddressId)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)
            every { userManager.currentUserId } returns Id("another")

            // When
            worker.doWork()

            // Then
            verify(exactly = 0) { apiDraftMessage.addAttachmentKeyPacket("attachment2", any()) }
        }
    }

    @Test
    fun workerAddsExistingParentAttachmentsToRequestWhenSenderAddressWasNotChanged() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c29-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId835"
                messageBody = "messageBody"
            }
            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(
                    Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true),
                    Attachment("attachment1", keyPackets = "Attachment1KeyPackets", inline = false),
                    Attachment("attachment2", keyPackets = "Attachment2KeyPackets", inline = true)
                )
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(FORWARD)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            coVerify { parentMessage.attachments(databaseProvider.provideMessageDao(testUserId)) }
            verifyOrder {
                apiDraftMessage.addAttachmentKeyPacket("attachment", "OriginalAttachmentPackets")
                apiDraftMessage.addAttachmentKeyPacket("attachment1", "Attachment1KeyPackets")
                apiDraftMessage.addAttachmentKeyPacket("attachment2", "Attachment2KeyPackets")
            }
        }
    }

    @Test
    fun workerAddsOnlyInlineParentAttachmentsToRequestWhenActionIsReplyAndSenderAddressWasNotChanged() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c28-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId835"
                messageBody = "messageBody"
            }
            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(
                    Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true),
                    Attachment("attachment1", keyPackets = "Attachment1KeyPackets", inline = false),
                    Attachment("attachment2", keyPackets = "Attachment2KeyPackets", inline = true)
                )
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(REPLY)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            coVerify { parentMessage.attachments(databaseProvider.provideMessageDao(testUserId)) }
            verifyOrder {
                apiDraftMessage.addAttachmentKeyPacket("attachment", "OriginalAttachmentPackets")
                apiDraftMessage.addAttachmentKeyPacket("attachment2", "Attachment2KeyPackets")
            }

            verify(exactly = 0) { apiDraftMessage.addAttachmentKeyPacket("attachment1", "Attachment1KeyPackets") }
            verifyOrder {
                apiDraftMessage.addAttachmentKeyPacket("attachment", "OriginalAttachmentPackets")
                apiDraftMessage.addAttachmentKeyPacket("attachment2", "Attachment2KeyPackets")
            }
        }
    }

    @Test
    fun workerDoesNotAddParentAttachmentsToRequestWhenActionTypeIsOtherThenForwardReplyReplyAll() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c31-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId835"
                messageBody = "messageBody"
            }

            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            verify(exactly = 0) { apiDraftMessage.addAttachmentKeyPacket(any(), any()) }
        }
    }

    @Test
    fun workerDoesNotAddParentDataToRequestWhenDraftCreationAlreadyHappenedAndDraftIsBeingUpdated() {
        runBlockingTest {
            // Given
            val parentId = "88237"
            val messageDbId = 9238L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "remote-message-id2837"
                addressID = "addressId835"
                messageBody = "messageBody"
            }

            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(FORWARD)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            verify(exactly = 0) { apiDraftMessage.parentID = any() }
            verify(exactly = 0) { apiDraftMessage.addAttachmentKeyPacket(any(), any()) }
        }
    }

    @Test
    fun workerAddsExistingMessageAttachmentsKeyPacketsToRequestWhenSenderChangedAndPacketsWereAlreadyEncryptedWithTheCurrentSender() {
        // This case will happen when changing sender and then performing multiple draft updates (eg. through auyo save)
        runBlockingTest {
            // Given
            val parentId = "8238"
            val messageDbId = 1275L
            val messageId = "remote-message-ID-draft-being-updated8723"
            val messageAttachments = listOf(
                Attachment("attachment1", keyPackets = "MessageAtta1KeyPacketsBase64", inline = false),
            )
            val message = mockk<Message>(relaxed = true) {
                coEvery { attachments(any()) } returns messageAttachments
                every { dbId } returns messageDbId
                every { this@mockk.messageId } returns messageId
                every { addressID } returns "currentAddressId12346"
                every { messageBody } returns "messageBody"
            }
            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            val previousSenderAddressId = "senderAddressUsedInFirstDraftCreation1"
            givenPreviousSenderAddress(previousSenderAddressId)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findAttachmentsByMessageId(messageId) } returns messageAttachments
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            every { userManager.currentUserId } returns testUserId
            every { userManager.requireCurrentUserId() } returns testUserId
            every { addressCryptoFactory.create(testUserId, Id(previousSenderAddressId)) } returns mockk(relaxed = true) {
                every { decryptKeyPacket(any()) } throws Exception("Decryption failed")
            }

            // When
            worker.doWork()

            // Then
            coVerify { messageDetailsRepository.findAttachmentsByMessageId(messageId) }
            verifyOrder {
                apiDraftMessage.addAttachmentKeyPacket("attachment1", "MessageAtta1KeyPacketsBase64")
            }
        }
    }

    @Test
    fun workerReEncryptsMessageAttachmentsKeyPacketsAndAddsThemToRequestWhenSenderAddressWasChanged() {
        runBlockingTest {
            // Given
            val parentId = "8823"
            val messageDbId = 1274L
            val messageId = "remote-message-ID-draft-being-updated2323"
            val messageAttachments = listOf(
                Attachment("attachment1", keyPackets = "MessageAtta1KeyPacketsBase64", inline = true),
                Attachment("attachment2", keyPackets = "MessageAtta2KeyPacketsBase64", inline = false),
                Attachment("attachment3", keyPackets = "MessageAtta3KeyPacketsBase64", inline = false)
            )
            val message = mockk<Message>(relaxed = true) {
                coEvery { attachments(any()) } returns messageAttachments
                every { dbId } returns messageDbId
                every { this@mockk.messageId } returns messageId
                every { addressID } returns "currentAddressId12345"
                every { messageBody } returns "messageBody"
            }
            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            val previousSenderAddressId = "senderAddressUsedInFirstDraftCreation"
            givenPreviousSenderAddress(previousSenderAddressId)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findAttachmentsByMessageId(messageId) } returns messageAttachments
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            every { userManager.currentUserId } returns testUserId
            every { userManager.requireCurrentUserId() } returns testUserId

            every { base64.decode("MessageAtta1KeyPacketsBase64") } returns "MessageAtta1KeyPackets".toByteArray()
            every { base64.decode("MessageAtta2KeyPacketsBase64") } returns "MessageAtta2KeyPackets".toByteArray()
            every { base64.decode("MessageAtta3KeyPacketsBase64") } returns "MessageAtta3KeyPackets".toByteArray()

            every { addressCryptoFactory.create(testUserId, Id(previousSenderAddressId)) } returns mockk(relaxed = true) {
                every { decryptKeyPacket("MessageAtta1KeyPackets".toByteArray()) } returns "decryptedKeyPackets1".toByteArray()
                every { decryptKeyPacket("MessageAtta2KeyPackets".toByteArray()) } returns "decryptedKeyPackets2".toByteArray()
                every { decryptKeyPacket("MessageAtta3KeyPackets".toByteArray()) } returns "decryptedKeyPackets3".toByteArray()

                every { encryptKeyPacket("decryptedKeyPackets1".toByteArray(), any()) } returns "MessageAtt1ReEncryptedPackets".toByteArray()
                every { encryptKeyPacket("decryptedKeyPackets2".toByteArray(), any()) } returns "MessageAtt2ReEncryptedPackets".toByteArray()
                every { encryptKeyPacket("decryptedKeyPackets3".toByteArray(), any()) } returns "MessageAtt3ReEncryptedPackets".toByteArray()
            }
            every { base64.encode("MessageAtt1ReEncryptedPackets".toByteArray()) } returns "MessageAtt1ReEncryptedPacketsBase64"
            every { base64.encode("MessageAtt2ReEncryptedPackets".toByteArray()) } returns "MessageAtt2ReEncryptedPacketsBase64"
            every { base64.encode("MessageAtt3ReEncryptedPackets".toByteArray()) } returns "MessageAtt3ReEncryptedPacketsBase64"

            // When
            worker.doWork()

            // Then
            coVerify { messageDetailsRepository.findAttachmentsByMessageId(messageId) }
            verifyOrder {
                apiDraftMessage.addAttachmentKeyPacket("attachment1", "MessageAtt1ReEncryptedPacketsBase64")
                apiDraftMessage.addAttachmentKeyPacket("attachment2", "MessageAtt2ReEncryptedPacketsBase64")
                apiDraftMessage.addAttachmentKeyPacket("attachment3", "MessageAtt3ReEncryptedPacketsBase64")
            }
        }
    }

    @Test
    fun workerAddsExistingMessageAttachmentsKeyPacketsToRequestWhenSenderAddressWasNotChanged() {
        runBlockingTest {
            // Given
            val parentId = "8234"
            val messageDbId = 1273L
            val messageId = "remote-message-ID-draft-being-updated2348"
            val messageAttachments = listOf(
                Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true),
                Attachment("attachment1", keyPackets = "Attachment1KeyPackets", inline = false),
                Attachment("attachment2", keyPackets = "Attachment2KeyPackets", inline = false)
            )
            val message = mockk<Message>(relaxed = true) {
                coEvery { attachments(any()) } returns messageAttachments
                every { dbId } returns messageDbId
                every { this@mockk.messageId } returns messageId
                every { addressID } returns "addressId12345"
                every { messageBody } returns "messageBody"
            }
            val apiDraftMessage = mockk<DraftBody>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findAttachmentsByMessageId(messageId) } returns messageAttachments
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            // When
            worker.doWork()

            // Then
            coVerify { messageDetailsRepository.findAttachmentsByMessageId(messageId) }
            verifyOrder {
                apiDraftMessage.addAttachmentKeyPacket("attachment", "OriginalAttachmentPackets")
                apiDraftMessage.addAttachmentKeyPacket("attachment1", "Attachment1KeyPackets")
                apiDraftMessage.addAttachmentKeyPacket("attachment2", "Attachment2KeyPackets")
            }
        }
    }

    @Test
    fun workerPerformsCreateDraftRequestAndBuildsMessageFromResponseWhenRequestSucceeds() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "ac7b3d53-fc64-4d44-a1f5-39ed45b629ef"
                addressID = "addressId835"
                messageBody = "messageBody"
                sender = MessageSender("sender2342", "senderEmail@2340.com")
                setLabelIDs(listOf("label", "label1", "label2"))
                parsedHeaders = ParsedHeaders("recEncryption", "recAuth")
                numAttachments = 2
                attachments = emptyList()
            }

            val apiDraftRequest = mockk<DraftBody>(relaxed = true)
            val responseMessage = Message(messageId = "created_draft_id").apply {
                attachments = listOf(Attachment("235423"), Attachment("823421"))
            }
            val apiDraftResponse = mockk<MessageResponse> {
                every { code } returns 1000
                every { messageId } returns "created_draft_id"
                every { this@mockk.message } returns responseMessage
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } returns apiDraftRequest
            coEvery { apiManager.createDraft(apiDraftRequest) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            coVerify { apiManager.createDraft(apiDraftRequest) }
            val expected = Message().apply {
                this.dbId = messageDbId
                this.messageId = "created_draft_id"
                this.toList = listOf()
                this.ccList = listOf()
                this.bccList = listOf()
                this.replyTos = listOf()
                this.sender = message.sender
                this.setLabelIDs(message.getEventLabelIDs())
                this.parsedHeaders = message.parsedHeaders
                this.isDownloaded = true
                this.setIsRead(true)
                this.numAttachments = message.numAttachments
                this.attachments = responseMessage.attachments
                this.localId = message.messageId
            }
            val actualMessage = slot<Message>()
            coVerify { messageDetailsRepository.saveMessage(capture(actualMessage)) }
            assertEquals(expected, actualMessage.captured)
            assertEquals(expected.attachments, actualMessage.captured.attachments)
        }
    }

    @Test
    fun workerUpdatesLocalDbDraftWithCreatedDraftAndReturnsSuccessWhenRequestSucceeds() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                addressID = "addressId835"
                messageId = "ac7b3d53-fc64-4d44-a1f5-39df45b629ef"
                messageBody = "messageBody"
                sender = MessageSender("sender2342", "senderEmail@2340.com")
                setLabelIDs(listOf("label", "label1", "label2"))
                parsedHeaders = ParsedHeaders("recEncryption", "recAuth")
                numAttachments = 3
            }

            val apiDraftRequest = mockk<DraftBody>(relaxed = true)
            val responseMessage = message.copy(
                messageId = "response_message_id",
                isDownloaded = true,
                localId = "ac7b3d53-fc64-4d44-a1f5-39df45b629ef",
                Unread = false
            )
            responseMessage.dbId = messageDbId
            val apiDraftResponse = mockk<MessageResponse> {
                every { code } returns 1000
                every { messageId } returns "response_message_id"
                every { this@mockk.message } returns responseMessage
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } returns apiDraftRequest
            coEvery { apiManager.createDraft(apiDraftRequest) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            val result = worker.doWork()

            // Then
            coVerify { messageDetailsRepository.saveMessage(responseMessage) }
            assertEquals(message.dbId, responseMessage.dbId)
            val expected = ListenableWorker.Result.success(
                Data.Builder().putString(KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID, "response_message_id").build()
            )
            assertEquals(expected, result)
        }
    }

    @Test
    fun workerReturnsFailureWithoutRetryingWhenApiRequestSucceedsButReturnsNonSuccessResponseCode() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 3452L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c23-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId835"
                messageBody = "messageBody"
                subject = "Subject002"
            }
            val errorMessage = "Draft not created because.."
            val errorAPIResponse = mockk<MessageResponse> {
                every { code } returns 402
                every { error } returns errorMessage
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject002"
            }
            coEvery { apiManager.createDraft(any()) } returns errorAPIResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)
            every { parameters.runAttemptCount } returns 1

            // When
            val result = worker.doWork()

            verify { userNotifier.showPersistentError(errorMessage, "Subject002") }
            val expected = ListenableWorker.Result.failure(
                Data.Builder().putString(
                    KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM,
                    CreateDraftWorkerErrors.BadResponseCodeError.name
                ).build()
            )
            assertEquals(expected, result)
        }
    }

    @Test
    fun workerRetriesSavingDraftWhenApiRequestFailsAndMaxTriesWereNotReached() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c22-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId835"
                messageBody = "messageBody"
                subject = "Subject001"
            }
            val errorMessage = "Error performing request"
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject001"
            }
            coEvery { apiManager.createDraft(any()) } throws IOException(errorMessage)
            every { parameters.runAttemptCount } returns 2
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            val result = worker.doWork()

            // Then
            val expected = ListenableWorker.Result.retry()
            assertEquals(expected, result)
        }
    }

    @Test
    fun workerNotifiesErrorAndReturnsFailureWithErrorWhenAPIRequestFailsAndMaxTriesWereReached() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c22-c3d9-4f3a-9188-02dea1321cc6"
                addressID = "addressId835"
                messageBody = "messageBody"
                subject = "Subject001"
            }
            val errorMessage = "Error performing request"
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject001"
            }
            coEvery { apiManager.createDraft(any()) } throws IOException(errorMessage)
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)
            every { parameters.runAttemptCount } returns 4

            // When
            val result = worker.doWork()

            // Then
            verify { userNotifier.showPersistentError(errorMessage, "Subject001") }
            val expected = ListenableWorker.Result.failure(
                Data.Builder().putString(
                    KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM,
                    CreateDraftWorkerErrors.ServerError.name
                ).build()
            )
            assertEquals(expected, result)
        }
    }

    @Test
    fun workerPerformsUpdateDraftRequestWhenMessageIsNotLocalAndStoresResponseMessageInDbWhenRequestSucceeds() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val remoteMessageId = "7pmfkddyCO69Ch5Gzn0b517H-x-zycdj1Urhn-pj6Eam38FnYY3IxZ62jJ-gbwxVg=="
            val message = Message().apply {
                dbId = messageDbId
                messageId = remoteMessageId
                addressID = "addressId835"
                messageBody = "messageBody"
                sender = MessageSender("sender2342", "senderEmail@2340.com")
                setLabelIDs(listOf("label", "label1", "label2"))
                parsedHeaders = ParsedHeaders("recEncryption", "recAuth")
                numAttachments = 1
                attachments = listOf(Attachment(attachmentId = "12749"))
            }

            val apiDraftRequest = mockk<DraftBody>(relaxed = true)
            val responseMessage = Message(messageId = "created_draft_id").apply {
                attachments = listOf(Attachment(attachmentId = "82374"))
            }
            val apiDraftResponse = mockk<MessageResponse> {
                every { code } returns 1000
                every { messageId } returns "created_draft_id"
                every { this@mockk.message } returns responseMessage
            }
            val retrofitTag = UserIdTag(testUserId)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(message)
            every { messageFactory.createDraftApiRequest(message) } returns apiDraftRequest
            coEvery { apiManager.updateDraft(remoteMessageId, apiDraftRequest, retrofitTag) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            worker.doWork()

            // Then
            coVerify { apiManager.updateDraft(remoteMessageId, apiDraftRequest, retrofitTag) }
            val expectedMessage = Message().apply {
                this.dbId = messageDbId
                this.messageId = "created_draft_id"
                this.toList = listOf()
                this.ccList = listOf()
                this.bccList = listOf()
                this.replyTos = listOf()
                this.sender = message.sender
                this.setLabelIDs(message.getEventLabelIDs())
                this.parsedHeaders = message.parsedHeaders
                this.isDownloaded = true
                this.setIsRead(true)
                this.numAttachments = message.numAttachments
                this.attachments = responseMessage.attachments
                this.localId = message.messageId
            }
            val actualMessage = slot<Message>()
            coVerify { messageDetailsRepository.saveMessage(capture(actualMessage)) }
            assertEquals(expectedMessage, actualMessage.captured)
            assertEquals(expectedMessage.attachments, actualMessage.captured.attachments)
        }
    }

    @Test
    fun workerSavesMessageLocallyWithAttachmentsFromApiDraftPlusAnyLocalAttachmentThatWasNotUploaded() {
        runBlockingTest {
            // Given
            val parentId = "8238482"
            val messageDbId = 345L
            val remoteMessageId = "7pmfkkkyCO69Ch5Gzn0b517H-x-zycdj1Urhn-pj6Eam38FnYY3IxZ62jJ-gbwxVg=="
            val localAttachment = Attachment(attachmentId = "LOCAL-82374", isUploaded = false)
            val localMessage = Message().apply {
                dbId = messageDbId
                messageId = remoteMessageId
                addressID = "addressId8238"
                messageBody = "messageBody12"
                sender = MessageSender("sender92384", "senderEmail@8238.com")
                setLabelIDs(listOf("label", "label1", "label2"))
                parsedHeaders = ParsedHeaders("recEncryption", "recAuth")
                numAttachments = 1
                attachments = listOf(localAttachment)
            }

            val apiDraftRequest = mockk<DraftBody>(relaxed = true)
            val remoteAttachment = Attachment(attachmentId = "REMOTE-12736", isUploaded = true)
            val responseMessage = Message(messageId = "created_draft_id").apply {
                attachments = listOf(remoteAttachment)
            }
            val apiDraftResponse = mockk<MessageResponse> {
                every { code } returns 1000
                every { messageId } returns "created_draft_id"
                every { this@mockk.message } returns responseMessage
            }
            val retrofitTag = UserIdTag(testUserId)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns flowOf(localMessage)
            every { messageFactory.createDraftApiRequest(localMessage) } returns apiDraftRequest
            coEvery { apiManager.updateDraft(remoteMessageId, apiDraftRequest, retrofitTag) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            worker.doWork()

            // Then
            coVerify { apiManager.updateDraft(remoteMessageId, apiDraftRequest, retrofitTag) }
            val expectedMessage = Message().apply {
                this.dbId = messageDbId
                this.messageId = "created_draft_id"
                this.toList = listOf()
                this.ccList = listOf()
                this.bccList = listOf()
                this.replyTos = listOf()
                this.sender = localMessage.sender
                this.setLabelIDs(localMessage.getEventLabelIDs())
                this.parsedHeaders = localMessage.parsedHeaders
                this.isDownloaded = true
                this.setIsRead(true)
                this.numAttachments = localMessage.numAttachments
                this.attachments = listOf(localAttachment, remoteAttachment)
                this.localId = localMessage.messageId
            }
            val actualMessage = slot<Message>()
            coVerify { messageDetailsRepository.saveMessage(capture(actualMessage)) }
            assertEquals(expectedMessage, actualMessage.captured)
            assertEquals(expectedMessage.attachments, actualMessage.captured.attachments)
        }
    }

    private fun givenPreviousSenderAddress(address: String) {
        every { parameters.inputData.getString(KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID) } answers { address }
    }

    private fun givenActionTypeInput(actionType: Constants.MessageActionType = NONE) {
        every {
            parameters.inputData.getInt(KEY_INPUT_SAVE_DRAFT_ACTION_TYPE, -1)
        } answers {
            actionType.messageActionTypeValue
        }
    }

    private fun givenParentIdInput(parentId: String) {
        every { parameters.inputData.getString(KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID) } answers { parentId }
    }

    private fun givenMessageIdInput(messageDbId: Long) {
        every { parameters.inputData.getLong(KEY_INPUT_SAVE_DRAFT_MSG_DB_ID, -1) } answers { messageDbId }
    }
}
