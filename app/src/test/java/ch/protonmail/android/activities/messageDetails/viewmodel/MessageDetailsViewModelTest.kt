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

package ch.protonmail.android.activities.messageDetails.viewmodel

import android.graphics.Color
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
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.details.data.toConversationUiModel
import ch.protonmail.android.details.domain.usecase.GetViewInDarkModeMessagePreference
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.details.presentation.model.MessageBodyState
import ch.protonmail.android.details.presentation.model.RenderedMessage
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity.Companion.EXTRA_MAILBOX_LABEL_ID
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity.Companion.EXTRA_MESSAGE_LOCATION_ID
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity.Companion.EXTRA_MESSAGE_OR_CONVERSATION_ID
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.presentation.util.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.usecase.IsAppInDarkMode
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import ch.protonmail.android.usecase.message.ChangeMessagesReadStatus
import ch.protonmail.android.usecase.message.ChangeMessagesStarredStatus
import ch.protonmail.android.util.ProtonCalendarUtil
import ch.protonmail.android.utils.DownloadUtils
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

private const val INPUT_ITEM_DETAIL_ID = "inputMessageOrConversationId"
private const val MESSAGE_ID_ONE = "messageId1"
private const val MESSAGE_ID_TWO = "messageId2"
private const val CONVERSATION_ID = "conversationId"
private const val MESSAGE_TIME = 82374730L
private const val SUBJECT = "subject"
private const val MESSAGE_SENDER_EMAIL_ADDRESS = "sender@protonmail.com"

