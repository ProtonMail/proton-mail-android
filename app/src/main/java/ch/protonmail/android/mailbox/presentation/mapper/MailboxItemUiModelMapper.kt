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

import arrow.core.Either
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.mapper.LabelChipUiModelMapper
import ch.protonmail.android.mailbox.data.mapper.MessageRecipientToCorrespondentMapper
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.mailbox.presentation.model.MessageData
import ch.protonmail.android.ui.model.LabelChipUiModel
import kotlinx.coroutines.flow.first
import me.proton.core.domain.arch.Mapper
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.takeIfNotBlank
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

class MailboxItemUiModelMapper @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val messageRecipientToCorrespondentMapper: MessageRecipientToCorrespondentMapper,
    private val labelChipUiModelMapper: LabelChipUiModelMapper
) : Mapper<Either<Message, Conversation>, MailboxItemUiModel> {

    suspend fun toUiModel(message: Message, allLabels: Collection<Label>): MailboxItemUiModel = MailboxItemUiModel(
        itemId = checkNotNull(message.messageId) { "Message id is null" },
        senderName = toDisplayNameFromContacts(message.sender, message.senderDisplayName),
        subject = checkNotNull(message.subject) { "Message subject is null" },
        lastMessageTimeMs = message.timeMs,
        hasAttachments = message.numAttachments > 0,
        isStarred = message.isStarred == true,
        isRead = message.isRead,
        expirationTime = message.expirationTime,
        messagesCount = null,
        messageData = buildMessageData(message),
        messageLabels = message.buildLabelChipFromMessageLabels(allLabels),
        allLabelsIds = message.allLabelsIds(),
        recipients = toDisplayNamesFromContacts(message.toList, message.ccList, message.bccList).joinToString(),
        isDraft = message.isDraft()
    )

    suspend fun toUiModels(messages: Collection<Message>, allLabels: Collection<Label>): List<MailboxItemUiModel> =
        messages.map { toUiModel(it, allLabels) }

    suspend fun toUiModel(
        conversation: Conversation,
        currentLabelId: LabelId,
        allLabels: Collection<Label>
    ) = MailboxItemUiModel(
        itemId = conversation.id,
        senderName = toDisplayNamesFromContacts(conversation.senders).joinToString(),
        subject = conversation.subject,
        lastMessageTimeMs = conversation.lastMessageTimeMs(currentLabelId),
        hasAttachments = conversation.attachmentsCount > 0,
        isStarred = conversation.labels.any { it.id == MessageLocationType.STARRED.asLabelIdString() },
        isRead = conversation.unreadCount == 0,
        expirationTime = conversation.expirationTime,
        messagesCount = conversation.messagesCount.takeIf { it >= MIN_MESSAGES_TO_SHOW_COUNT },
        messageData = null,
        messageLabels = conversation.buildLabelChipFromMessageLabels(allLabels),
        allLabelsIds = conversation.allLabelsIds(),
        recipients = conversation.receivers.joinToString { it.name },
        isDraft = conversation.containsSingleDraftMessage()
    )

    suspend fun toUiModels(
        conversations: Collection<Conversation>,
        currentLabelId: LabelId,
        allLabels: Collection<Label>
    ): List<MailboxItemUiModel> = conversations.map { toUiModel(it, currentLabelId, allLabels) }

    private suspend fun toDisplayNameFromContacts(sender: MessageSender?, senderDisplayName: String?): String {
        checkNotNull(sender) { "Message has null sender" }
        val emailAddress = requireNotNull(sender.emailAddress) { "MessageSender has null emailAddress" }
        val contactEmail = contactsRepository.findContactEmailByEmail(emailAddress)
        return contactEmail?.name?.takeIfNotBlank()
            ?: senderDisplayName?.takeIfNotBlank()
            ?: sender.name?.takeIfNotBlank()
            ?: sender.emailAddress ?: EMPTY_STRING
    }

    @JvmName("toDisplayNamesFromContacts_MessageRecipient")
    private suspend fun toDisplayNamesFromContacts(
        toRecipients: Collection<MessageRecipient>,
        ccRecipients: Collection<MessageRecipient>,
        bccRecipients: Collection<MessageRecipient>
    ): List<String> {
        val recipients = toRecipients.takeIfNotEmpty()
            ?: ccRecipients.takeIfNotEmpty()
            ?: bccRecipients
        return toDisplayNamesFromContacts(messageRecipientToCorrespondentMapper.toDomainModels(recipients))
    }

    private suspend fun toDisplayNamesFromContacts(correspondents: Collection<Correspondent>): List<String> {
        val contacts = contactsRepository.findContactsByEmail(correspondents.map { it.address }).first()
        return correspondents
            .associateWith { correspondent -> contacts.find { it.email == correspondent.address } }
            .map { (correspondent, contactEmail) ->
                contactEmail?.name?.takeIfNotBlank()
                    ?: correspondent.name.takeIfNotBlank()
                    ?: correspondent.address
            }
    }

    private fun buildMessageData(message: Message) = MessageData(
        location = message.location,
        isReplied = message.isReplied == true,
        isRepliedAll = message.isRepliedAll == true,
        isForwarded = message.isForwarded == true,
        isInline = message.isInline,
    )

    private fun Conversation.lastMessageTimeMs(currentLabelId: LabelId) = labels
        .find { it.id == currentLabelId.id }
        ?.let { it.contextTime * SEC_TO_MS_RATIO }
        ?: 0

    private fun Conversation.buildLabelChipFromMessageLabels(allLabels: Collection<Label>): List<LabelChipUiModel> {
        val conversationLabels: List<Label> =
            allLabels.filter { it.type == LabelType.MESSAGE_LABEL && it.id in allLabelsIds() }
        return labelChipUiModelMapper.toUiModels(conversationLabels)
    }

    private fun Message.buildLabelChipFromMessageLabels(allLabels: Collection<Label>): List<LabelChipUiModel> {
        val messageLabels: List<Label> =
            allLabels.filter { it.type == LabelType.MESSAGE_LABEL && it.id in allLabelsIds() }
        return labelChipUiModelMapper.toUiModels(messageLabels)
    }

    private fun Conversation.allLabelsIds(): List<LabelId> =
        labels.map { LabelId(it.id) }

    private fun Message.allLabelsIds(): List<LabelId> =
        allLabelIDs.map { LabelId(it) }

    private fun Conversation.containsSingleDraftMessage() = messagesCount == 1 && labels.any { labelContext ->
        labelContext.id in DRAFT_LABELS_IDS.map { it.asLabelIdString() }
    }

    companion object {

        private const val MIN_MESSAGES_TO_SHOW_COUNT = 2
        private const val SEC_TO_MS_RATIO = 1_000

        private val DRAFT_LABELS_IDS = arrayOf(MessageLocationType.DRAFT, MessageLocationType.ALL_DRAFT)
    }
}
