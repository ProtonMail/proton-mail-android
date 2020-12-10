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
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.NewMessage
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.messages.MessageSender
import ch.protonmail.android.api.utils.Fields.Message.SELF
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.NONE
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
import ch.protonmail.android.utils.extensions.serialize
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CreateDraftWorkerTest : CoroutinesTest {

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

    @MockK
    private lateinit var base64: Base64Encoder

    @InjectMockKs
    private lateinit var worker: CreateDraftWorker

    @Test
    fun workerEnqueuerCreatesOneTimeRequestWorkerWithParams() {
        runBlockingTest {
            // Given
            val messageParentId = "98234"
            val messageLocalId = "2834"
            val messageDbId = 534L
            val messageActionType = REPLY_ALL
            val message = Message(messageLocalId)
            message.dbId = messageDbId
            val previousSenderAddressId = "previousSenderId82348"
            val requestSlot = slot<OneTimeWorkRequest>()
            every { workManager.enqueue(capture(requestSlot)) } answers { mockk() }

            // When
            CreateDraftWorker.Enqueuer(workManager).enqueue(
                message,
                messageParentId,
                messageActionType,
                previousSenderAddressId
            )

            // Then
            val constraints = requestSlot.captured.workSpec.constraints
            val inputData = requestSlot.captured.workSpec.input
            val actualMessageDbId = inputData.getLong(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID, -1)
            val actualMessageLocalId = inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_LOCAL_ID)
            val actualMessageParentId = inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID)
            val actualMessageActionType = inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_ACTION_TYPE_SERIALIZED)
            val actualPreviousSenderAddress = inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_PREVIOUS_SENDER_ADDRESS_ID)
            assertEquals(message.dbId, actualMessageDbId)
            assertEquals(message.messageId, actualMessageLocalId)
            assertEquals(messageParentId, actualMessageParentId)
            assertEquals(messageActionType.serialize(), actualMessageActionType)
            assertEquals(previousSenderAddressId, actualPreviousSenderAddress)
            assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
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
            val error = CreateDraftWorker.CreateDraftWorkerErrors.MessageNotFound
            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_ERROR_ENUM, error.name).build()
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
                addressID = "addressId"
                messageBody = "messageBody"
            }
            val apiDraftMessage = mockk<NewMessage>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(actionType)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            // When
            worker.doWork()

            // Then
            verify { apiDraftMessage.setParentID(parentId) }
            verify { apiDraftMessage.action = 2 }
            // Always get parent message from messageDetailsDB, never from searchDB
            // ignoring isTransient property as the values in the two DB appears to be the same
            verify { messageDetailsRepository.findMessageById(parentId) }
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
                addressID = addressId
                messageBody = "messageBody"
            }
            val apiDraftMessage = mockk<NewMessage>(relaxed = true)
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

            // When
            worker.doWork()

            // Then
            val messageSender = MessageSender("senderName", "sender@email.it")
            verify { apiDraftMessage.setSender(messageSender) }
            verify { apiDraftMessage.addMessageBody(SELF, "messageBody") }
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
                addressID = addressId
                messageBody = "messageBody2341"
                sender = MessageSender("sender by alias", "sender+alias@pm.me")
            }
            val apiDraftMessage = mockk<NewMessage>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenActionTypeInput()
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            // When
            worker.doWork()

            // Then
            val messageSender = MessageSender("sender by alias", "sender+alias@pm.me")
            verify { apiDraftMessage.setSender(messageSender) }
        }
    }

    @Test
    fun workerReEncryptParentAttachmentsWhenSenderAddressChanged() {
        runBlockingTest {
            // Given
            val parentId = "89345"
            val actionType = FORWARD
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
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

            val apiDraftMessage = mockk<NewMessage>(relaxed = true)
            val parentMessage = mockk<Message> {
                every { attachments(any()) } returns listOf(attachment)
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
            every { messageDetailsRepository.findMessageById(parentId) } returns parentMessage
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
            verify { parentMessage.attachments(messageDetailsRepository.databaseProvider.provideMessagesDao()) }
            verify { addressCrypto.buildArmoredPublicKey(privateKey) }
            verify { apiDraftMessage.addAttachmentKeyPacket("attachment1", attachmentReEncrypted.keyPackets) }
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
                addressID = "addressId835"
                messageBody = "messageBody"
            }
            val apiDraftMessage = mockk<NewMessage>(relaxed = true)
            val parentMessage = mockk<Message> {
                every { attachments(any()) } returns listOf(
                    Attachment("attachment", keyPackets = "OriginalAttachmentPackets", inline = true),
                    Attachment("attachment1", keyPackets = "Attachment1KeyPackets", inline = false),
                    Attachment("attachment2", keyPackets = "Attachment2KeyPackets", inline = true)
                )
            }
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(REPLY_ALL)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }
            every { messageDetailsRepository.findMessageById(parentId) } returns parentMessage

            // When
            worker.doWork()

            // Then
            verify { parentMessage.attachments(messageDetailsRepository.databaseProvider.provideMessagesDao()) }
            verifyOrder {
                apiDraftMessage.addAttachmentKeyPacket("attachment", "OriginalAttachmentPackets")
                apiDraftMessage.addAttachmentKeyPacket("attachment2", "Attachment2KeyPackets")
            }

            // TODO figure if we actually need this (regarding inline attachments, discussing with the team)
            verify(exactly = 0) { apiDraftMessage.addAttachmentKeyPacket("attachment1", "Attachment1KeyPackets") }
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
                addressID = "addressId835"
                messageBody = "messageBody"
            }

            val apiDraftMessage = mockk<NewMessage>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            givenActionTypeInput(NONE)
            givenPreviousSenderAddress("")
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { messageFactory.createDraftApiRequest(message) } answers { apiDraftMessage }

            // When
            worker.doWork()

            // Then
            verify(exactly = 0) { apiDraftMessage.addAttachmentKeyPacket(any(), any()) }
        }
    }

    private fun givenPreviousSenderAddress(address: String) {
        every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_PREVIOUS_SENDER_ADDRESS_ID) } answers { address }
    }

    private fun givenActionTypeInput(actionType: Constants.MessageActionType = NONE) {
        every {
            parameters.inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_ACTION_TYPE_SERIALIZED)
        } answers {
            actionType.serialize()
        }
    }

    private fun givenParentIdInput(parentId: String) {
        every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID) } answers { parentId }
    }

    private fun givenMessageIdInput(messageDbId: Long) {
        every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID, -1) } answers { messageDbId }
    }
}
