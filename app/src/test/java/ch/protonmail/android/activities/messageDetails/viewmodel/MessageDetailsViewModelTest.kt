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
import ch.protonmail.android.activities.messageDetails.MessageRenderer
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.attachments.AttachmentsHelper
import ch.protonmail.android.attachments.DownloadEmbeddedAttachmentsWorker
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.LabelRepository
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.details.presentation.MessageDetailsActivity.Companion.EXTRA_MESSAGE_LOCATION_ID
import ch.protonmail.android.details.presentation.MessageDetailsActivity.Companion.EXTRA_MESSAGE_OR_CONVERSATION_ID
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.fetch.FetchVerificationKeys
import ch.protonmail.android.utils.DownloadUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

private const val INPUT_ITEM_DETAIL_ID = "inputMessageOrConversationId"

class MessageDetailsViewModelTest : ArchTest, CoroutinesTest {

    private val conversationRepository: ConversationsRepository = mockk(relaxed = true)

    private val messageDetailsRepository: MessageDetailsRepository = mockk(relaxed = true)

    private val messageRepository: MessageRepository = mockk(relaxed = true)

    private val labelRepository: LabelRepository = mockk(relaxed = true)

    private val contactsRepository: ContactsRepository = mockk(relaxed = true)

    private val attachmentsHelper: AttachmentsHelper = mockk(relaxed = true)

    private val attachmentMetadataDao: AttachmentMetadataDao = mockk(relaxed = true)

    private val moveMessagesToFolder: MoveMessagesToFolder = mockk(relaxed = true)

    private val fetchVerificationKeys: FetchVerificationKeys = mockk(relaxed = true)

    private val verifyConnection: VerifyConnection = mockk(relaxed = true)

    private val networkConfigurator: NetworkConfigurator = mockk(relaxed = true)

    private val attachmentsWorker: DownloadEmbeddedAttachmentsWorker.Enqueuer = mockk(relaxed = true)

    private val conversationModeEnabled: ConversationModeEnabled = mockk {
        every { this@mockk.invoke(any()) } returns false
    }

    private val savedStateHandle = mockk<SavedStateHandle> {
        every { get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns INPUT_ITEM_DETAIL_ID
        every { get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns INBOX.messageLocationTypeValue
    }

    private val userManager: UserManager = mockk(relaxed = true) {
        every { requireCurrentUserId() } returns Id("userId1")
    }

    private var messageRendererFactory = mockk<MessageRenderer.Factory> {
        every { create(any(), any()) } returns mockk(relaxed = true) {
            every { renderedBody } returns Channel()
        }
    }

    private lateinit var viewModel: MessageDetailsViewModel

    @Before
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
            conversationModeEnabled,
            conversationRepository,
            savedStateHandle,
            messageRendererFactory,
            verifyConnection,
            networkConfigurator,
        )
    }

    @Test
    fun verifyThatMessageIsParsedProperly() {
        // given
        val decryptedMessage = "decrypted message content"
        val windowWidth = 500
        val defaultErrorMessage = "errorHappened"
        val cssContent = "css"
        val expected =
            "<html>\n <head>\n  <style>$cssContent</style>\n  <meta name=\"viewport\" content=\"width=$windowWidth, maximum-scale=2\"> \n </head>\n <body>\n  <div id=\"pm-body\" class=\"inbox-body\">   $decryptedMessage  \n  </div>\n </body>\n</html>"

        // when
        val parsedMessage = viewModel.getParsedMessage(decryptedMessage, windowWidth, cssContent, defaultErrorMessage)

        // then
        assertEquals(expected, parsedMessage)
    }

    @Test
    fun loadMessageDetailsInvokesMessageRepositoryWithMessageIdAndUserId() = runBlockingTest {
        // Given
        val userId = Id("userId2")
        every { userManager.requireCurrentUserId() } returns userId
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns Message()

        // When
        viewModel.loadMessageDetails()

        // Then
        coVerify { messageRepository.getMessage(userId, INPUT_ITEM_DETAIL_ID, true) }
    }

    @Test
    fun loadMessageEmitsFoundMessageToLiveDataWithContactNameAsDisplayName() = runBlockingTest {
        // Given
        val senderEmail = "senderEmail2"
        val message = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = true,
            sender = MessageSender("senderName", senderEmail)
        )
        val senderContact = ContactEmail("ceId2", senderEmail, "senderContactName")
        val messageObserver = viewModel.decryptedMessageData.testObserver()
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns message
        coEvery { contactsRepository.findContactEmailByEmail(senderEmail) } returns senderContact

        // When
        viewModel.loadMessageDetails()

