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

package ch.protonmail.android.mailbox.presentation.mapper

import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.Constants.MessageLocationType.SENT
import ch.protonmail.android.core.Constants.MessageLocationType.STARRED
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.mapper.LabelChipUiModelMapper
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.mailbox.presentation.model.MessageData
import ch.protonmail.android.ui.model.LabelChipUiModel
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MailboxItemUiModelMapperTest {

    private val contactsRepository: ContactsRepository = mockk {
        coEvery { findContactEmailByEmail(any(), any()) } returns buildContactEmail()
        every { findContactsByEmail(any(), any()) } returns flowOf(emptyList())
    }
    private val labelChipUiModelMapper: LabelChipUiModelMapper = mockk {
        every { toUiModels(any()) } returns emptyList()
    }
    private val mapper = MailboxItemUiModelMapper(
        contactsRepository = contactsRepository,
        labelChipUiModelMapper = labelChipUiModelMapper
    )

    // region Messages
    @Test
    fun `message is mapped correctly`() = runBlockingTest {
        // given
        val message = Message().apply {
            messageId = TEST_ITEM_ID
            sender = MessageSender(TEST_SENDER_NAME, TEST_SENDER_ADDRESS, true)
            subject = TEST_SUBJECT
            time = TEST_MESSAGE_TIME_SEC
            numAttachments = 1
            isStarred = true
            Unread = true
            expirationTime = TEST_EXPIRATION_TIME
            toList = listOf(MessageRecipient(TEST_RECIPIENT_NAME, TEST_RECIPIENT_ADDRESS))
            location = INBOX.messageLocationTypeValue
            isReplied = true
            isRepliedAll = false
            isForwarded = false
            isInline = false
        }
        val expected = MailboxItemUiModel(
            itemId = TEST_ITEM_ID,
            correspondentsNames = TEST_SENDER_NAME,
            subject = TEST_SUBJECT,
            lastMessageTimeMs = TEST_MESSAGE_TIME_MS,
            hasAttachments = true,
            isStarred = true,
            isRead = false,
            expirationTime = TEST_EXPIRATION_TIME,
            messagesCount = null,
            messageData = MessageData(
                location = INBOX.messageLocationTypeValue,
                isReplied = true,
                isRepliedAll = false,
                isForwarded = false,
                isInline = false
            ),
            messageLabels = emptyList(),
            allLabelsIds = emptyList(),
            isDraft = false,
            isScheduled = false,
            isProton = true
        )

        // when
        val result = mapper.toUiModel(message, INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message is draft if labels contain DRAFT or ALL_DRAFT`() = runBlockingTest {
        // given
        val message = buildMessage()
        val expected = buildMailboxItemUiModel(messageData = buildMessageData())

        // when
        val result = mapper.toUiModel(message, INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message is scheduled if labels contain ALL_SCHEDULED`() = runBlockingTest {
        // given
        val message = buildMessage(allLabelsIds = listOf(MessageLocationType.ALL_SCHEDULED.asLabelId()))
        val expected = buildMailboxItemUiModel(
            isScheduled = true,
            messageData = buildMessageData(),
            allLabelsIds = listOf(MessageLocationType.ALL_SCHEDULED.asLabelId())
        )

        // when
        val result = mapper.toUiModel(message, INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message sender has name from correspondent address`() = runBlockingTest {
        // given
        val sender = MessageSender(EMPTY_STRING, TEST_EMAIL_ADDRESS, true)
        val message = buildMessage(sender)
        val expected = buildMailboxItemUiModel(
            correspondentsNames = sender.emailAddress!!, messageData = buildMessageData(), isProton = true
        )

        // when
        val result = mapper.toUiModel(message, INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message sender has name from correspondent name`() = runBlockingTest {
        // given
        val sender = MessageSender(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS, true)
        val message = buildMessage(sender)
        val expected = buildMailboxItemUiModel(
            correspondentsNames = sender.name!!, messageData = buildMessageData(), isProton = true
        )

        // when
        val result = mapper.toUiModel(message, INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message has sender as correspondents names for inbox folder`() = runBlockingTest {
        // given
        val sender = MessageSender(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS, true)
        val message = buildMessage(sender = sender)
        val expected = buildMailboxItemUiModel(
            correspondentsNames = TEST_CORRESPONDENT_NAME, messageData = buildMessageData(), isProton = true
        )

        // when
        val result = mapper.toUiModel(message, INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message has recipient as correspondents names for sent folder`() = runBlockingTest {
        // given
        val recipient = MessageRecipient(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS)
        val message = buildMessage(toRecipients = listOf(recipient))
        val expected = buildMailboxItemUiModel(
            correspondentsNames = TEST_CORRESPONDENT_NAME, messageData = buildMessageData()
        )

        // when
        val result = mapper.toUiModel(message, SENT.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message has recipient as correspondents names for draft folder`() = runBlockingTest {
        // given
        val recipient = MessageRecipient(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS)
        val message = buildMessage(toRecipients = listOf(recipient))
        val expected = buildMailboxItemUiModel(
            correspondentsNames = TEST_CORRESPONDENT_NAME, messageData = buildMessageData()
        )

        // when
        val result = mapper.toUiModel(message, DRAFT.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message's messageLabels contains only labels of type MESSAGE_LABEL`() = runBlockingTest {
        // given
        labelChipUiModelMapper.mockUiModelsCreation()

        val messageFoldersIds = listOf(
            TestLabels.INBOX,
            TestLabels.HEALTH_DOCUMENTS_FOLDER
        ).map { it.id }
        val messageLabelsIds = listOf(
            TestLabels.GAME_LABEL,
            TestLabels.TRAVEL_LABEL
        ).map { it.id }
        val message = buildMessage(allLabelsIds = messageFoldersIds + messageLabelsIds)
        val expected = messageLabelsIds.map(::buildLabelChipUiModel)

        // when
        val result = mapper.toUiModel(message, INBOX.asLabelId(), TestLabels.allLabelsAndFolders())

        // then
        assertEquals(expected, result.messageLabels)
    }

    @Test
    fun `message's allLabelsIds contains ids for every type of label, including default ones`() = runBlockingTest {
        // given
        val messageLabelsAndFoldersIds = listOf(
            TestLabels.INBOX,
            TestLabels.HEALTH_DOCUMENTS_FOLDER,
            TestLabels.GAME_LABEL,
            TestLabels.TRAVEL_LABEL
        ).map { it.id }
        val message = buildMessage(allLabelsIds = messageLabelsAndFoldersIds)
        val expected = messageLabelsAndFoldersIds

        // when
        val result = mapper.toUiModel(message, INBOX.asLabelId(), TestLabels.allLabelsAndFolders())

        // then
        assertEquals(expected, result.allLabelsIds)
    }

    @Test
    fun `message recipients contains from To, Cc and Bcc`() = runBlockingTest {
        // given
        val toRecipients = buildRecipients("first", "second")
        val ccRecipients = buildRecipients("third, fourth")
        val bccRecipients = buildRecipients("fifth", "sixth")
        val message = buildMessage(
            toRecipients = toRecipients,
            ccRecipients = ccRecipients,
            bccRecipients = bccRecipients
        )
        val expected = "first, second, third, fourth, fifth, sixth"

        // when
        val result = mapper.toUiModel(message, SENT.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result.correspondentsNames)
    }
    // endregion

    // region Conversations

    @Test
    fun `conversation is mapped correctly`() = runBlockingTest {
        // given
        val currentLabelId = SENT.asLabelId()
        val conversation = Conversation(
            id = TEST_ITEM_ID,
            subject = TEST_SUBJECT,
            senders = listOf(Correspondent(TEST_SENDER_NAME, TEST_SENDER_ADDRESS)),
            receivers = listOf(Correspondent(TEST_RECIPIENT_NAME, TEST_RECIPIENT_ADDRESS)),
            messagesCount = 2,
            unreadCount = 1,
            attachmentsCount = 3,
            expirationTime = TEST_EXPIRATION_TIME,
            labels = emptyList(),
            messages = emptyList()
        )
        val expected = MailboxItemUiModel(
            itemId = TEST_ITEM_ID,
            correspondentsNames = TEST_RECIPIENT_NAME,
            subject = TEST_SUBJECT,
            lastMessageTimeMs = 0,
            hasAttachments = true,
            isStarred = false,
            isRead = false,
            expirationTime = TEST_EXPIRATION_TIME,
            messagesCount = 2,
            messageData = null,
            messageLabels = emptyList(),
            allLabelsIds = emptyList(),
            isDraft = false,
            isScheduled = false,
            isProton = false
        )

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = currentLabelId, allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation is starred if labels contain starred label id`() = runBlockingTest {
        // given
        val conversation = buildConversation(
            allLabelsIds = listOf(STARRED.asLabelId())
        )

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertTrue(result.isStarred)
    }

    @Test
    fun `conversation sender has name from correspondent address`() = runBlockingTest {
        // given
        val sender = Correspondent(EMPTY_STRING, TEST_EMAIL_ADDRESS)
        val conversation = buildConversation(sender)
        val expected = buildMailboxItemUiModel(correspondentsNames = sender.address)

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation sender has name from correspondent name`() = runBlockingTest {
        // given
        val sender = Correspondent(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS)
        val conversation = buildConversation(sender)
        val expected = buildMailboxItemUiModel(correspondentsNames = sender.name)

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation sender has name from contact name`() = runBlockingTest {
        // given
        val sender = Correspondent(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS)
        every { contactsRepository.findContactsByEmail(any(), any()) } returns flowOf(
            listOf(ContactEmail(EMPTY_STRING, TEST_EMAIL_ADDRESS, TEST_CONTACT_NAME))
        )
        val conversation = buildConversation(sender)
        val expected = buildMailboxItemUiModel(correspondentsNames = TEST_CONTACT_NAME)

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation is draft when it has only one draft message`() = runBlockingTest {
        // given
        val conversation = buildConversation(
            messageCount = 1,
            allLabelsIds = listOf(ALL_DRAFT.asLabelId(), DRAFT.asLabelId())
        )

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertTrue(result.isDraft)
    }

    @Test
    fun `conversation is not draft when it has more than one draft message`() = runBlockingTest {
        // given
        val conversation = buildConversation(
            messageCount = 2,
            allLabelsIds = listOf(ALL_DRAFT.asLabelId(), DRAFT.asLabelId())
        )

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertFalse(result.isDraft)
    }

    @Test
    fun `conversation last message time is used from the correct current label`() = runBlockingTest {
        // given
        val currentLabel = SENT.asLabelId()
        val conversation = buildConversation(
            messageCount = 2,
            labels = listOf(
                buildLabelContext(DRAFT.asLabelId()),
                buildLabelContext(currentLabel, contextTime = TEST_MESSAGE_TIME_SEC)
            )
        )

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = SENT.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(TEST_MESSAGE_TIME_MS, result.lastMessageTimeMs)
    }

    @Test
    fun `conversation message count is null when below 2, so it won't be displayed`() = runBlockingTest {
        // given
        val conversation = buildConversation(messageCount = 1)
        val expected = buildMailboxItemUiModel(messageCount = null)

        // when
        val result =
            mapper.toUiModel(TEST_USER_ID, conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation's messageLabels contains only labels of type MESSAGE_LABEL`() = runBlockingTest {
        // given
        labelChipUiModelMapper.mockUiModelsCreation()

        val conversationFoldersIds = listOf(
            TestLabels.INBOX,
            TestLabels.HEALTH_DOCUMENTS_FOLDER
        ).map { it.id }
        val conversationLabelsIds = listOf(
            TestLabels.GAME_LABEL,
            TestLabels.TRAVEL_LABEL
        ).map { it.id }
        val conversation = buildConversation(allLabelsIds = conversationFoldersIds + conversationLabelsIds)
        val expected = conversationLabelsIds.map(::buildLabelChipUiModel)

        // when
        val result = mapper.toUiModel(TEST_USER_ID, conversation, INBOX.asLabelId(), TestLabels.allLabelsAndFolders())

        // then
        assertEquals(expected, result.messageLabels)
    }

    @Test
    fun `conversation's allLabelsIds contains ids for every type of label, including default ones`() = runBlockingTest {
        // given
        val conversationLabelsAndFoldersIds = listOf(
            TestLabels.INBOX,
            TestLabels.HEALTH_DOCUMENTS_FOLDER,
            TestLabels.GAME_LABEL,
            TestLabels.TRAVEL_LABEL
        ).map { it.id }
        val conversation = buildConversation(allLabelsIds = conversationLabelsAndFoldersIds)
        val expected = buildMailboxItemUiModel(allLabelsIds = conversationLabelsAndFoldersIds)

        // when
        val result = mapper.toUiModel(TEST_USER_ID, conversation, INBOX.asLabelId(), TestLabels.allLabelsAndFolders())

        // then
        assertEquals(expected, result)
    }
    // endregion

    companion object TestData {

        private val TEST_USER_ID = UserId("user")
        private const val TEST_ITEM_ID = "item_id"
        private const val TEST_SUBJECT = "subject"
        private const val TEST_SENDER_NAME = "sender"
        private const val TEST_SENDER_ADDRESS = "sender@proton.ch"
        private const val TEST_RECIPIENT_NAME = "recipient"
        private const val TEST_RECIPIENT_ADDRESS = "recipient@proton.ch"
        private const val TEST_CORRESPONDENT_NAME = "correspondent"
        private const val TEST_EMAIL_ADDRESS = "email@proton.ch"
        private const val TEST_CONTACT_NAME = "contact"
        private const val TEST_MESSAGE_TIME_SEC = 468L
        private const val TEST_MESSAGE_TIME_MS = TEST_MESSAGE_TIME_SEC * 1_000
        private const val TEST_EXPIRATION_TIME = 567L

        private object TestLabels {

            val ALL_STANDARD_LABELS =
                MessageLocationType.values().map { buildLabel(it.asLabelId(), type = LabelType.FOLDER) }
            val INBOX = buildLabel(MessageLocationType.INBOX.asLabelId(), type = LabelType.FOLDER)

            val WORK_DOCUMENTS_FOLDER = buildLabel(LabelId("work documents"), type = LabelType.FOLDER)
            val FUN_DOCUMENTS_FOLDER = buildLabel(LabelId("fun documents"), type = LabelType.FOLDER)
            val HEALTH_DOCUMENTS_FOLDER = buildLabel(LabelId("health documents"), type = LabelType.FOLDER)

            val GAME_LABEL = buildLabel(LabelId("game"), type = LabelType.MESSAGE_LABEL)
            val TRAVEL_LABEL = buildLabel(LabelId("travel"), type = LabelType.MESSAGE_LABEL)
            val WORK_LABEL = buildLabel(LabelId("work"), type = LabelType.MESSAGE_LABEL)

            fun allLabelsAndFolders() = ALL_STANDARD_LABELS + listOf(
                WORK_DOCUMENTS_FOLDER,
                FUN_DOCUMENTS_FOLDER,
                HEALTH_DOCUMENTS_FOLDER,
                GAME_LABEL,
                TRAVEL_LABEL,
                WORK_LABEL
            )
        }

        fun buildMessage(
            sender: MessageSender = MessageSender(EMPTY_STRING, EMPTY_STRING, false),
            toRecipients: List<MessageRecipient> = emptyList(),
            ccRecipients: List<MessageRecipient> = emptyList(),
            bccRecipients: List<MessageRecipient> = emptyList(),
            allLabelsIds: Collection<LabelId> = emptyList()
        ) = Message().apply {
            this.messageId = EMPTY_STRING
            this.subject = EMPTY_STRING
            this.sender = sender
            this.toList = toRecipients
            this.ccList = ccRecipients
            this.bccList = bccRecipients
            this.Unread = true
            this.allLabelIDs = allLabelsIds.map { it.id }
        }

        fun buildConversation(
            sender: Correspondent = Correspondent(EMPTY_STRING, EMPTY_STRING),
            allLabelsIds: List<LabelId> = emptyList(),
            labels: List<LabelContext> = allLabelsIds.map(::buildLabelContext),
            messageCount: Int = 0
        ) = Conversation(
            id = EMPTY_STRING,
            subject = EMPTY_STRING,
            senders = listOf(sender),
            receivers = emptyList(),
            messagesCount = messageCount,
            unreadCount = 2,
            attachmentsCount = 0,
            expirationTime = 0,
            labels = labels,
            messages = emptyList()
        )

        fun buildMailboxItemUiModel(
            correspondentsNames: String = EMPTY_STRING,
            lastMessageTimeMs: Long = 0,
            messageCount: Int? = null,
            isStarred: Boolean = false,
            isDraft: Boolean = false,
            isScheduled: Boolean = false,
            messageData: MessageData? = null,
            messageLabels: List<LabelChipUiModel> = emptyList(),
            allLabelsIds: List<LabelId> = emptyList(),
            isProton: Boolean = false
        ) = MailboxItemUiModel(
            itemId = EMPTY_STRING,
            correspondentsNames = correspondentsNames,
            subject = EMPTY_STRING,
            lastMessageTimeMs = lastMessageTimeMs,
            hasAttachments = false,
            isStarred = isStarred,
            isRead = false,
            expirationTime = 0,
            messagesCount = messageCount,
            messageData = messageData,
            messageLabels = messageLabels,
            allLabelsIds = allLabelsIds,
            isDraft = isDraft,
            isScheduled = isScheduled,
            isProton = isProton
        )

        fun buildMessageData() = MessageData(
            location = -1,
            isReplied = false,
            isRepliedAll = false,
            isForwarded = false,
            isInline = false
        )

        fun buildLabelContext(
            id: LabelId,
            contextTime: Long = 0
        ) = LabelContext(
            id = id.id,
            contextNumUnread = 0,
            contextNumMessages = 0,
            contextTime = contextTime,
            contextSize = 0,
            contextNumAttachments = 0
        )

        fun buildLabel(id: LabelId, type: LabelType) = Label(
            id = id,
            name = "label",
            color = "red",
            order = 0,
            type = type,
            path = "label",
            parentId = EMPTY_STRING
        )

        fun buildLabelChipUiModel(id: LabelId) = LabelChipUiModel(
            id = id,
            name = Name(id.id),
            color = 0
        )

        fun buildContactEmail() = ContactEmail(
            contactEmailId = EMPTY_STRING,
            email = EMPTY_STRING,
            name = EMPTY_STRING
        )

        fun buildRecipients(vararg names: String): List<MessageRecipient> =
            names.map { name -> MessageRecipient(name, TEST_EMAIL_ADDRESS) }

        fun LabelChipUiModelMapper.mockUiModelsCreation() {
            every { toUiModels(any()) } answers {
                val labelsArg = firstArg<Collection<Label>>()
                labelsArg.map { buildLabelChipUiModel(it.id) }
            }
        }
    }
}
