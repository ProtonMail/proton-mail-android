/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.mailbox.presentation.mapper

import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.INBOX
import ch.protonmail.android.core.Constants.MessageLocationType.SENT
import ch.protonmail.android.core.Constants.MessageLocationType.STARRED
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.mapper.LabelChipUiModelMapper
import ch.protonmail.android.mailbox.data.mapper.MessageRecipientToCorrespondentMapper
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.LabelContext
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.mailbox.presentation.model.MessageData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.Test
import kotlin.test.assertEquals

class MailboxUiItemMapperTest {

    private val contactsRepository: ContactsRepository = mockk {
        coEvery { findContactEmailByEmail(any()) } returns buildContactEmail()
        every { findContactsByEmail(any()) } returns flowOf(emptyList())
    }
    private val labelChipUiModelMapper: LabelChipUiModelMapper = mockk {
        every { toUiModels(any()) } returns emptyList()
    }
    private val mapper = MailboxUiItemMapper(
        contactsRepository = contactsRepository,
        messageRecipientToCorrespondentMapper = MessageRecipientToCorrespondentMapper(),
        labelChipUiModelMapper = labelChipUiModelMapper
    )

    // region Messages
    @Test
    fun `message is mapped correctly`() = runBlockingTest {
        // given
        val allLabels = listOf(
            buildLabel(INBOX.asLabelId()),
            buildLabel(ALL_MAIL.asLabelId()),
            buildLabel(DRAFT.asLabelId())
        )
        val messageLabels = listOf(
            buildLabel(INBOX.asLabelId()),
            buildLabel(ALL_MAIL.asLabelId())
        )
        val message = Message().apply {
            messageId = TEST_ITEM_ID
            sender = MessageSender(TEST_SENDER_NAME, TEST_SENDER_ADDRESS)
            subject = TEST_SUBJECT
            time = TEST_MESSAGE_TIME_SEC
            numAttachments = 1
            isStarred = true
            Unread = true
            expirationTime = TEST_EXPIRATION_TIME
            allLabelIDs = messageLabels.map { it.id.id }
            toList = listOf(MessageRecipient(TEST_RECIPIENT_NAME, TEST_RECIPIENT_ADDRESS))
            location = INBOX.messageLocationTypeValue
            isReplied = true
            isRepliedAll = false
            isForwarded = false
            isInline = false
        }
        val expected = MailboxUiItem(
            itemId = TEST_ITEM_ID,
            senderName = TEST_SENDER_NAME,
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
            labels = emptyList(),
            recipients = TEST_RECIPIENT_NAME,
            isDraft = false
        )

        // when
        val result = mapper.toUiItem(message, allLabels = allLabels)

        // then
        assertEquals(expected, result)
        verify { labelChipUiModelMapper.toUiModels(messageLabels) }
    }

