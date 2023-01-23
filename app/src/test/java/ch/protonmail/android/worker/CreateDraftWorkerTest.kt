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

package ch.protonmail.android.worker

import androidx.work.BackoffPolicy
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.MessagePayload
import ch.protonmail.android.api.models.messages.ParsedHeaders
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.messages.receive.ServerMessageSender
import ch.protonmail.android.api.segments.RESPONSE_CODE_UNPROCESSABLE_ENTITY
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.NONE
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.repository.MessageRepository
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.assertTrue
import me.proton.core.user.domain.entity.AddressId
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CreateDraftWorkerTest : CoroutinesTest by CoroutinesTest() {

    private val testUserId = UserId("id")
    private val testMessagePayload = MessagePayload(
        sender = ServerMessageSender(address = "some@pm.me"),
        body = "some message body"
    )

    private val userNotifier: UserNotifier = mockk(relaxed = true)

    private val parameters: WorkerParameters = mockk(relaxed = true) {
        every { inputData.getString(KEY_INPUT_SAVE_DRAFT_USER_ID) } returns testUserId.id
    }

    private val messageFactory: MessageFactory = mockk(relaxed = true)

    private val messageDetailsRepository: MessageDetailsRepository = mockk {
        coEvery { saveMessage(any()) } returns 0
    }
    
    private val messageRepository: MessageRepository = mockk()

    private val workManager: WorkManager = mockk(relaxed = true)

    private val userManager: UserManager = mockk(relaxed = true) {
        every { currentUserId } returns testUserId
        every { requireCurrentUserId() } returns testUserId
        coEvery { getUser(testUserId) } returns mockk(relaxed = true)
    }

    private val addressCryptoFactory: AddressCrypto.Factory = mockk(relaxed = true)

    private val base64: Base64Encoder = mockk(relaxed = true)

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val worker = CreateDraftWorker(
        context = mockk(),
        params = parameters,
        messageDetailsRepository = messageDetailsRepository,
        messageRepository = messageRepository,
        messageFactory = messageFactory,
        userManager = userManager,
        addressCryptoFactory = addressCryptoFactory,
        base64 = base64,
        apiManager = apiManager,
        userNotifier = userNotifier
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
            assertEquals(BackoffPolicy.LINEAR, workSpec.backoffPolicy)
            assertEquals(10000, workSpec.backoffDelayDuration)
            verify { workManager.getWorkInfoByIdLiveData(any()) }
        }
    }

    @Test
    fun workerReturnsMessageNotFoundErrorWhenMessageDetailsRepositoryDoesNotReturnAValidMessage() {
        runBlockingTest {
            // Given
            val messageDbId = 345L
            givenMessageIdInput(messageDbId)
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns null

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
            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(actionType)
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            withCapturedDraftBody { draftBody ->
                assertEquals(parentId, draftBody.parentId)
                assertEquals(2, draftBody.action)
            }
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
            val apiDraftMessage = DraftBody(message = testMessagePayload)
            val address = Address(
                AddressId(addressId),
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { userManager.currentUserId } returns testUserId
            coEvery { userManager.getUser(testUserId) } returns mockk {
                every { findAddressById(AddressId(addressId)) } returns address
            }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById("") } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            val messageSender = ServerMessageSender("senderName", "sender@email.it")
            withCapturedDraftBody { draftBody ->
                assertEquals(messageSender, draftBody.message.sender)
                assertEquals("messageBody", draftBody.message.body)
            }
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
            val apiDraftMessage = DraftBody(message = testMessagePayload)
            givenMessageIdInput(messageDbId)
            givenActionTypeInput()
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById("") } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            val messageSender = ServerMessageSender("sender by alias", "sender+alias@pm.me")
            withCapturedDraftBody { draftBody ->
                assertEquals(messageSender, draftBody.message.sender)
            }
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

            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)
            every { userManager.currentUserId } returns testUserId
            coEvery { userManager.getUser(testUserId) } returns mockk {
                every { findAddressById(AddressId("addressId835")) } returns senderAddress
            }
            every { addressCryptoFactory.create(testUserId, AddressId(previousSenderAddressId)) } returns addressCrypto
            every { addressCrypto.buildArmoredPublicKey(privateKey) } returns senderPublicKey
            every { base64.decode(attachment.keyPackets!!) } returns decodedPacketsBytes
            every { base64.encode(encryptedKeyPackets) } returns "encrypted encoded packets"

            // When
            worker.doWork()

            // Then
            val attachmentReEncrypted = attachment.copy(keyPackets = "encrypted encoded packets")
            verify { addressCrypto.buildArmoredPublicKey(privateKey) }
            withCapturedDraftBody { draftBody ->
                assertEquals(1, draftBody.attachmentKeyPackets.size)
                assertEquals(attachmentReEncrypted.keyPackets, draftBody.attachmentKeyPackets.values.first())
            }
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
            val attachment = Attachment(
                "attachment1",
                "pic.jpg",
                "image/jpeg",
                keyPackets = "somePackets",
                inline = true
            )
            val attachment2 = Attachment(
                "attachment2",
                "pic2.jpg",
                keyPackets = "somePackets2",
                inline = false
            )
            val previousSenderAddressId = "previousSenderId82348"

            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment, attachment2)
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(REPLY_ALL)
            givenPreviousSenderAddress(previousSenderAddressId)
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)
            every { userManager.currentUserId } returns UserId("another")

            // When
            worker.doWork()

            // Then
            withCapturedDraftBody { draftBody ->
                assertEquals(1, draftBody.attachmentKeyPackets.size)
                assertEquals("attachment1", draftBody.attachmentKeyPackets.keys.first())
            }
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
            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(
                    Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true),
                    Attachment("attachment1", keyPackets = "Attachment1KeyPackets", inline = false),
                    Attachment("attachment2", keyPackets = "Attachment2KeyPackets", inline = true)
                )
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(FORWARD)
            givenPreviousSenderAddress("")
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            val expectedKeyPackets = mapOf(
                "attachment" to "OriginalAttachmentPackets",
                "attachment1" to "Attachment1KeyPackets",
                "attachment2" to "Attachment2KeyPackets"
            )
            withCapturedDraftBody { draftBody ->
                assertEquals(expectedKeyPackets, draftBody.attachmentKeyPackets)
            }
        }
    }

    @Test
    fun workerFetchesParentMessageFromNetworkWhenNotFoundInDatabase() {
        // This case happens when the action to forward / reply to a message (parent message)
        // is being started from "search" list, as the sending process doesn't take searchDB into account
        // as it's deprecated and scheduled for removal.
        runBlockingTest {
            // Given
            val parentId = "89346"
            val messageDbId = 346L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "17575c29-c3d9-4f3a-9188-02dea1321cc7"
                addressID = "addressId836"
                messageBody = "messageBody"
            }
            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(
                    Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true),
                    Attachment("attachment1", keyPackets = "Attachment1KeyPackets", inline = false),
                    Attachment("attachment2", keyPackets = "Attachment2KeyPackets", inline = true)
                )
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(FORWARD)
            givenPreviousSenderAddress("")
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(null)
            every { apiManager.fetchMessageDetailsBlocking(parentId) } returns mockk {
                every { this@mockk.message } returns parentMessage
            }

            // When
            worker.doWork()

            // Then
            coVerify { apiManager.fetchMessageDetailsBlocking(parentId) }
            val expectedKeyPackets = mapOf(
                "attachment" to "OriginalAttachmentPackets",
                "attachment1" to "Attachment1KeyPackets",
                "attachment2" to "Attachment2KeyPackets"
            )
            withCapturedDraftBody { draftBody ->
                assertEquals(expectedKeyPackets, draftBody.attachmentKeyPackets)
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
            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(
                    Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true),
                    Attachment("attachment1", keyPackets = "Attachment1KeyPackets", inline = false),
                    Attachment("attachment2", keyPackets = "Attachment2KeyPackets", inline = true)
                )
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(REPLY)
            givenPreviousSenderAddress("")
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            val expectedKeyPackets = mapOf(
                "attachment" to "OriginalAttachmentPackets",
                "attachment2" to "Attachment2KeyPackets"
            )
            withCapturedDraftBody { draftBody ->
                assertEquals(expectedKeyPackets, draftBody.attachmentKeyPackets)
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            withCapturedDraftBody { draftBody ->
                assertEquals(0, draftBody.attachmentKeyPackets.size)
            }
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

            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(FORWARD)
            givenPreviousSenderAddress("")
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            worker.doWork()

            // Then
            withCapturedDraftBody { draftBody ->
                assertNull(draftBody.parentId)
                assertEquals(0, draftBody.attachmentKeyPackets.size)
            }
        }
    }

    @Test fun workerAddsExistingMessageAttachmentsKeyPacketsToRequestWhenSenderChangedAndPacketsWereAlreadyEncryptedWithTheCurrentSender() {
        // This case will happen when changing sender and then performing multiple draft updates (eg. through auto save)
        runBlockingTest {
            // Given
            val parentId = "8238"
            val messageDbId = 1275L
            val messageId = "remote-message-ID-draft-being-updated8723"
            val messageAttachments = listOf(
                Attachment("attachment1", keyPackets = "MessageAtta1KeyPacketsBase64", inline = false),
            )
            val message = mockk<Message>(relaxed = true) {
                coEvery { this@mockk.attachments } returns messageAttachments
                every { dbId } returns messageDbId
                every { this@mockk.messageId } returns messageId
                every { addressID } returns "currentAddressId12346"
                every { messageBody } returns "messageBody"
            }
            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            val previousSenderAddressId = "senderAddressUsedInFirstDraftCreation1"
            givenPreviousSenderAddress(previousSenderAddressId)
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            coEvery { messageDetailsRepository.findAttachmentsByMessageId(messageId) } returns messageAttachments
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            every { userManager.currentUserId } returns testUserId
            every { userManager.requireCurrentUserId() } returns testUserId
            every { addressCryptoFactory.create(testUserId, AddressId(previousSenderAddressId)) } returns
                mockk(relaxed = true) {
                    every { decryptKeyPacket(any()) } throws Exception("Decryption failed")
                }

            // When
            worker.doWork()

            // Then
            coVerify { messageDetailsRepository.findAttachmentsByMessageId(messageId) }
            val expectedKeyPackets = mapOf(
                "attachment1" to "MessageAtta1KeyPacketsBase64"
            )
            withCapturedDraftBody { draftBody ->
                assertEquals(expectedKeyPackets, draftBody.attachmentKeyPackets)
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
                coEvery { this@mockk.attachments } returns messageAttachments
                every { dbId } returns messageDbId
                every { this@mockk.messageId } returns messageId
                every { addressID } returns "currentAddressId12345"
                every { messageBody } returns "messageBody"
            }
            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            val previousSenderAddressId = "senderAddressUsedInFirstDraftCreation"
            givenPreviousSenderAddress(previousSenderAddressId)
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            coEvery { messageDetailsRepository.findAttachmentsByMessageId(messageId) } returns messageAttachments
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            every { userManager.currentUserId } returns testUserId
            every { userManager.requireCurrentUserId() } returns testUserId

            every { base64.decode("MessageAtta1KeyPacketsBase64") } returns
                "MessageAtta1KeyPackets".toByteArray()
            every { base64.decode("MessageAtta2KeyPacketsBase64") } returns
                "MessageAtta2KeyPackets".toByteArray()
            every { base64.decode("MessageAtta3KeyPacketsBase64") } returns
                "MessageAtta3KeyPackets".toByteArray()

            every { addressCryptoFactory.create(testUserId, AddressId(previousSenderAddressId)) } returns
                mockk(relaxed = true) {
                    every { decryptKeyPacket("MessageAtta1KeyPackets".toByteArray()) } returns
                        "decryptedKeyPackets1".toByteArray()
                    every { decryptKeyPacket("MessageAtta2KeyPackets".toByteArray()) } returns
                        "decryptedKeyPackets2".toByteArray()
                    every { decryptKeyPacket("MessageAtta3KeyPackets".toByteArray()) } returns
                        "decryptedKeyPackets3".toByteArray()

                    every { encryptKeyPacket("decryptedKeyPackets1".toByteArray(), any()) } returns
                        "MessageAtt1ReEncryptedPackets".toByteArray()
                    every { encryptKeyPacket("decryptedKeyPackets2".toByteArray(), any()) } returns
                        "MessageAtt2ReEncryptedPackets".toByteArray()
                    every { encryptKeyPacket("decryptedKeyPackets3".toByteArray(), any()) } returns
                        "MessageAtt3ReEncryptedPackets".toByteArray()
                }
            every { base64.encode("MessageAtt1ReEncryptedPackets".toByteArray()) } returns
                "MessageAtt1ReEncryptedPacketsBase64"
            every { base64.encode("MessageAtt2ReEncryptedPackets".toByteArray()) } returns
                "MessageAtt2ReEncryptedPacketsBase64"
            every { base64.encode("MessageAtt3ReEncryptedPackets".toByteArray()) } returns
                "MessageAtt3ReEncryptedPacketsBase64"

            // When
            worker.doWork()

            // Then
            coVerify { messageDetailsRepository.findAttachmentsByMessageId(messageId) }
            val expectedKeyPackets = mapOf(
                "attachment1" to "MessageAtt1ReEncryptedPacketsBase64",
                "attachment2" to "MessageAtt2ReEncryptedPacketsBase64",
                "attachment3" to "MessageAtt3ReEncryptedPacketsBase64"
            )
            withCapturedDraftBody { draftBody ->
                assertEquals(expectedKeyPackets, draftBody.attachmentKeyPackets)
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
                coEvery { this@mockk.attachments } returns messageAttachments
                every { dbId } returns messageDbId
                every { this@mockk.messageId } returns messageId
                every { addressID } returns "addressId12345"
                every { messageBody } returns "messageBody"
            }
            val apiDraftMessage = DraftBody(message = mockk(relaxed = true))
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenPreviousSenderAddress("")
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            coEvery { messageDetailsRepository.findAttachmentsByMessageId(messageId) } returns messageAttachments
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            // When
            worker.doWork()

            // Then
            coVerify { messageDetailsRepository.findAttachmentsByMessageId(messageId) }
            val expectedKeyPackets = mapOf(
                "attachment" to "OriginalAttachmentPackets",
                "attachment1" to "Attachment1KeyPackets",
                "attachment2" to "Attachment2KeyPackets"
            )
            withCapturedDraftBody { draftBody ->
                assertEquals(expectedKeyPackets, draftBody.attachmentKeyPackets)
            }
        }
    }

    @Test
    fun workerPerformsCreateDraftRequestAndBuildsMessageFromResponseWhenRequestSucceeds() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val messageDbId = 345L
            val expirationTime = 1234567L
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
                this.expirationTime = expirationTime
            }

            val apiDraftRequest = DraftBody(message = testMessagePayload)
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns apiDraftRequest
            coEvery { apiManager.createDraft(any()) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            val result = worker.doWork()

            // Then
            assertIs<ListenableWorker.Result.Success>(result)
            coVerify { apiManager.createDraft(any()) }
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
                this.expirationTime = message.expirationTime
                this.messageBody = message.messageBody
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

            val apiDraftRequest = DraftBody(message = testMessagePayload)
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns apiDraftRequest
            coEvery { apiManager.createDraft(any()) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)

            // When
            val result = worker.doWork()

            // Then
            val expected = ListenableWorker.Result.success(
                Data.Builder().putString(KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID, "response_message_id").build()
            )
            assertEquals(expected, result)
            coVerify { messageDetailsRepository.saveMessage(responseMessage) }
            assertEquals(message.dbId, responseMessage.dbId)
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns
                DraftBody(message = testMessagePayload.copy(subject = "Subject002"))
            coEvery { apiManager.createDraft(any()) } returns errorAPIResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject001"
            }
            coEvery { apiManager.createDraft(any()) } throws IOException(errorMessage)
            every { parameters.runAttemptCount } returns 0
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns
                DraftBody(message = testMessagePayload.copy(subject = "Subject001"))
            coEvery { apiManager.createDraft(any()) } throws IOException(errorMessage)
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageById(parentId) } returns flowOf(parentMessage)
            every { parameters.runAttemptCount } returns 4

            // When
            val result = worker.doWork()

            // Then
            verify { userNotifier.showPersistentError(null, "Subject001") }
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

            val apiDraftRequest = DraftBody(message = testMessagePayload)
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns apiDraftRequest
            coEvery { apiManager.updateDraft(remoteMessageId, any(), retrofitTag) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            val result = worker.doWork()

            // Then
            assertIs<ListenableWorker.Result.Success>(result)
            coVerify { apiManager.updateDraft(remoteMessageId, any(), retrofitTag) }
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
                this.messageBody = message.messageBody
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

            val apiDraftRequest = DraftBody(message = mockk(relaxed = true))
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
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns localMessage
            every { messageFactory.createDraftApiRequest(localMessage) } returns apiDraftRequest
            coEvery { apiManager.updateDraft(remoteMessageId, any(), retrofitTag) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { this@mockk.attachments } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            val result = worker.doWork()

            // Then
            assertIs<ListenableWorker.Result.Success>(result)
            coVerify { apiManager.updateDraft(remoteMessageId, any(), retrofitTag) }
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
                this.messageBody = localMessage.messageBody
            }
            val actualMessage = slot<Message>()
            coVerify { messageDetailsRepository.saveMessage(capture(actualMessage)) }
            assertEquals(expectedMessage, actualMessage.captured)
            assertEquals(expectedMessage.attachments, actualMessage.captured.attachments)
        }
    }

    @Test
    fun failsIfMessageHasNullMessageId() = runBlockingTest {
        val message = Message().apply {
            addressID = "addressId"
        }
        coEvery { messageRepository.getMessage(any(), messageDatabaseId = any()) } returns message
        val expectedErrorData = workDataOf(
            Pair(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM,
                CreateDraftWorkerErrors.MessageHasNullId.name
            )
        )
        val expectedResult = ListenableWorker.Result.failure(expectedErrorData)

        // when
        val result = worker.doWork()

        // then
        assertEquals(expectedResult, result)
    }

    @Test
    fun failsIfMessageHasNullBody() = runBlockingTest {
        // given
        val message = Message().apply {
            messageId = "messageId"
            addressID = "addressId"
        }
        coEvery { messageRepository.getMessage(any(), messageDatabaseId = any()) } returns message
        val expectedErrorData = workDataOf(
            Pair(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM,
                CreateDraftWorkerErrors.MessageHasNullBody.name
            )
        )
        val expectedResult = ListenableWorker.Result.failure(expectedErrorData)

        // when
        val result = worker.doWork()

        // then
        assertEquals(expectedResult, result)
    }

    @Test
    fun failsIfMessageHasBlankBody() = runBlockingTest {
        // given
        val message = Message().apply {
            messageId = "messageId"
            addressID = "addressId"
            messageBody = "  "
        }
        coEvery { messageRepository.getMessage(any(), messageDatabaseId = any()) } returns message
        val expectedErrorData = workDataOf(
            Pair(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM,
                CreateDraftWorkerErrors.MessageHasBlankBody.name
            )
        )
        val expectedResult = ListenableWorker.Result.failure(expectedErrorData)

        // when
        val result = worker.doWork()

        // then
        assertEquals(expectedResult, result)
    }

    /**
     * Asserts that [actual] [`is`] [EXPECTED].
     * Ignored if [EXPECTED] is nullable and [actual] is null
     */
    inline fun <reified EXPECTED> assertIs(actual: Any?, crossinline lazyMessage: () -> String? = { null }) {
        // If `EXPECTED` is not nullable, assert that `actual` is not null
        if (null !is EXPECTED) assertNotNull(actual, lazyMessage())

        if (null !is EXPECTED || actual != null) {
            assertTrue(actual is EXPECTED) {
                // Usage of unsafe operator `!!` since if `EXPECTED` is not nullable, we already assert
                // that `actual` is not null
                lazyMessage()?.let { "\n$it" }.orEmpty() +
                    "Expected to be '${EXPECTED::class.qualifiedName}'. " +
                    "Actual: '${actual!!::class.qualifiedName}'"
            }
        }
    }

    private inline fun withCapturedDraftBody(block: (DraftBody) -> Unit) {
        val draftBodySlot = slot<DraftBody>()
        try {
            coVerify { apiManager.createDraft(capture(draftBodySlot)) }
        } catch (ignored: AssertionError) {
            coVerify { apiManager.updateDraft(any(), capture(draftBodySlot), any()) }
        }
        block(draftBodySlot.captured)
    }

    @Test
    fun workerFailsReturningMessageAlreadySentErrorWhenApiResponseIs422WithMessageAlreadySentErrorCode() {
        runBlockingTest {
            // Given
            val messageDbId = 346L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "remoteMessageIdA71237=="
                addressID = "addressId836"
                messageBody = "messageBody"
                subject = "Subject002"
            }
            val unprocessableEntityException = HttpException(
                Response.error<String>(
                    RESPONSE_CODE_UNPROCESSABLE_ENTITY,
                    """{ "Code": 15034, "Error": "Message has already been sent" }""".toResponseBody(
                        "text/json".toMediaTypeOrNull()
                    )
                )
            )
            givenMessageIdInput(messageDbId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject002"
            }
            coEvery { apiManager.updateDraft("remoteMessageIdA71237==", any(), any()) } throws unprocessableEntityException
            every { parameters.runAttemptCount } returns 0

            // When
            val result = worker.doWork()

            // Then
            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(
                    KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM,
                    CreateDraftWorkerErrors.MessageAlreadySent.name
                ).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun `worker fails returning InvalidSender sent error when api response is 422 with InvalidSender error code`() {
        runBlockingTest {
            // Given
            val messageDbId = 346L
            val message = Message().apply {
                dbId = messageDbId
                messageId = "remoteMessageIdA71237=="
                addressID = "addressId836"
                messageBody = "messageBody"
                subject = "Subject003"
            }
            val unprocessableEntityException = HttpException(
                Response.error<String>(
                    RESPONSE_CODE_UNPROCESSABLE_ENTITY,
                    """{ "Code": 2001, "Error": "Invalid sender" }""".toResponseBody(
                        "text/json".toMediaTypeOrNull()
                    )
                )
            )
            givenMessageIdInput(messageDbId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            coEvery { messageRepository.getMessage(any(), messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject003"
            }
            coEvery {
                apiManager.updateDraft(
                    "remoteMessageIdA71237==", any(), any()
                )
            } throws unprocessableEntityException

            // When
            val result = worker.doWork()

            // Then
            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(
                    KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM,
                    CreateDraftWorkerErrors.InvalidSender.name
                ).build()
            )
            assertEquals(expectedFailure, result)
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
