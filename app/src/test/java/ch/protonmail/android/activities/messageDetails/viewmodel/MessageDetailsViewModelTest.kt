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

package ch.protonmail.android.activities.messageDetails.viewmodel

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import ch.protonmail.android.activities.messageDetails.MessageRenderer
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.attachments.AttachmentsHelper
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.LabelRepository
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.details.data.toConversationUiModel
import ch.protonmail.android.details.presentation.MessageDetailsActivity.Companion.EXTRA_MAILBOX_LABEL_ID
import ch.protonmail.android.details.presentation.MessageDetailsActivity.Companion.EXTRA_MESSAGE_LOCATION_ID
import ch.protonmail.android.details.presentation.MessageDetailsActivity.Companion.EXTRA_MESSAGE_OR_CONVERSATION_ID
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.domain.entity.LabelId
import ch.protonmail.android.details.presentation.model.MessageBodyState
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import ch.protonmail.android.utils.DownloadUtils
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.EMPTY_STRING
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val INPUT_ITEM_DETAIL_ID = "inputMessageOrConversationId"
private const val MESSAGE_SENDER_EMAIL_ADDRESS = "sender@protonmail.com"

class MessageDetailsViewModelTest : ArchTest, CoroutinesTest {

    private val changeConversationsReadStatus: ChangeConversationsReadStatus = mockk(relaxed = true)

    private val changeConversationsStarredStatus: ChangeConversationsStarredStatus = mockk(relaxed = true)

    private val messageDetailsRepository: MessageDetailsRepository = mockk(relaxed = true)

    private val observeConversationFlow = MutableSharedFlow<DataResult<Conversation>>(
        replay = 1, onBufferOverflow = BufferOverflow.SUSPEND
    )
    private val conversationRepository: ConversationsRepository = mockk {
        every { getConversation(any(), any()) } returns observeConversationFlow
    }

    private val observeMessageFlow = MutableSharedFlow<Message?>(replay = 1, onBufferOverflow = BufferOverflow.SUSPEND)
    private val messageRepository: MessageRepository = mockk {
        every { observeMessage(any(), any()) } returns observeMessageFlow
    }

    private val labelRepository: LabelRepository = mockk(relaxed = true)

    private val testSenderContactEmail = ContactEmail(
        "defaultMockContactEmailId", "defaultMockContactEmailAddress", "defaultMockContactName"
    )
    private val contactsRepository: ContactsRepository = mockk(relaxed = true) {
        coEvery { findContactEmailByEmail(any()) } returns testSenderContactEmail
    }

    private val attachmentsHelper: AttachmentsHelper = mockk(relaxed = true)

    private val attachmentMetadataDao: AttachmentMetadataDao = mockk(relaxed = true)

    private val moveMessagesToFolder: MoveMessagesToFolder = mockk(relaxed = true)

    private val moveConversationsToFolder: MoveConversationsToFolder = mockk(relaxed = true)

    private val fetchVerificationKeys: FetchVerificationKeys = mockk(relaxed = true)

    private val verifyConnection: VerifyConnection = mockk(relaxed = true)

    private val networkConfigurator: NetworkConfigurator = mockk(relaxed = true)

    private val attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer = mockk(relaxed = true)

    private val conversationModeEnabled: ConversationModeEnabled = mockk {
        every { this@mockk.invoke(any()) } returns false
    }

    private val deleteConversations: DeleteConversations = mockk()

    private val deleteMessage: DeleteMessage = mockk()