    @Test
    fun `message is draft if labels contain DRAFT or ALL_DRAFT`() = runBlockingTest {
        // given
        val message = buildMessage()
        val expected = buildMailboxUiItem(messageData = buildMessageData())

        // when
        val result = mapper.toUiItem(message, allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message sender has name from correspondent address`() = runBlockingTest {
        // given
        val sender = MessageSender(EMPTY_STRING, TEST_EMAIL_ADDRESS)
        val message = buildMessage(sender)
        val expected = buildMailboxUiItem(senderName = sender.emailAddress!!, messageData = buildMessageData())

        // when
        val result = mapper.toUiItem(message, allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message sender has name from correspondent name`() = runBlockingTest {
        // given
        val sender = MessageSender(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS)
        val message = buildMessage(sender)
        val expected = buildMailboxUiItem(senderName = sender.name!!, messageData = buildMessageData())

        // when
        val result = mapper.toUiItem(message, allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `message sender has name from contact name`() = runBlockingTest {
        // given
        val sender = MessageSender(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS)
        coEvery { contactsRepository.findContactEmailByEmail(any()) } returns
            ContactEmail(EMPTY_STRING, TEST_EMAIL_ADDRESS, TEST_CONTACT_NAME)
        val message = buildMessage(sender)
        val expected = buildMailboxUiItem(senderName = TEST_CONTACT_NAME, messageData = buildMessageData())

        // when
        val result = mapper.toUiItem(message, allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }
    // endregion

    // region Conversations

    @Test
    fun `conversation is mapped correctly`() = runBlockingTest {
        // given
        val currentLabelId = SENT.asLabelId()
        val allLabels = listOf(
            buildLabel(INBOX.asLabelId()),
            buildLabel(ALL_MAIL.asLabelId()),
            buildLabel(DRAFT.asLabelId())
        )
        val conversationLabels = listOf(
            buildLabel(INBOX.asLabelId()),
            buildLabel(ALL_MAIL.asLabelId())
        )
        val conversation = Conversation(
            id = TEST_ITEM_ID,
            subject = TEST_SUBJECT,
            senders = listOf(Correspondent(TEST_SENDER_NAME, TEST_SENDER_ADDRESS)),
            receivers = listOf(Correspondent(TEST_RECIPIENT_NAME, TEST_RECIPIENT_ADDRESS)),
            messagesCount = 2,
            unreadCount = 1,
            attachmentsCount = 3,
            expirationTime = TEST_EXPIRATION_TIME,
            labels = conversationLabels.map { buildLabelContext(it.id) },
            messages = emptyList()
        )
        val expected = MailboxUiItem(
            itemId = TEST_ITEM_ID,
            senderName = TEST_SENDER_NAME,
            subject = TEST_SUBJECT,
            lastMessageTimeMs = 0,
            hasAttachments = true,
            isStarred = false,
            isRead = false,
            expirationTime = TEST_EXPIRATION_TIME,
            messagesCount = 2,
            messageData = null,
            labels = emptyList(),
            recipients = TEST_RECIPIENT_NAME,
            isDraft = false
        )

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = currentLabelId, allLabels = allLabels)

        // then
        assertEquals(expected, result)
        verify { labelChipUiModelMapper.toUiModels(conversationLabels) }
    }

    @Test
    fun `conversation is starred if labels contain starred label id`() = runBlockingTest {
        // given
        val conversation = buildConversation(
            labels = listOf(buildLabelContext(id = STARRED.asLabelId()))
        )
        val expected = buildMailboxUiItem(isStarred = true)

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation sender has name from correspondent address`() = runBlockingTest {
        // given
        val sender = Correspondent(EMPTY_STRING, TEST_EMAIL_ADDRESS)
        val conversation = buildConversation(sender)
        val expected = buildMailboxUiItem(senderName = sender.address)

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation sender has name from correspondent name`() = runBlockingTest {
        // given
        val sender = Correspondent(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS)
        val conversation = buildConversation(sender)
        val expected = buildMailboxUiItem(senderName = sender.name)

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation sender has name from contact name`() = runBlockingTest {
        // given
        val sender = Correspondent(TEST_CORRESPONDENT_NAME, TEST_EMAIL_ADDRESS)
        every { contactsRepository.findContactsByEmail(any()) } returns flowOf(
            listOf(ContactEmail(EMPTY_STRING, TEST_EMAIL_ADDRESS, TEST_CONTACT_NAME))
        )
        val conversation = buildConversation(sender)
        val expected = buildMailboxUiItem(senderName = TEST_CONTACT_NAME)

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation is draft when it has only one draft message`() = runBlockingTest {
        // given
        val conversation = buildConversation(
            messageCount = 1,
            labels = listOf(
                buildLabelContext(ALL_DRAFT.asLabelId()),
                buildLabelContext(DRAFT.asLabelId())
            )
        )
        val expected = buildMailboxUiItem(isDraft = true)

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation is not draft when it has more than one draft message`() = runBlockingTest {
        // given
        val conversation = buildConversation(
            messageCount = 2,
            labels = listOf(
                buildLabelContext(ALL_DRAFT.asLabelId()),
                buildLabelContext(DRAFT.asLabelId())
            )
        )
        val expected = buildMailboxUiItem(isDraft = false, messageCount = 2)

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
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
        val expected = buildMailboxUiItem(lastMessageTimeMs = TEST_MESSAGE_TIME_MS, messageCount = 2)

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = SENT.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `conversation message count is null when below 2, so it won't be displayed`() = runBlockingTest {
        // given
        val conversation = buildConversation(messageCount = 1)
        val expected = buildMailboxUiItem(messageCount = null)

        // when
        val result = mapper.toUiItem(conversation, currentLabelId = INBOX.asLabelId(), allLabels = emptyList())

        // then
        assertEquals(expected, result)
    }
    // endregion

    companion object TestData {

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

        fun buildMessage(
            sender: MessageSender = MessageSender(EMPTY_STRING, EMPTY_STRING)
        ) = Message().apply {
            this.messageId = EMPTY_STRING
            this.subject = EMPTY_STRING
            this.sender = sender
            this.Unread = true
        }

        fun buildConversation(
            sender: Correspondent = Correspondent(EMPTY_STRING, EMPTY_STRING),
            labels: List<LabelContext> = emptyList(),
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

        fun buildMailboxUiItem(
            senderName: String = EMPTY_STRING,
            lastMessageTimeMs: Long = 0,
            messageCount: Int? = null,
            isStarred: Boolean = false,
            isDraft: Boolean = false,
            messageData: MessageData? = null
        ) = MailboxUiItem(
            itemId = EMPTY_STRING,
            senderName = senderName,
            subject = EMPTY_STRING,
            lastMessageTimeMs = lastMessageTimeMs,
            hasAttachments = false,
            isStarred = isStarred,
            isRead = false,
            expirationTime = 0,
            messagesCount = messageCount,
            messageData = messageData,
            labels = emptyList(),
            recipients = EMPTY_STRING,
            isDraft = isDraft
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
        
        fun buildLabel(id: LabelId) = Label(
            id = id,
            name = "label",
            color = "red",
            order = 0,
            type = LabelType.MESSAGE_LABEL,
            path = "label",
            parentId = EMPTY_STRING
        )

        fun buildContactEmail() = ContactEmail(
            contactEmailId = EMPTY_STRING,
            email = EMPTY_STRING,
            name = EMPTY_STRING
        )
    }
}