class MessageDetailsViewModelTest : ArchTest by ArchTest(),
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val isAppInDarkMode: IsAppInDarkMode = mockk()

    private val getViewInDarkModeMessagePreference: GetViewInDarkModeMessagePreference = mockk()

    private val changeMessagesReadStatus: ChangeMessagesReadStatus = mockk()

    private val changeConversationsReadStatus: ChangeConversationsReadStatus = mockk(relaxed = true)

    private val changeMessagesStarredStatus: ChangeMessagesStarredStatus = mockk()

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
        "defaultMockContactEmailId",
        "defaultMockContactEmailAddress",
        "defaultMockContactName"
    )
    private val contactsRepository: ContactsRepository = mockk {
        coEvery { findContactEmailByEmail(any(), any()) } returns testSenderContactEmail
    }

    private val attachmentsHelper: AttachmentsHelper = mockk(relaxed = true)

    private val downloadUtils: DownloadUtils = mockk()

    private val attachmentMetadataDao: AttachmentMetadataDao = mockk(relaxed = true)

    private val moveMessagesToFolder: MoveMessagesToFolder = mockk(relaxed = true)

    private val moveConversationsToFolder: MoveConversationsToFolder = mockk(relaxed = true)

    private val fetchVerificationKeys: FetchVerificationKeys = mockk(relaxed = true)

    private val verifyConnection: VerifyConnection = mockk(relaxed = true)

    private val networkConfigurator: NetworkConfigurator = mockk(relaxed = true)

    private val protonCalendarUtil: ProtonCalendarUtil = mockk()

    private val attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer = mockk(relaxed = true)

    private val conversationModeEnabled: ConversationModeEnabled = mockk {
        every { this@mockk(location = any(), userId = any()) } returns false
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
        every { currentUserId } returns testId1
        coEvery { primaryUserId } returns userIdFlow
    }

    private var messageRendererFactory = mockk<MessageRenderer.Factory> {
        every { create(any()) } returns mockk(relaxed = true) {
            coEvery { setImagesAndProcess(any(), any()) } answers {
                RenderedMessage(firstArg(), secondArg())
            }
        }
    }

    private val messageSender = MessageSender("senderName", MESSAGE_SENDER_EMAIL_ADDRESS)

    private lateinit var viewModel: MessageDetailsViewModel

    private val testColorInt = 871

    @BeforeTest
    fun setUp() {
        mockkStatic(Color::class)
        every { Color.parseColor(any()) } returns testColorInt
        viewModel = MessageDetailsViewModel(
            isAppInDarkMode = isAppInDarkMode,
            getViewInDarkModeMessagePreference = getViewInDarkModeMessagePreference,
            messageDetailsRepository = messageDetailsRepository,
            messageRepository = messageRepository,
            userManager = userManager,
            contactsRepository = contactsRepository,
            labelRepository = labelRepository,
            attachmentMetadataDao = attachmentMetadataDao,
            fetchVerificationKeys = fetchVerificationKeys,
            attachmentsWorker = attachmentsWorker,
            dispatchers = dispatchers,
            attachmentsHelper = attachmentsHelper,
            downloadUtils = downloadUtils,
            moveMessagesToFolder = moveMessagesToFolder,
            moveConversationsToFolder = moveConversationsToFolder,
            conversationModeEnabled = conversationModeEnabled,
            conversationRepository = conversationRepository,
            changeMessagesReadStatus = changeMessagesReadStatus,
            changeConversationsReadStatus = changeConversationsReadStatus,
            changeMessagesStarredStatus = changeMessagesStarredStatus,
            changeConversationsStarredStatus = changeConversationsStarredStatus,
            deleteMessage = deleteMessage,
            deleteConversations = deleteConversations,
            savedStateHandle = savedStateHandle,
            messageRendererFactory = messageRendererFactory,
            verifyConnection = verifyConnection,
            networkConfigurator = networkConfigurator,
            protonCalendarUtil = protonCalendarUtil
        )
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Color::class)
    }

    @Test
    fun loadMailboxItemGetsMessageFromServerWithMessageIdAndUserIdWhenWeDontHaveTheMessageInDb() =
        runTest(dispatchers.Main) {
            // given
            val messageOrConversationId = INPUT_ITEM_DETAIL_ID
            val downLoadedMessage = buildMessage(isDownloaded = true)
            coEvery { messageRepository.getMessage(testId1, messageOrConversationId, true) } returns downLoadedMessage
            val expected = ConversationUiModel(
                false,
                SUBJECT,
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
        val darkCssContent = "darkCss"
        val expected =
            "<html>\n <head>\n  <style>$cssContent$darkCssContent</style>\n  <meta name=\"viewport\" content=\"width=$windowWidth, maximum-scale=2\"> \n </head>\n <body>\n  <div id=\"pm-body\" class=\"inbox-body\">   $decryptedMessageContent  \n  </div>\n </body>\n</html>"

        // when
        val parsedMessage =
            viewModel.formatMessageHtmlBody(
                decryptedMessage, windowWidth, cssContent, darkCssContent, defaultErrorMessage
            )

        // then
        assertEquals(expected, parsedMessage)
    }

    @Test
    fun verifyThatDecryptedMessageHtmlIsEmittedThroughDecryptedConversationUiModel() {
        // given
        val decryptedMessageContent = "decrypted message content"
        // The ID of this message matches the ID of the "fake" message created by `buildMessageDomainModel` method
        val decryptedMessage = Message(messageId = MESSAGE_ID_ONE).apply {
            decryptedHTML = decryptedMessageContent
        }
        val decryptedConversationObserver = viewModel.decryptedConversationUiModel.testObserver()
        val cssContent = "css"
        val darkCssContent = "darkCss"
        val windowWidth = 500

        val conversationId = UUID.randomUUID().toString()
        val conversationMessage = buildMessage().toSpy()

        every { conversationModeEnabled(location = any(), userId = any()) } returns true
        every { userManager.requireCurrentUserId() } returns testId2
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation(conversationId))
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_TWO) } returns null

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.formatMessageHtmlBody(decryptedMessage, windowWidth, cssContent, darkCssContent, "errorHappened")

        // then
        val expectedMessageContent =
            "<html>\n <head>\n  <style>$cssContent$darkCssContent</style>\n  <meta name=\"viewport\" content=\"width=$windowWidth, maximum-scale=2\"> \n </head>\n <body>\n  <div id=\"pm-body\" class=\"inbox-body\">   $decryptedMessageContent  \n  </div>\n </body>\n</html>"
        verify { conversationMessage setProperty "decryptedHTML" value expectedMessageContent }
        assertEquals(conversationMessage, decryptedConversationObserver.observedValues.last()!!.messages[0])
    }

    @Test
    fun loadMailboxItemInvokesMessageRepositoryWithMessageIdAndUserId() = runTest(dispatchers.Main) {
        // Given
        val message = buildMessage(messageId = INPUT_ITEM_DETAIL_ID, isDownloaded = false)
        val downLoadedMessage = buildMessage(messageId = INPUT_ITEM_DETAIL_ID, isDownloaded = true)

        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns downLoadedMessage
        val expected = ConversationUiModel(
            false,
            SUBJECT,
            listOf(downLoadedMessage),
            null
        )

        // When
        viewModel.conversationUiModel.test {
            // Then
            coVerify { messageRepository.observeMessage(testUserId1, INPUT_ITEM_DETAIL_ID) }
            observeMessageFlow.emit(message)
            coVerify { contactsRepository.findContactEmailByEmail(any(), any()) }
            val actualItem = awaitItem()
            assertEquals(expected, actualItem)
            assertEquals(testSenderContactEmail.name, actualItem.messages[0].senderDisplayName)
        }
    }

    @Test
    fun shouldLoadMessageWithLabelsWhenLabelsPresent() = runTest(dispatchers.Main) {
        val labels = (1..2).map {
            Label(
                id = LabelId("id$it"),
                "name$it",
                testColorInt.toString(),
                0,
                LabelType.MESSAGE_LABEL,
                EMPTY_STRING,
                "parent",
            )
        }
        val folders = (3..5).map {
            Label(
                id = LabelId("id$it"),
                "name$it",
                testColorInt.toString(),
                0,
                LabelType.FOLDER,
                EMPTY_STRING,
                "parent",
            )
        }
        val allLabels = labels + folders
        val allLabelIds = allLabels.map { it.id }
        val message = buildMessage(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = false,
            allLabelIds = allLabelIds.map { it.id }
        )
        val downLoadedMessage = buildMessage(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = true,
            allLabelIds = allLabelIds.map { it.id }
        )
        val nonExclusiveLabels = hashMapOf(
            INPUT_ITEM_DETAIL_ID to allLabels.take(2).map {
                LabelChipUiModel(it.id, Name(it.name), color = testColorInt)
            }
        )
        val exclusiveLabels = hashMapOf(INPUT_ITEM_DETAIL_ID to allLabels.takeLast(3))
        every {
            labelRepository.observeLabels(allLabelIds)
        } returns flowOf(allLabels)
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns downLoadedMessage

        viewModel.conversationUiModel.test {
            observeMessageFlow.emit(message)
            val actualItem = awaitItem()
            assertEquals(exclusiveLabels, actualItem.exclusiveLabels)
            assertEquals(nonExclusiveLabels, actualItem.nonExclusiveLabels)
        }
    }

    @Test
    fun shouldLoadMessageWithoutLabelsWhenLabelsNotPresent() = runTest(dispatchers.Main) {
        val message = buildMessage(isDownloaded = false, allLabelIds = emptyList())
        val downLoadedMessage = buildMessage(isDownloaded = true, allLabelIds = emptyList())
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns downLoadedMessage

        viewModel.conversationUiModel.test {
            observeMessageFlow.emit(message)
            val actualItem = awaitItem()
            assertTrue(actualItem.exclusiveLabels.isEmpty())
            assertTrue(actualItem.nonExclusiveLabels.isEmpty())
        }
    }

    @Test
    fun shouldNotEmitConversationIfItIsIncomplete() = runTest(dispatchers.Main) {
        every { conversationModeEnabled(location = any(), userId = any()) } returns true
        every { userManager.requireCurrentUserId() } returns testId2
        val conversation = buildConversation(CONVERSATION_ID).copy(messagesCount = 99)
        val message1 = buildMessage()
        val message2 = buildMessage(messageId = MESSAGE_ID_TWO)
        coEvery { messageRepository.findMessage(testId2, MESSAGE_ID_ONE) } returns message1
        coEvery { messageRepository.findMessage(testId2, MESSAGE_ID_TWO) } returns message2

        viewModel.conversationUiModel.test {
            userIdFlow.emit(testId2)
            observeConversationFlow.tryEmit(DataResult.Success(ResponseSource.Local, conversation))
            expectNoEvents()
        }
    }

    @Test
    fun loadMailboxItemInvokesMessageRepositoryWithMessageIdAndUserIdForConversations() = runTest(dispatchers.Main) {
        // Given
        every { conversationModeEnabled(location = any(), userId = any()) } returns true
        every { userManager.requireCurrentUserId() } returns testId2
        val testConversation = buildConversation(CONVERSATION_ID)
        val testConversationResult = DataResult.Success(ResponseSource.Local, testConversation)
        every { userManager.requireCurrentUserId() } returns testId1
        // isDownloaded is false as with current converters (.toDbModel()) we loose this information
        val downLoadedMessage1 = buildMessage(isDownloaded = false)
        val downLoadedMessage2 = buildMessage(messageId = MESSAGE_ID_TWO, isDownloaded = false)
        coEvery { messageRepository.findMessage(testId2, MESSAGE_ID_ONE) } returns downLoadedMessage1
        coEvery { messageRepository.findMessage(testId2, MESSAGE_ID_TWO) } returns downLoadedMessage2

        // When
        viewModel.conversationUiModel.test {
            userIdFlow.emit(testUserId2)
            // Then
            coVerify { conversationRepository.getConversation(testId2, INPUT_ITEM_DETAIL_ID) }
            observeConversationFlow.emit(testConversationResult)
            coVerify { contactsRepository.findContactEmailByEmail(any(), any()) }
            val actualItem = awaitItem()
            assertNotNull(actualItem)
            assertEquals(testConversation.toConversationUiModel(), actualItem)
        }
    }

    @Test
    fun loadMessageBodyMarksMessageAsReadAndEmitsItWhenTheMessageWasSuccessfullyDecrypted() =
        runTest(dispatchers.Main) {
            // Given
            val messageSpy = buildMessage(unread = true).toSpy()
            coEvery { messageRepository.getMessage(any(), any(), any()) } returns messageSpy
            coEvery { messageRepository.markRead(any()) } just Runs

            // When
            val actual = viewModel.loadMessageBody(messageSpy).first()
            val expected = MessageBodyState.Success(messageSpy)

            // Then
        verify { messageRepository.markRead(listOf(MESSAGE_ID_ONE)) }
        assertEquals(expected, actual)
    }

    @Test
    fun loadMessageBodyDoesMarksMessageAsReadWhenTheMessageDecryptionFails() = runTest(dispatchers.Main) {
        // Given
        val messageSpy = buildMessage(unread = true).toSpy()
        every { messageSpy.decrypt(any(), any(), any()) } throws Exception("Test - Decryption failed")
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns messageSpy
        coEvery { messageRepository.markRead(any()) } just Runs

        // When
        val actual = viewModel.loadMessageBody(messageSpy).first()
        val expected = MessageBodyState.Error.DecryptionError(messageSpy)

        // Then
        verify { messageRepository.markRead(listOf(MESSAGE_ID_ONE)) }
        assertEquals(expected, actual)
    }

    @Test
    fun loadMessageDoesNotMarkMessageAsReadWhenTheMessageIsAlreadyRead() = runTest(dispatchers.Main) {
        // Given
        val messageSpy = buildMessage(unread = false).toSpy()
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns messageSpy

        // When
        viewModel.loadMessageBody(messageSpy).test {

            // Then
            verify(exactly = 0) { messageRepository.markRead(any()) }
            assertEquals(MessageBodyState.Success(messageSpy), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun loadMessageIsMarkedAsReadWhenTheMessageIsUnRead() = runTest(dispatchers.Main) {
        // Given
        val messageSpy = buildMessage(unread = true).toSpy()
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns messageSpy
        coEvery { messageRepository.markRead(any()) } just runs

        // When
        viewModel.loadMessageBody(messageSpy).test {

            // Then
            verify(exactly = 1) { messageRepository.markRead(listOf(MESSAGE_ID_ONE)) }
            assertEquals(MessageBodyState.Success(messageSpy), awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun loadMessageDoesNotEmitMessageToLiveDataWhenTheMessageWasNotFound() = runTest(dispatchers.Main) {
        // Given
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
        runTest(dispatchers.Main) {
            // Given
            val conversationObserver = viewModel.decryptedConversationUiModel.testObserver()
            val conversationId = UUID.randomUUID().toString()
            val conversationMessage = buildMessage()
            val olderConversationMessage = buildMessage(
                messageId = conversationMessage.messageId + "1",
                subject = conversationMessage.subject + "1",
                time = conversationMessage.time - 1000
            )
            every { userManager.requireCurrentUserId() } returns testId2
            coEvery { conversationModeEnabled(location = any(), userId = any()) } returns true
            val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation(conversationId))
            coEvery { messageRepository.findMessage(testId2, MESSAGE_ID_ONE) } returns conversationMessage
            coEvery { messageRepository.findMessage(testId2, MESSAGE_ID_TWO) } returns olderConversationMessage

            // When
            userIdFlow.tryEmit(testUserId2)
            observeConversationFlow.tryEmit(conversationResult)

            // Then
            val conversationUiModel = ConversationUiModel(
                false,
                SUBJECT,
                listOf(olderConversationMessage, conversationMessage),
                2
            )
            assertEquals(conversationUiModel, conversationObserver.observedValues[0])
        }

    @Test
    fun loadMessageBodyEmitsInputMessageWhenBodyIsAlreadyDecrypted() = runTest(dispatchers.Main) {
        val decryptedMessageHtml = "<html>Decrypted message body HTML</html>"
        val message = buildMessage().apply { decryptedHTML = decryptedMessageHtml }

        val decryptedMessage = viewModel.loadMessageBody(message).first()

        assertEquals(decryptedMessageHtml, (decryptedMessage as MessageBodyState.Success).message.decryptedHTML)
    }

    @Test
    fun loadMessageBodyFetchesMessageFromMessageRepositoryWhenInputMessageIsNotDecrypted() = runTest(dispatchers.Main) {
        // Given
        val message = buildMessage(unread = true)
        val fetchedMessage = buildMessage(unread = false)
            .apply { messageBody = "encrypted message body" }
            .toSpy()
        coEvery { messageRepository.getMessage(testId1, MESSAGE_ID_ONE, true) } returns fetchedMessage

        // When
        val decryptedMessage = viewModel.loadMessageBody(message).first()
        val expectedMessage = MessageBodyState.Success(fetchedMessage)

        // Then
        assertEquals(expectedMessage, decryptedMessage)
        verify(exactly = 0) { messageRepository.markRead(any()) }
    }

    @Test
    fun verifyMarkUnReadOnInConversationModeWhenConversationHasMoreThanOneMessage() = runTest(dispatchers.Main) {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        every { savedStateHandle.get<String>(EXTRA_MAILBOX_LABEL_ID) } returns null
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation(CONVERSATION_ID))
        val conversationMessage = buildMessage()
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_TWO) } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.markUnread()

        // then
        coVerify(exactly = 1) {
            changeConversationsReadStatus.invoke(
                listOf(inputConversationId),
                ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                testId1,
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            messageRepository.markUnRead(listOf(inputConversationId))
        }
    }

    @Test
    fun verifyMarkUnReadOnInConversationModeWhenConversationHasOneMessage() = runTest(dispatchers.Main) {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        every { savedStateHandle.get<String>(EXTRA_MAILBOX_LABEL_ID) } returns null
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult =
            DataResult.Success(ResponseSource.Local, buildConversationWithOneMessage(CONVERSATION_ID))
        val conversationMessage = buildMessage()
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage
        coEvery { messageRepository.markUnRead(listOf(MESSAGE_ID_ONE)) } just runs

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.markUnread()

        // then
        coVerify(exactly = 1) {
            changeConversationsReadStatus.invoke(
                listOf(inputConversationId),
                ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                testId1,
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            messageRepository.markUnRead(listOf(MESSAGE_ID_ONE))
        }
    }

    @Test
    fun verifyMarkUnReadInMessageMode() = runTest(dispatchers.Main) {
        // given
        val inputMessageLocation = INBOX
        // messageId is defined as a field as it's needed at VM's instantiation time.
        val inputConversationId = INPUT_ITEM_DETAIL_ID
        every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
            inputMessageLocation.messageLocationTypeValue
        coEvery { conversationModeEnabled(inputMessageLocation) } returns false
        coEvery {
            changeMessagesReadStatus.invoke(
                listOf(inputConversationId),
                ChangeMessagesReadStatus.Action.ACTION_MARK_UNREAD,
                any()
            )
        } just Runs
        val message = Message(
            messageId = inputConversationId,
            isDownloaded = true,
            sender = messageSender
        )
        coEvery { messageRepository.getMessage(testId1, inputConversationId, true) } returns message

        // when
        observeMessageFlow.tryEmit(message)
        viewModel.markUnread()

        // then
        coVerify(exactly = 1) {
            changeMessagesReadStatus.invoke(
                listOf(inputConversationId),
                ChangeMessagesReadStatus.Action.ACTION_MARK_UNREAD,
                any()
            )
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
        viewModel.moveToTrash()

        // then
        coVerify(exactly = 1) {
            moveMessagesToFolder.invoke(
                listOf(inputConversationId),
                Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString(),
                inputMessageLocation.messageLocationTypeValue.toString(),
                testUserId1
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
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation(CONVERSATION_ID))
        val conversationMessage = buildMessage()
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_TWO) } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.moveToTrash()

        // then
        coVerify(exactly = 1) {
            moveConversationsToFolder(
                listOf(inputConversationId),
                testId1,
                Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            moveMessagesToFolder.invoke(
                any(),
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
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult =
            DataResult.Success(ResponseSource.Local, buildConversationWithOneMessage(CONVERSATION_ID))
        val conversationMessage = buildMessage()
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.moveToTrash()

        // then
        coVerify(exactly = 1) {
            moveConversationsToFolder(
                listOf(inputConversationId),
                testId1,
                Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            moveMessagesToFolder.invoke(
                any(),
                any(),
                any(),
                testUserId1
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
                inputMessageLocation.messageLocationTypeValue.toString(),
                testUserId1
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
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation(CONVERSATION_ID))
        val conversationMessage = buildMessage()
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_TWO) } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.delete()

        // then
        coVerify(exactly = 1) {
            deleteConversations(
                listOf(inputConversationId),
                testId1,
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            deleteMessage.invoke(
                listOf(inputConversationId),
                inputMessageLocation.messageLocationTypeValue.toString(),
                testId1
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
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val conversationResult =
            DataResult.Success(ResponseSource.Local, buildConversationWithOneMessage(CONVERSATION_ID))
        val conversationMessage = buildMessage()
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        viewModel.delete()

        // then
        coVerify(exactly = 1) {
            deleteConversations(
                listOf(inputConversationId),
                testId1,
                inputMessageLocation.messageLocationTypeValue.toString()
            )
        }
        coVerify(exactly = 0) {
            deleteMessage.invoke(
                listOf(MESSAGE_ID_ONE),
                inputMessageLocation.messageLocationTypeValue.toString(),
                testUserId1
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
        coEvery { conversationModeEnabled(inputMessageLocation) } returns true
        val isChecked = true

        // when
        viewModel.handleStarUnStar(inputConversationId, isChecked)

        // then
        coVerify(exactly = 1) {
            changeConversationsStarredStatus(
                listOf(inputConversationId),
                testId1,
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
        coEvery { conversationModeEnabled(inputMessageLocation) } returns false
        val isChecked = true
        coEvery {
            changeMessagesStarredStatus(
                any(),
                listOf(inputConversationId),
                ChangeMessagesStarredStatus.Action.ACTION_STAR
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
            changeMessagesStarredStatus(
                any(),
                listOf(inputConversationId),
                ChangeMessagesStarredStatus.Action.ACTION_STAR
            )
        }
        coVerify(exactly = 0) {
            changeMessagesStarredStatus(
                any(),
                listOf(inputConversationId),
                ChangeMessagesStarredStatus.Action.ACTION_UNSTAR
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
        val conversationResult = DataResult.Success(ResponseSource.Local, buildConversation(CONVERSATION_ID))
        val conversationMessage = buildMessage()
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_TWO) } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        val result = viewModel.shouldShowDeleteActionInBottomActionBar()

        // then
        assertTrue(result)
    }

    @Test
    fun verifyDeleteActionIsShownWhenConversationModeIsOnAndConversationHasOneMessageAndLocationIsAnyOfSentSpamDraftOrTrash() {
        val location = Constants.MessageLocationType.SENT
        every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns location.messageLocationTypeValue
        every { conversationModeEnabled(location) } returns true
        val conversationResult =
            DataResult.Success(ResponseSource.Local, buildConversationWithOneMessage(CONVERSATION_ID))
        val conversationMessage = buildMessage()
        coEvery { messageRepository.findMessage(any(), MESSAGE_ID_ONE) } returns conversationMessage

        // when
        userIdFlow.tryEmit(testUserId2)
        observeConversationFlow.tryEmit(conversationResult)
        val result = viewModel.shouldShowDeleteActionInBottomActionBar()

        // then
        assertTrue(result)
    }

    @Test
    fun whenLoadingMessageBodyAndPausedShouldNotMarkMessageAsRead() = runTest(dispatchers.Main) {
        // Given
        val message = Message(messageId = INPUT_ITEM_DETAIL_ID, Unread = true).toSpy()
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns message
        every { messageRepository.markRead(any()) } returns Unit

        // When
        viewModel.pause()
        val loadMessageBodyFlow = viewModel.loadMessageBody(message)

        // Then
        loadMessageBodyFlow.test {
            coVerify(exactly = 0) { messageRepository.markRead(any()) }
            awaitItem()
            awaitComplete()
        }
    }

    @Test
    fun whenLoadingMessageBodyAndResumedShouldMarkMessageAsRead() = runTest(dispatchers.Main) {
        // Given
        val message = Message(messageId = INPUT_ITEM_DETAIL_ID, Unread = true).toSpy()
        coEvery { messageRepository.getMessage(testId1, INPUT_ITEM_DETAIL_ID, true) } returns message
        every { messageRepository.markRead(any()) } returns Unit

        // When
        viewModel.pause()
        viewModel.resume()
        val loadMessageBodyFlow = viewModel.loadMessageBody(message)

        // Then
        loadMessageBodyFlow.test {
            coVerify(exactly = 1) { messageRepository.markRead(listOf(INPUT_ITEM_DETAIL_ID)) }
            awaitItem()
            awaitComplete()
        }
    }

    private fun buildMessage(
        messageId: String = MESSAGE_ID_ONE,
        subject: String = SUBJECT,
        isDownloaded: Boolean = true,
        time: Long = MESSAGE_TIME,
        unread: Boolean = false,
        allLabelIds: List<String> = listOf("1", "2")
    ): Message {
        return Message(
            messageId = messageId,
            conversationId = CONVERSATION_ID,
            subject = subject,
            sender = messageSender,
            isDownloaded = isDownloaded,
            numAttachments = 1,
            time = time,
            Unread = unread,
            isReplied = false,
            isRepliedAll = true,
            isForwarded = false,
            allLabelIDs = allLabelIds
        )
    }

    private fun Message.toSpy(): Message {
        return spyk(this).apply {
            every { decrypt(any(), any(), any()) } just runs
        }
    }

    private fun buildConversation(conversationId: String): Conversation {
        val messageId = MESSAGE_ID_ONE
        val secondMessageId = MESSAGE_ID_TWO
        return Conversation(
            conversationId,
            SUBJECT,
            listOf(),
            listOf(),
            2,
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
        val messageId = MESSAGE_ID_ONE
        return Conversation(
            conversationId,
            SUBJECT,
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
        SUBJECT,
        false,
        Correspondent("senderName", MESSAGE_SENDER_EMAIL_ADDRESS),
        listOf(),
        MESSAGE_TIME,
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
