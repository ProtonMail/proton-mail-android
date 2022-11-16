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
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.mailbox.presentation.model.MessageData
import ch.protonmail.android.ui.model.LabelChipUiModel
import kotlinx.coroutines.flow.first
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject

class MailboxItemUiModelMapper @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val labelChipUiModelMapper: LabelChipUiModelMapper
) : Mapper<Either<Message, Conversation>, MailboxItemUiModel> {

    fun toUiModel(
        message: Message,
        currentLabelId: LabelId,
        allLabels: Collection<Label>
    ): MailboxItemUiModel = MailboxItemUiModel(
        itemId = checkNotNull(message.messageId) { "Message id is null" },
        correspondentsNames = getCorrespondentsNames(message, currentLabelId),
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
        isDraft = message.isDraft(),
        isScheduled = message.isScheduled,
        isProton = message.sender?.isProton ?: false
    )

    @JvmName("messagesToUiModels")
    fun toUiModels(
        messages: Collection<Message>,
        currentLabelId: LabelId,
        allLabels: Collection<Label>
    ): List<MailboxItemUiModel> =
        messages.map { toUiModel(it, currentLabelId, allLabels) }

    suspend fun toUiModel(
        userId: UserId,
        conversation: Conversation,
        currentLabelId: LabelId,
        allLabels: Collection<Label>
    ) = MailboxItemUiModel(
        itemId = conversation.id,
        correspondentsNames = getCorrespondentsNames(userId, conversation, currentLabelId),
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
        isDraft = conversation.containsSingleDraftMessage(),
        isScheduled = conversation.labels.any { it.id == MessageLocationType.ALL_SCHEDULED.asLabelIdString() },
        isProton = conversation.messages?.any { it.sender.isProton } ?: false
    )

    @JvmName("conversationsToUiModels")
    suspend fun toUiModels(
        userId: UserId,
        conversations: Collection<Conversation>,
        currentLabelId: LabelId,
        allLabels: Collection<Label>
    ): List<MailboxItemUiModel> = conversations.map { toUiModel(userId, it, currentLabelId, allLabels) }

    private suspend fun getCorrespondentsNames(
        userId: UserId,
        conversation: Conversation,
        currentLabelId: LabelId
    ): String =
        if (isDraftOrSentLabel(currentLabelId)) conversation.receivers.joinToString { it.name }
        else toDisplayNamesFromContacts(userId, conversation.senders).joinToString()

    private fun getCorrespondentsNames(message: Message, currentLabelId: LabelId): String =
        if (isDraftOrSentLabel(currentLabelId)) {
            toDisplayNamesFromContacts(
                allRecipients = message.toList + message.ccList + message.bccList
            ).joinToString()
        } else {
            toDisplayNameFromContacts(
                sender = message.sender,
                senderDisplayName = message.senderDisplayName
            )
        }

    private fun isDraftOrSentLabel(currentLabelId: LabelId): Boolean {
        val sentAndDraftLabels = listOf(
            MessageLocationType.DRAFT,
            MessageLocationType.ALL_DRAFT,
            MessageLocationType.SENT,
            MessageLocationType.ALL_SENT,
            MessageLocationType.ALL_SCHEDULED
        ).map { it.asLabelId() }

        return currentLabelId in sentAndDraftLabels
    }

    private fun toDisplayNameFromContacts(
        sender: MessageSender?,
        senderDisplayName: String?
    ): String {
        checkNotNull(sender) { "Message has null sender" }
        return sender.name?.takeIfNotBlank()
            ?: senderDisplayName?.takeIfNotBlank()
            ?: sender.emailAddress?.takeIfNotBlank()
            ?: EMPTY_STRING
    }

    @JvmName("toDisplayNamesFromContacts_MessageRecipient")
    private fun toDisplayNamesFromContacts(
        allRecipients: Collection<MessageRecipient>
    ): List<String> =
        allRecipients.map { it.name.takeIfNotBlank() ?: it.emailAddress }

    private suspend fun toDisplayNamesFromContacts(
        userId: UserId,
        correspondents: Collection<Correspondent>
    ): List<String> {
        val contacts = contactsRepository.findContactsByEmail(userId, correspondents.map { it.address }).first()
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