        // Then
        val expectedMessage = message.copy()
        expectedMessage.senderDisplayName = "senderContactName"
        val emittedMessage = messageObserver.observedValues[0]?.messages?.last()
        assertEquals(expectedMessage, emittedMessage)
        assertEquals(expectedMessage.senderDisplayName, emittedMessage!!.senderDisplayName)
    }

    @Test
    fun loadMessageDoesNotEmitTheFoundMessageToLiveDataWhenTheMessageIsNotDownloaded() = runBlockingTest {
        // Given
        val senderEmail = "senderEmail2"
        val message = Message(
            messageId = INPUT_ITEM_DETAIL_ID,
            isDownloaded = false,
            sender = MessageSender("senderName", senderEmail)
        )
        val messageObserver = viewModel.decryptedMessageData.testObserver()
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns message

        // When
        viewModel.loadMessageDetails()

        // Then
        assertEquals(emptyList(), messageObserver.observedValues)
    }

    @Test
    fun loadMessageDoesNotEmitMessageToLiveDataWhenTheMessageWasNotFound() = runBlockingTest {
        // Given
        val messageErrorObserver = viewModel.messageDetailsError.testObserver()
        coEvery { messageRepository.getMessage(any(), any(), any()) } returns null

        // When
        viewModel.loadMessageDetails()

        // Then
        assertEquals("Failed getting message details", messageErrorObserver.observedValues[0]?.getContentIfNotHandled())
    }

    @Test
    fun loadMessageDetailsInvokesConversationRepositoryWithConversationIdAndUserIdWhenConversationModeIsEnabled() =
        runBlockingTest {
            // Given
            val inputMessageLocation = INBOX
            // messageId is defined as a field as it's needed at VM's instantiation time.
            val inputConversationId = INPUT_ITEM_DETAIL_ID
            val userId = Id("userId3")
            every { userManager.requireCurrentUserId() } returns userId
            coEvery { conversationModeEnabled(inputMessageLocation) } returns true
            every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
            every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
                inputMessageLocation.messageLocationTypeValue

            // When
            viewModel.loadMessageDetails()

            // Then
            coVerify(exactly = 0) { messageRepository.getMessage(any(), any()) }
            coVerify { conversationRepository.getConversation(inputConversationId, userId) }
        }

    @Test
    fun loadMessageDetailsEmitsConversationUiItemWithConversationDataWhenRepositoryReturnsAConversation() =
        runBlockingTest {
            // Given
            val inputMessageLocation = INBOX
            // messageId is defined as a field as it's needed at VM's instantiation time.
            val inputConversationId = INPUT_ITEM_DETAIL_ID
            val userId = Id("userId4")
            val conversationObserver = viewModel.decryptedMessageData.testObserver()
            val conversationId = UUID.randomUUID().toString()
            val conversationMessage = Message(
                messageId = "messageId4",
                conversationId = conversationId,
                subject = "subject4",
                Unread = false,
                sender = MessageSender("senderName", "sender@protonmail.ch"),
                time = 82374724L,
                numAttachments = 1,
                expirationTime = 0L,
                isReplied = false,
                isRepliedAll = true,
                isForwarded = false,
                ccList = emptyList(),
                bccList = emptyList(),
                allLabelIDs = listOf("1", "2"),
                isDownloaded = true
            )
            every { userManager.requireCurrentUserId() } returns userId
            coEvery { conversationModeEnabled(inputMessageLocation) } returns true
            every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
            every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
                inputMessageLocation.messageLocationTypeValue
            coEvery { conversationRepository.getConversation(inputConversationId, userId) } returns
                flowOf(DataResult.Success(ResponseSource.Local, buildConversation(conversationId)))
            // Return the same message from DB for simplicity
            coEvery { messageRepository.getMessage(userId, "messageId4", true) } returns conversationMessage
            coEvery { messageRepository.getMessage(userId, "messageId5", true) } returns conversationMessage

            // When
            viewModel.loadMessageDetails()

            // Then
            val conversationUiModel = ConversationUiModel(
                false, "subject4", listOf("1", "2"), listOf(conversationMessage, conversationMessage)
            )
            assertEquals(conversationUiModel, conversationObserver.observedValues[0])
        }

    @Test
    fun loadMessageDetailsEmitsErrorWhenConversationIsEnabledAndConversationRepositorySucceedsButMessageIsNotInDatabase() =
        runBlockingTest {
            // Given
            val inputMessageLocation = INBOX
            // messageId is defined as a field as it's needed at VM's instantiation time.
            val inputConversationId = INPUT_ITEM_DETAIL_ID
            val userId = Id("userId4")
            val errorObserver = viewModel.messageDetailsError.testObserver()
            val conversationId = UUID.randomUUID().toString()
            every { userManager.requireCurrentUserId() } returns userId
            coEvery { conversationModeEnabled(inputMessageLocation) } returns true
            every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
            every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
                inputMessageLocation.messageLocationTypeValue
            coEvery { conversationRepository.getConversation(inputConversationId, userId) } returns
                flowOf(DataResult.Success(ResponseSource.Local, buildConversation(conversationId)))
            coEvery { messageRepository.getMessage(userId, any(), true) } returns null

            // When
            viewModel.loadMessageDetails()

            // Then
            assertEquals("Failed getting message details", errorObserver.observedValues[0]?.getContentIfNotHandled())
        }

    @Test
    fun loadMessageDetailsEmitsErrorWhenConversationIsEnabledAndConversationRepositoryFails() =
        runBlockingTest {
            // Given
            val inputMessageLocation = INBOX
            // messageId is defined as a field as it's needed at VM's instantiation time.
            val inputConversationId = INPUT_ITEM_DETAIL_ID
            val userId = Id("userId4")
            val errorObserver = viewModel.messageDetailsError.testObserver()
            every { userManager.requireCurrentUserId() } returns userId
            coEvery { conversationModeEnabled(inputMessageLocation) } returns true
            every { savedStateHandle.get<String>(EXTRA_MESSAGE_OR_CONVERSATION_ID) } returns inputConversationId
            every { savedStateHandle.get<Int>(EXTRA_MESSAGE_LOCATION_ID) } returns
                inputMessageLocation.messageLocationTypeValue
            coEvery { conversationRepository.getConversation(inputConversationId, userId) } returns
                flowOf(DataResult.Error.Local("failed getting conversation", null))

            // When
            viewModel.loadMessageDetails()

            // Then
            assertEquals("Failed getting message details", errorObserver.observedValues[0]?.getContentIfNotHandled())
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

    private fun buildMessageDomainModel(messageId: String, conversationId: String) = MessageDomainModel(
        messageId,
        conversationId,
        "subject4",
        false,
        Correspondent("senderName", "sender@protonmail.ch"),
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