    private val savedStateHandle = mockk<SavedStateHandle> {
        every { get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns INPUT_ITEM_DETAIL_ID
        every { get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns INBOX.messageLocationTypeValue
    }

    private val testId1 = UserId("userId1")
    private val testId2 = UserId("userId2")
    private val testUserId1 = UserId(testId1.id)
    private val testUserId2 = UserId(testId2.id)
    private val userIdFlow = MutableStateFlow(testUserId1)

    private val userManager: UserManager = mockk(relaxed = true) {
        every { requireCurrentUserId() } returns testId1
        coEvery { primaryUserId } returns userIdFlow
    }

    private var messageRendererFactory = mockk<MessageRenderer.Factory> {
        every { create(any()) } returns mockk(relaxed = true) {
            every { renderedMessage } returns Channel()
        }
    }

    private val messageSender = MessageSender("senderName", MESSAGE_SENDER_EMAIL_ADDRESS)

    private lateinit var viewModel: MessageDetailsViewModel

    @BeforeTest
    fun setUp() {
        viewModel = MessageDetailsViewModel(
            messageDetailsRepository,
            messageRepository,
            userManager,
            contactsRepository,
            labelRepository,
            attachmentMetadataDao,
            fetchVerificationKeys,
            attachmentsWorker,
            dispatchers,
            attachmentsHelper,
            DownloadUtils(),
            moveMessagesToFolder,
            moveConversationsToFolder,
            conversationModeEnabled,
            conversationRepository,
            changeConversationsReadStatus,
            changeConversationsStarredStatus,
            deleteMessage,
            deleteConversations,
            savedStateHandle,
            messageRendererFactory,
            verifyConnection,
            networkConfigurator,
        )
    }

    @Test
    fun loadMailboxItemGetsMessageFromServerWithMessageIdAndUserIdWhenWeDontHaveTheMessageInDb() = runBlockingTest {

        // given
        every { userManager.requireCurrentUserId() } returns testId1
        val messageOrConversationId = INPUT_ITEM_DETAIL_ID
        val downLoadedMessage = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = true,
            sender = messageSender
        )
        coEvery { messageRepository.getMessage(testId1, messageOrConversationId, true) } returns downLoadedMessage
        val expected = ConversationUiModel(
            false,
            null,
            listOf(downLoadedMessage),
            null
        )

        // when
        observeMessageFlow.tryEmit(null)
        val actualItem = viewModel.conversationUiModel.take(1).toList()[0]

        // then
        assertEquals(expected, actualItem)
    }

    @Test
    fun verifyThatMessageIsParsedProperly() {
        // given
        val decryptedMessageContent = "decrypted message content"
        val decryptedMessage = Message(messageId = "messageId1").apply {
            decryptedHTML = decryptedMessageContent
        }
        val windowWidth = 500
        val defaultErrorMessage = "errorHappened"
        val cssContent = "css"
        val expected =
            "<html>\n <head>\n  <style>$cssContent</style>\n  <meta name=\"viewport\" content=\"width=$windowWidth, maximum-scale=2\"> \n </head>\n <body>\n  <div id=\"pm-body\" class=\"inbox-body\">   $decryptedMessageContent  \n  </div>\n </body>\n</html>"

        // when
        val parsedMessage =
            viewModel.formatMessageHtmlBody(decryptedMessage, windowWidth, cssContent, defaultErrorMessage)

        // then
        assertEquals(expected, parsedMessage)
    }

    @Test
    fun verifyThatDecryptedMessageHtmlIsEmittedThroughDecryptedConversationUiModel() {
        // given
        val decryptedMessageContent = "decrypted message content"
        // The ID of this message matches the ID of the "fake" message created by `buildMessageDomainModel` method
        val decryptedMessage = Message(messageId = "messageId4").apply {
            decryptedHTML = decryptedMessageContent
        }
        val decryptedConversationObserver = viewModel.decryptedConversationUiModel.testObserver()
        val cssContent = "css"
        val windowWidth = 500

        val conversationId = UUID.randomUUID().toString()
        val conversationMessage = mockk<Message>(relaxed = true)
        every { conversationMessage.messageId } returns "messageId4"
        every { conversationMessage.conversationId } returns conversationId
        every { conversationMessage.subject } returns "subject4"
        every { conversationMessage.sender } returns messageSender
        every { conversationMessage.isDownloaded } returns true
        every { conversationMessage.senderEmail } returns MESSAGE_SENDER_EMAIL_ADDRESS
        every { conversationMessage.numAttachments } returns 1
        every { conversationMessage.time } returns 82374730L
        every { conversationMessage.decrypt(any(), any(), any()) } just Runs

        every { conversationModeEnabled.invoke(any()) } returns true
        every { userManager.requireCurrentUserId() } returns testId2
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation(conversationId))
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), "messageId5") } returns null

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.formatMessageHtmlBody(decryptedMessage, windowWidth, cssContent, "errorHappened")

        // then
        val expectedMessageContent =
            "<html>\n <head>\n  <style>$cssContent</style>\n  <meta name=\"viewport\" content=\"width=$windowWidth, maximum-scale=2\"> \n </head>\n <body>\n  <div id=\"pm-body\" class=\"inbox-body\">   $decryptedMessageContent  \n  </div>\n </body>\n</html>"
        verify { conversationMessage setProperty "decryptedHTML" value expectedMessageContent }
        assertEquals(conversationMessage, decryptedConversationObserver.observedValues.last()!!.messages[0])
    }

    @Test
    fun loadMailboxItemInvokesMessageRepositoryWithMessageIdAndUserId() = runBlockingTest {
        // Given
        every { userManager.requireCurrentUserId() } returns testId1
        val message = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = false,
            sender = messageSender
        )
        val downLoadedMessage = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = true,
            sender = messageSender
        )

        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns downLoadedMessage
        val expected = ConversationUiModel(
            false,
            null,
            listOf(downLoadedMessage),
            null
        )

        // When
        viewModel.conversationUiModel.test {
            // Then
            coVerify { messageRepository.observeMessage(testUserId1, INPUT_ITEM_DETAIL_ID) }
            observeMessageFlow.emit(message)
            coVerify { contactsRepository.findContactEmailByEmail(any()) }
            val actualItem = expectItem()
            assertEquals(expected, actualItem)
            assertEquals(testSenderContactEmail.name, actualItem.messages[0].senderDisplayName)
        }
    }

    @Test
    fun shouldLoadMessageWithLabelsWhenLabelsPresent() = runBlockingTest {
        val allLabels = (1..5).map { Label(id = "id$it", name = "name$it", color = "", exclusive = it > 2) }
        val allLabelIds = allLabels.map { LabelId(it.id) }
        every { userManager.requireCurrentUserId() } returns testId1
        val message = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = false,
            sender = messageSender,
            allLabelIDs = allLabelIds.map { it.id }
        )
        val downLoadedMessage = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = true,
            sender = messageSender,
            allLabelIDs = allLabelIds.map { it.id }
        )
        val nonExclusiveLabels = hashMapOf(
            INPUT_ITEM_DETAIL_ID to allLabels.take(2).map {
                LabelChipUiModel(LabelId(it.id), Name(it.name), color = null)
            }
        )
        val exclusiveLabels = hashMapOf(INPUT_ITEM_DETAIL_ID to allLabels.takeLast(3))
        every {
            labelRepository.findLabels(testId1, allLabelIds)
        } returns flowOf(allLabels)
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns downLoadedMessage

        viewModel.conversationUiModel.test {
            observeMessageFlow.emit(message)
            val actualItem = expectItem()
            assertEquals(exclusiveLabels, actualItem.exclusiveLabels)
            assertEquals(nonExclusiveLabels, actualItem.nonExclusiveLabels)
        }
    }

    @Test
    fun shouldLoadMessageWithoutLabelsWhenLabelsNotPresent() = runBlockingTest {
        every { userManager.requireCurrentUserId() } returns testId1
        val message = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = false,
            sender = messageSender,
            allLabelIDs = emptyList()
        )
        val downLoadedMessage = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = true,
            sender = messageSender,
            allLabelIDs = emptyList()
        )
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns downLoadedMessage

        viewModel.conversationUiModel.test {
            observeMessageFlow.emit(message)
            val actualItem = expectItem()
            assertTrue(actualItem.exclusiveLabels.isEmpty())
            assertTrue(actualItem.nonExclusiveLabels.isEmpty())
        }
    }

    @Test
    fun loadMailboxItemInvokesMessageRepositoryWithMessageIdAndUserIdForConversations() = runBlockingTest {
        // Given
        every { conversationModeEnabled.invoke(any()) } returns true
        every { userManager.requireCurrentUserId() } returns testId2
        val conversationId = UUID.randomUUID().toString()
        val testConversation = buildConversation(conversationId)
        val testConversationResult = DataResult.Success(ResponseSource.Local, testConversation)
        every { userManager.requireCurrentUserId() } returns testId1
        val messageId = "messageId4"
        val secondMessageId = "messageId5"
        val downLoadedMessage1 = Message(
            messageId = messageId,
            isDownloaded = false, // this is false as with current converters (.toDbModel()) we loose this information
            sender = messageSender,
            time = 82_374_724L,
            subject = "subject4",
            conversationId = conversationId,
            isReplied = false,
            isRepliedAll = true,
            isForwarded = false,
            numAttachments = 1,
            allLabelIDs = listOf("1", "2")
        )
        val downLoadedMessage2 = Message(
            messageId = secondMessageId,
            isDownloaded = false, // this is false as with current converters (.toDbModel()) we loose this information
            sender = messageSender,
            time = 82_374_724L,
            subject = "subject4",
            conversationId = conversationId,
            isReplied = false,
            isRepliedAll = true,
            isForwarded = false,
            numAttachments = 1,
            allLabelIDs = listOf("1", "2")
        )
        coEvery { messageRepository.findMessage(testId2, messageId) } returns downLoadedMessage1
        coEvery { messageRepository.findMessage(testId2, secondMessageId) } returns downLoadedMessage2

        // When
        viewModel.conversationUiModel.test {
            userIdFlow.emit(testUserId2)
            // Then
            coVerify { conversationRepository.getConversation(UserId(testId2.s), INPUT_ITEM_DETAIL_ID) }
            observeConversationFlow.emit(testConversationResult)
            coVerify { contactsRepository.findContactEmailByEmail(any()) }
            val actualItem = expectItem()
            assertNotNull(actualItem)
            assertEquals(testConversation.toConversationUiModel(), actualItem)
        }
    }

    @Test
    fun loadMessageBodyMarksMessageAsReadAndEmitsItWhenTheMessageWasSuccessfullyDecrypted() = runBlockingTest {
        // Given
        val message = mockk<Message>(relaxed = true)
        every { message.messageId } returns "messageId1"
        every { message.isDownloaded } returns true
        every { message.senderEmail } returns "senderEmail"
        every { message.decryptedHTML } returns null
        every { message.isRead } returns false
        every { message.decrypt(any(), any(), any()) } just Runs
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns message
        coEvery { messageRepository.markRead(any()) } just Runs

        val userId = UserId("userId4")
        every { userManager.requireCurrentUserId() } returns userId

        // When
        val actual = viewModel.loadMessageBody(message).first()
        val expected = MessageBodyState.Success(message)

        // Then
        verify { messageRepository.markRead(listOf("messageId1")) }
        assertEquals(expected, actual)
    }

    @Test
    fun loadMessageBodyDoesMarksMessageAsReadWhenTheMessageDecryptionFails() = runBlockingTest {
        // Given
        val message = mockk<Message>(relaxed = true)
        every { message.messageId } returns "messageId2"
        every { message.isDownloaded } returns true
        every { message.isRead } returns false
        every { message.decryptedHTML } returns null
        every { message.senderEmail } returns "senderEmail"
        every { message.decrypt(any(), any(), any()) } throws Exception("Test - Decryption failed")
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns message
        coEvery { messageRepository.markRead(any()) } just Runs

        // When
        val actual = viewModel.loadMessageBody(message).first()
        val expected = MessageBodyState.Error.DecryptionError(message)

        // Then
        verify { messageRepository.markRead(listOf("messageId2")) }
        assertEquals(expected, actual)
    }

    @Test
    fun loadMessageDoesNotMarkMessageAsReadWhenTheMessageIsAlreadyRead() = runBlockingTest {
        // This prevents the message detail to be refreshed in a loop, caused by the messageFlow to continuously emit
        // the message after it was marked as read (ignoring distinctUntilChanged clause). This is probably due to
        // some mutable property of `Message` class changing unexpectedly.
        // Given
        val message = mockk<Message>(relaxed = true)
        every { message.messageId } returns "messageId3"
        every { message.isDownloaded } returns true
        every { message.isRead } returns true
        every { message.senderEmail } returns "senderEmail"
        every { message.decrypt(any(), any(), any()) } just Runs
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns message

        // When
        viewModel.loadMessageBody(message).test {

            // Then
            verify(exactly = 0) { messageRepository.markRead(any()) }
            assertEquals(MessageBodyState.Success(message), expectItem())
            expectComplete()
        }
    }

    @Test
    fun loadMessageIsMarkedAsReadWhenTheMessageIsUnRead() = runBlockingTest {
        // Given
        val message = mockk<Message>(relaxed = true)
        every { message.messageId } returns "messageId3"
        every { message.isDownloaded } returns true
        every { message.isRead } returns false
        every { message.senderEmail } returns "senderEmail"
        every { message.decrypt(any(), any(), any()) } just Runs
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns message
        coEvery { messageRepository.markRead(any()) } just Runs

        // When
        viewModel.loadMessageBody(message).test {

            // Then
            verify(exactly = 1) { messageRepository.markRead(any()) }
            assertEquals(MessageBodyState.Success(message), expectItem())
            expectComplete()
        }
    }

    @Test
    fun loadMessageDoesNotEmitMessageToLiveDataWhenTheMessageWasNotFound() = runBlockingTest {
        // Given
        every { userManager.requireCurrentUserId() } returns testId1
        val message = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = false,
            sender = messageSender
        )
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns null
        val messageErrorObserver = viewModel.messageDetailsError.testObserver()

        // When
        observeMessageFlow.emit(message)
        assertEquals("Failed getting message details", messageErrorObserver.observedValues[0]?.getContentIfNotHandled())
    }

    @Test
    fun loadMailboxItemEmitsConversationUiItemWithOrderedConversationDataWhenRepositoryReturnsAConversationAndLastMessageDecryptionSucceeds() =
        runBlockingTest {
            // Given
            val conversationObserver = viewModel.decryptedConversationUiModel.testObserver()
            val conversationId = UUID.randomUUID().toString()

            val conversationMessage = mockk<Message>(relaxed = true)
            every { conversationMessage.messageId } returns "messageId4"
            every { conversationMessage.conversationId } returns conversationId
            every { conversationMessage.subject } returns "subject4"
            every { conversationMessage.sender } returns messageSender
            every { conversationMessage.isDownloaded } returns true
            every { conversationMessage.senderEmail } returns MESSAGE_SENDER_EMAIL_ADDRESS
            every { conversationMessage.numAttachments } returns 1
            every { conversationMessage.time } returns 82374730L
            every { conversationMessage.decrypt(any(), any(), any()) } just Runs

            val olderConversationMessage = mockk<Message>(relaxed = true)
            every { olderConversationMessage.messageId } returns "messageId5"
            every { olderConversationMessage.conversationId } returns conversationId
            every { olderConversationMessage.subject } returns "subject5"
            every { olderConversationMessage.sender } returns messageSender
            every { olderConversationMessage.isDownloaded } returns true
            every { olderConversationMessage.senderEmail } returns MESSAGE_SENDER_EMAIL_ADDRESS
            every { olderConversationMessage.numAttachments } returns 0
            every { olderConversationMessage.time } returns 82374724L
            every { olderConversationMessage.decrypt(any(), any(), any()) } just Runs

            every { userManager.requireCurrentUserId() } returns testId2
            coEvery { conversationModeEnabled(any()) } returns true
            val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation(conversationId))
            coEvery { messageRepository.findMessage(testId2, "messageId4") } returns conversationMessage
            coEvery { messageRepository.findMessage(testId2, "messageId5") } returns olderConversationMessage

            // When
            userIdFlow.tryEmit(testUserId2)
            observeConversationFlow.tryEmit(conversationResult)

            // Then
            val conversationUiModel = ConversationUiModel(
                false,
                "Conversation subject",
                listOf(olderConversationMessage, conversationMessage),
                5
            )
            assertEquals(conversationUiModel, conversationObserver.observedValues[0])
        }

    @Test
    fun loadMessageBodyEmitsInputMessageWhenBodyIsAlreadyDecrypted() = runBlockingTest {
        val message = mockk<Message> {
            every { messageId } returns "id1"
        }
        val decryptedMessageHtml = "<html>Decrypted message body HTML</html>"
        every { message.decryptedHTML } returns decryptedMessageHtml

        val decryptedMessage = viewModel.loadMessageBody(message).first()

        assertEquals(decryptedMessageHtml, (decryptedMessage as MessageBodyState.Success).message.decryptedHTML)
    }

    @Test
    fun loadMessageBodyFetchesMessageFromMessageRepositoryWhenInputMessageIsNotDecrypted() = runBlockingTest {
        // Given
        val messageId = "messageId"

        val userId = UserId("userId3")
        every { userManager.requireCurrentUserId() } returns userId

        val message = mockk<Message>()
        every { message.messageId } returns messageId
        every { message.decryptedHTML } returns null
        every { message.isRead } returns false

        val fetchedMessage = mockk<Message>()
        every { fetchedMessage.messageBody } returns "encrypted message body"
        every { fetchedMessage.decrypt(any(), any(), any()) } just Runs
        every { fetchedMessage.isRead } returns true

        coEvery { messageRepository.getMessage(userId, messageId, true) } returns fetchedMessage

        // When
        val decryptedMessage = viewModel.loadMessageBody(message).first()
        val expectedMessage = MessageBodyState.Success(fetchedMessage)

        // Then
        assertEquals(expectedMessage, decryptedMessage)
        verify(exactly = 0) { messageRepository.markRead(any()) }
    }

    @Test
    fun verifyMarkUnReadOnInConversationModeWhenConversationHasMoreThanOneMessage() = runBlockingTest {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        every { savedStateHandle.get<String>(EXTRA_MAILBOX_LABEL_ID) } returns null
        val userId = UserId("userId3")
        every { userManager.requireCurrentUserId() } returns userId
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation("conversationId"))
        val conversationMessage = mockk<Message> {
            every { messageId } returns "messageId4"
            every { conversationId } returns "conversationId"
            every { subject } returns "subject4"
            every { sender } returns messageSender
            every { isDownloaded } returns true
            every { senderEmail } returns MESSAGE_SENDER_EMAIL_ADDRESS
            every { numAttachments } returns 1
            every { time } returns 82374730L
            every { decrypt(any(), any(), any()) } just Runs
            every { this@mockk setProperty "senderDisplayName" value any<String>() } just runs
            every { allLabelIDs } returns emptyList()
        }
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), "messageId5") } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.markUnread()

        // then
        coVerify(exactly = 1) {
            changeConversationsReadStatus.invoke(
                listOf(inputConversationId),
                ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                userId,
                inputMessageLocation,
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            messageRepository.markUnRead(listOf(inputConversationId))
        }
    }

    @Test
    fun verifyMarkUnReadOnInConversationModeWhenConversationHasOneMessage() = runBlockingTest {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        every { savedStateHandle.get<String>(EXTRA_MAILBOX_LABEL_ID) } returns null
        val userId = UserId("userId3")
        every { userManager.requireCurrentUserId() } returns userId
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversationWithOneMessage("conversationId"))
        val conversationMessage = mockk<Message> {
            every { messageId } returns "messageId4"
            every { conversationId } returns "conversationId"
            every { subject } returns "subject4"
            every { sender } returns messageSender
            every { isDownloaded } returns true
            every { senderEmail } returns MESSAGE_SENDER_EMAIL_ADDRESS
            every { numAttachments } returns 1
            every { time } returns 82374730L
            every { decrypt(any(), any(), any()) } just Runs
            every { this@mockk setProperty "senderDisplayName" value any<String>() } just runs
            every { allLabelIDs } returns emptyList()
        }
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage
        coEvery { messageRepository.markUnRead(listOf("messageId4")) } just runs

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.markUnread()

        // then
        coVerify(exactly = 0) {
            changeConversationsReadStatus.invoke(
                listOf(inputConversationId),
                ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                userId,
                inputMessageLocation,
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 1) {
            messageRepository.markUnRead(listOf("messageId4"))
        }
    }

    @Test
    fun verifyMarkUnReadInMessageMode() = runBlockingTest {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        val userId = UserId("userId3")
        every { userManager.requireCurrentUserId() } returns userId
        coEvery { conversationModeEnabled(inputMessageLocation) } returns false
        coEvery { messageRepository.markUnRead(any()) } just Runs
        val message = Message(
            messageId = inputConversationId,
            isDownloaded = true,
            sender = messageSender
        )
        coEvery { messageRepository.getMessage(userId, inputConversationId, true) } returns message

        // when
        observeMessageFlow.tryEmit(message)
        viewModel.markUnread()

        // then
        coVerify(exactly = 1) {
            messageRepository.markUnRead(listOf(inputConversationId))
        }
    }

    @Test
    fun verifyMoveToTrashInMessagesMode() {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        every { userManager.requireCurrentUserId() } returns testId1
        coEvery { conversationModeEnabled(inputMessageLocation) } returns false
        val message = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            folderLocation = inputMessageLocation.name
        )
        val downLoadedMessage = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = true,
            folderLocation = inputMessageLocation.name
        )
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns downLoadedMessage

        // when
        observeMessageFlow.tryEmit(message)
        viewModel.moveLastMessageToTrash()

        // then
        coVerify(exactly = 1) {
            moveMessagesToFolder.invoke(
                listOf(inputConversationId),
                Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString(),
                message.folderLocation ?: EMPTY_STRING
            )
        }
        coVerify(exactly = 0) {
            moveConversationsToFolder(any(), any(), any())
        }
    }

    @Test
    fun verifyMoveToTrashInConversationModeWhenConversationHasMoreThanOneMessage() {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        val userString = "userId3"
        val id = UserId(userString)
        val userId = UserId(userString)
        every { userManager.requireCurrentUserId() } returns id
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation("conversationId"))
        val conversationMessage = mockk<Message> {
            every { messageId } returns "messageId4"
            every { conversationId } returns "conversationId"
            every { subject } returns "subject4"
            every { sender } returns messageSender
            every { isDownloaded } returns true
            every { senderEmail } returns MESSAGE_SENDER_EMAIL_ADDRESS
            every { numAttachments } returns 1
            every { time } returns 82374730L
            every { decrypt(any(), any(), any()) } just Runs
            every { this@mockk setProperty "senderDisplayName" value any<String>() } just runs
            every { allLabelIDs } returns emptyList()
        }
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), "messageId5") } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.moveLastMessageToTrash()

        // then
        coVerify(exactly = 1) {
            moveConversationsToFolder(
                listOf(inputConversationId),
                userId,
                Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            moveMessagesToFolder.invoke(
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun verifyMoveToTrashInConversationModeWhenConversationHasOneMessage() {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        val userString = "userId3"
        val id = UserId(userString)
        val userId = UserId(userString)
        every { userManager.requireCurrentUserId() } returns id
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversationWithOneMessage("conversationId"))
        val conversationMessage = mockk<Message> {
            every { messageId } returns "messageId4"
            every { conversationId } returns "conversationId"
            every { subject } returns "subject4"
            every { sender } returns messageSender
            every { isDownloaded } returns true
            every { senderEmail } returns MESSAGE_SENDER_EMAIL_ADDRESS
            every { numAttachments } returns 1
            every { time } returns 82374730L
            every { decrypt(any(), any(), any()) } just Runs
            every { this@mockk setProperty "senderDisplayName" value any<String>() } just runs
            every { folderLocation } returns "folderLocation"
            every { allLabelIDs } returns emptyList()
        }
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.moveLastMessageToTrash()

        // then
        coVerify(exactly = 0) {
            moveConversationsToFolder(
                listOf(inputConversationId),
                userId,
                Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 1) {
            moveMessagesToFolder.invoke(
                any(),
                any(),
                any()
            )
        }

    }

    @Test
    fun verifyDeleteInMessagesModeCallsDeleteMessageUseCase() {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        every { userManager.requireCurrentUserId() } returns testId1
        coEvery { conversationModeEnabled(inputMessageLocation) } returns false
        val message = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            folderLocation = inputMessageLocation.name
        )
        val downLoadedMessage = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = true,
            folderLocation = inputMessageLocation.name
        )
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns downLoadedMessage

        // when
        observeMessageFlow.tryEmit(message)
        viewModel.delete()

        // then
        coVerify(exactly = 1) {
            deleteMessage.invoke(
                listOf(inputConversationId),
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            deleteConversations(
                listOf(inputConversationId),
                any(),
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
    }

    @Test
    fun verifyDeleteInConversationModeWhenConversationHasMoreThanOneMessageCallsDeleteConversationsUseCase() {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        val userString = "userId3"
        val userId = UserId(userString)
        every { userManager.requireCurrentUserId() } returns userId
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation("conversationId"))
        val conversationMessage = mockk<Message> {
            every { messageId } returns "messageId4"
            every { conversationId } returns "conversationId"
            every { subject } returns "subject4"
            every { sender } returns MessageSender("senderName", "sender@protonmail.ch")
            every { isDownloaded } returns true
            every { senderEmail } returns "sender@protonmail.com"
            every { numAttachments } returns 1
            every { time } returns 82374730L
            every { decrypt(any(), any(), any()) } just Runs
            every { this@mockk setProperty "senderDisplayName" value any<String>() } just runs
            every { allLabelIDs } returns emptyList()
        }
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), "messageId5") } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.delete()

        // then
        coVerify(exactly = 1) {
            deleteConversations(
                listOf(inputConversationId),
                userId,
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            deleteMessage.invoke(
                listOf(inputConversationId),
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
    }

    @Test
    fun verifyDeleteInConversationModeWhenConversationHasOneMessageCallsDeleteMessageUseCase() {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        val userString = "userId3"
        val userId = UserId(userString)
        every { userManager.requireCurrentUserId() } returns userId
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversationWithOneMessage("conversationId"))
        val conversationMessage = mockk<Message> {
            every { messageId } returns "messageId4"
            every { conversationId } returns "conversationId"
            every { subject } returns "subject4"
            every { sender } returns MessageSender("senderName", "sender@protonmail.ch")
            every { isDownloaded } returns true
            every { senderEmail } returns "sender@protonmail.com"
            every { numAttachments } returns 1
            every { time } returns 82374730L
            every { decrypt(any(), any(), any()) } just Runs
            every { this@mockk setProperty "senderDisplayName" value any<String>() } just runs
            every { folderLocation } returns "folderLocation"
            every { allLabelIDs } returns emptyList()
        }
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.delete()

        // then
        coVerify(exactly = 0) {
            deleteConversations(
                listOf(inputConversationId),
                userId,
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 1) {
            deleteMessage.invoke(
                listOf("messageId4"),
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }

    }

    @Test
    fun verifyStarMessagesInConversationModeIsWorking() {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        val userString = "userId3"
        val id = UserId(userString)
        val userId = UserId(userString)
        every { userManager.requireCurrentUserId() } returns id
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val isChecked = true

        // when
        viewModel.handleStarUnStar(inputConversationId, isChecked)

        // then
        coVerify(exactly = 1) {
            changeConversationsStarredStatus(
                listOf(inputConversationId),
                userId,
                ChangeConversationsStarredStatus.Action.ACTION_STAR
            )
        }
        coVerify(exactly = 0) {
            messageRepository.starMessages(
                any()
            )
        }
        coVerify(exactly = 0) {
            messageRepository.unStarMessages(
                any()
            )
        }
    }

    @Test
    fun verifyStarMessagesInMessageModeIsWorking() {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        val userString = "userId3"
        val id = UserId(userString)
        every { userManager.requireCurrentUserId() } returns id
        coEvery { conversationModeEnabled(inputMessageLocation) } returns false
        val isChecked = true
        every {
            messageRepository.starMessages(
                listOf(inputConversationId)
            )
        } just Runs

        // when
        viewModel.handleStarUnStar(inputConversationId, isChecked)

        // then
        coVerify(exactly = 0) {
            changeConversationsStarredStatus(
                any(),
                any(),
                any()
            )
        }
        coVerify(exactly = 1) {
            messageRepository.starMessages(
                listOf(inputConversationId)
            )
        }
        coVerify(exactly = 0) {
            messageRepository.unStarMessages(
                any()
            )
        }
    }

    @Test
    fun whenStoragePermissionDeniedShouldEmitNewDialogTriggerEvent() {
        val dialogTriggerObserver = viewModel.showPermissionMissingDialog.testObserver()

        viewModel.storagePermissionDenied()

        val numberOfReceivedEvents = dialogTriggerObserver.observedValues.size
        assertEquals(expected = 1, actual = numberOfReceivedEvents)
    }

    @Test
    fun verifyDeleteActionIsShownWhenConversationModeIsOnAndConversationHasMoreThanOneMessageAndLocationIsTrash() {
        val location = Constants.MessageLocationType.TRASH
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns location.messageLocationTypeValue
        every { conversationModeEnabled(location) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation("conversationId"))
        val conversationMessage = mockk<Message> {
            every { messageId } returns "messageId4"
            every { conversationId } returns "conversationId"
            every { subject } returns "subject4"
            every { sender } returns MessageSender("senderName", "sender@protonmail.ch")
            every { isDownloaded } returns true
            every { senderEmail } returns "sender@protonmail.com"
            every { numAttachments } returns 1
            every { time } returns 82374730L
            every { decrypt(any(), any(), any()) } just Runs
            every { this@mockk setProperty "senderDisplayName" value any<String>() } just runs
            every { allLabelIDs } returns emptyList()
        }
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), "messageId5") } returns conversationMessage

        val expectedResult = true

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        val result = viewModel.shouldShowDeleteActionInBottomActionBar()

        // then
        assertEquals(expectedResult, result)
    }

    @Test
    fun verifyDeleteActionIsShownWhenConversationModeIsOnAndConversationHasOneMessageAndLocationIsAnyOfSentSpamDraftOrTrash() {
        val location = Constants.MessageLocationType.SENT
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns location.messageLocationTypeValue
        every { conversationModeEnabled(location) } returns true
        val conversationResult =
            DataResult.Success(ResponseSource.Local, buildConversationWithOneMessage("conversationId"))
        val conversationMessage = mockk<Message> {
            every { messageId } returns "messageId4"
            every { conversationId } returns "conversationId"
            every { subject } returns "subject4"
            every { sender } returns MessageSender("senderName", "sender@protonmail.ch")
            every { isDownloaded } returns true
            every { senderEmail } returns "sender@protonmail.com"
            every { numAttachments } returns 1
            every { time } returns 82374730L
            every { decrypt(any(), any(), any()) } just Runs
            every { this@mockk setProperty "senderDisplayName" value any<String>() } just runs
            every { allLabelIDs } returns emptyList()
        }
        coEvery { messageRepository.findMessage(any(), "messageId4") } returns conversationMessage

        val expectedResult = true

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        val result = viewModel.shouldShowDeleteActionInBottomActionBar()

        // then
        assertEquals(expectedResult, result)
    }

    private fun buildConversation(conversationId: String): Conversation {
        val messageId = "messageId4"
        val secondMessageId = "messageId5"
        return Conversation(
            conversationId,
            "Conversation subject",
            listOf(),
            listOf(),
            5,
            2,
            1,
            0,
            listOf(inboxLabelContext()),
            listOf(
                buildMessageDomainModel(messageId, conversationId),
                buildMessageDomainModel(secondMessageId, conversationId)
            )
        )
    }

    private fun buildConversationWithOneMessage(conversationId: String): Conversation {
        val messageId = "messageId4"
        return Conversation(
            conversationId,
            "Conversation subject",
            listOf(),
            listOf(),
            1,
            1,
            1,
            0,
            listOf(inboxLabelContext()),
            listOf(
                buildMessageDomainModel(messageId, conversationId),
            )
        )
    }

    private fun buildMessageDomainModel(
        messageId: String,
        conversationId: String,
    ) = MessageDomainModel(
        messageId,
        conversationId,
        "subject4",
        false,
        Correspondent("senderName", MESSAGE_SENDER_EMAIL_ADDRESS),
        listOf(),
        82374724L,
        1,
        0L,
        isReplied = false,
        isRepliedAll = true,
        isForwarded = false,
        ccReceivers = emptyList(),
        bccReceivers = emptyList(),
        labelsIds = listOf("1", "2")
    )

    private fun inboxLabelContext() = LabelContext(INBOX.messageLocationTypeValue.toString(), 0, 0, 0L, 0, 0)
}
