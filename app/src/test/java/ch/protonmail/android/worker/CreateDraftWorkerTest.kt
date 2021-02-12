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

import android.content.Context
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
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.messages.ParsedHeaders
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessageSender
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.NONE
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.utils.base64.Base64Encoder
import ch.protonmail.android.utils.notifier.ErrorNotifier
import ch.protonmail.android.worker.drafts.CreateDraftWorker
import ch.protonmail.android.worker.drafts.CreateDraftWorkerErrors
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_ACTION_TYPE
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_MSG_DB_ID
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_MSG_LOCAL_ID
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_MSG_PARENT_ID
import ch.protonmail.android.worker.drafts.KEY_INPUT_SAVE_DRAFT_PREV_SENDER_ADDR_ID
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test

class CreateDraftWorkerTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var errorNotifier: ErrorNotifier

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var messageFactory: MessageFactory

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var addressCryptoFactory: AddressCrypto.Factory

    @RelaxedMockK
    private lateinit var base64: Base64Encoder

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var pendingActionsDao: PendingActionsDao

    @InjectMockKs
    private lateinit var worker: CreateDraftWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { apiManager.createDraft(any()) } returns mockk(relaxed = true)
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
            val requestSlot = slot<OneTimeWorkRequest>()
            every {
                workManager.enqueueUniqueWork(messageId, ExistingWorkPolicy.REPLACE, capture(requestSlot))
            } answers { mockk() }

            // When
            CreateDraftWorker.Enqueuer(workManager).enqueue(
                message,
                messageParentId,
                messageActionType,
                previousSenderAddressId
            )

            // Then
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns null

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
    fun workerSetsParentIdAndActionTypeOnCreateDraftRequestWhenParentIdIsGiven() {
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            worker.doWork()

            // Then
            verify { apiDraftMessage.parentID = parentId }
            verify { apiDraftMessage.action = 2 }
            // Always get parent message from messageDetailsDB, never from searchDB
            // ignoring isTransient property as the values in the two DB appears to be the same
            verify { messageDetailsRepository.findMessageByIdBlocking(parentId) }
        }
    }

    @Test
    fun workerSetsSenderAndMessageBodyOnCreateDraftRequest() {
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
                true,
                Address.Type.ORIGINAL,
                allowedToSend = true,
                allowedToReceive = false,
                keys = AddressKeys(null, emptyList())
            )
            givenMessageIdInput(messageDbId)
            givenActionTypeInput()
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { userManager.username } returns "username"
            every { userManager.getUser("username").loadNew("username") } returns mockk {
                every { findAddressById(Id(addressId)) } returns address
            }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking("") } returns parentMessage

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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking("") } returns parentMessage

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
            val username = "username934"
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage
            every { userManager.username } returns username
            every { userManager.getUser(username).loadNew(username) } returns mockk {
                every { findAddressById(Id("addressId835")) } returns senderAddress
            }
            every { addressCryptoFactory.create(Id(previousSenderAddressId), Name(username)) } returns addressCrypto
            every { addressCrypto.buildArmoredPublicKey(privateKey) } returns senderPublicKey
            every { base64.decode(attachment.keyPackets!!) } returns decodedPacketsBytes
            every { base64.encode(encryptedKeyPackets) } returns "encrypted encoded packets"

            // When
            worker.doWork()

            // Then
            val attachmentReEncrypted = attachment.copy(keyPackets = "encrypted encoded packets")
            coVerify { parentMessage.attachments(messageDetailsRepository.databaseProvider.provideMessagesDao()) }
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage
            every { userManager.username } returns "username93w"

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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            worker.doWork()

            // Then
            coVerify { parentMessage.attachments(messageDetailsRepository.databaseProvider.provideMessagesDao()) }
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            worker.doWork()

            // Then
            coVerify { parentMessage.attachments(messageDetailsRepository.databaseProvider.provideMessagesDao()) }
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            worker.doWork()

            // Then
            verify(exactly = 0) { apiDraftMessage.addAttachmentKeyPacket(any(), any()) }
        }
    }

    @Test
    fun workerPerformsCreateDraftRequestAndBuildsMessageFromResponseWhenSucceding() {
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
                Attachments = listOf(Attachment("235423"), Attachment("823421"))
            }

            val apiDraftRequest = mockk<DraftBody>(relaxed = true)
            val responseMessage = Message(messageId = "created_draft_id")
            val apiDraftResponse = mockk<MessageResponse> {
                every { code } returns 1000
                every { messageId } returns "created_draft_id"
                every { this@mockk.message } returns responseMessage
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns apiDraftRequest
            coEvery { apiManager.createDraft(apiDraftRequest) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

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
                this.Attachments = message.Attachments
                this.localId = message.messageId
            }
            val actualMessage = slot<Message>()
            coVerify { messageDetailsRepository.saveMessageLocally(capture(actualMessage)) }
            assertEquals(expected, actualMessage.captured)
            assertEquals(expected.Attachments, actualMessage.captured.Attachments)
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns apiDraftRequest
            coEvery { apiManager.createDraft(apiDraftRequest) } returns apiDraftResponse
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            val result = worker.doWork()

            // Then
            coVerify { messageDetailsRepository.saveMessageLocally(responseMessage) }
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject002"
            }
            coEvery { apiManager.createDraft(any()) } returns errorAPIResponse
            every { parameters.runAttemptCount } returns 0
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            val result = worker.doWork()

            verify { errorNotifier.showPersistentError(errorMessage, "Subject002") }
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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject001"
            }
            coEvery { apiManager.createDraft(any()) } throws IOException(errorMessage)
            every { parameters.runAttemptCount } returns 3
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

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
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } returns mockk(relaxed = true) {
                every { this@mockk.message.subject } returns "Subject001"
            }
            coEvery { apiManager.createDraft(any()) } throws IOException(errorMessage)
            every { parameters.runAttemptCount } returns 11
            val attachment = Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true)
            val parentMessage = mockk<Message> {
                coEvery { attachments(any()) } returns listOf(attachment)
            }
            every { messageDetailsRepository.findMessageByIdBlocking(parentId) } returns parentMessage

            // When
            val result = worker.doWork()

            // Then
            verify { errorNotifier.showPersistentError(errorMessage, "Subject001") }
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
    fun workerPerformsUpdateDraftRequestAndStoresResponseMessageInDbWhenMessageIsNotLocal() {
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
                Attachments = listOf(Attachment(attachmentId = "82374"))
            }

            val apiDraftRequest = mockk<DraftBody>(relaxed = true)
            val responseMessage = Message(messageId = "created_draft_id")
            val apiDraftResponse = mockk<MessageResponse> {
                every { code } returns 1000
                every { messageId } returns "created_draft_id"
                every { this@mockk.message } returns responseMessage
            }
            val retrofitTag = RetrofitTag(userManager.username)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
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
                this.Attachments = message.Attachments
                this.localId = message.messageId
            }
            val actualMessage = slot<Message>()
            coVerify { messageDetailsRepository.saveMessageLocally(capture(actualMessage)) }
            assertEquals(expectedMessage, actualMessage.captured)
            assertEquals(expectedMessage.Attachments, actualMessage.captured.Attachments)
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
