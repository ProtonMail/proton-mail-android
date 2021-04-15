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

package ch.protonmail.android.mailbox.data

import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.data.remote.model.CorrespondentApiModel
import ch.protonmail.android.mailbox.data.remote.model.LabelContextApiModel
import ch.protonmail.android.mailbox.domain.Conversation
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.LabelContext


internal fun ConversationApiModel.toLocal(userId: String) = ConversationDatabaseModel(
    id = id,
    order = order,
    userId = userId,
    subject = subject,
    senders = senders.toMessageSender(),
    recipients = recipients.toMessageRecipient(),
    numMessages = numMessages,
    numUnread = numUnread,
    numAttachments = numAttachments,
    expirationTime = expirationTime,
    size = size,
    labels = labels.toLabelContextDatabaseModel()
)

internal fun ConversationDatabaseModel.toDomainModel() = Conversation(
    id = id,
    subject = subject,
    senders = senders.senderToCorespondent(),
    receivers = recipients.recipientToCorespondent(),
    messagesCount = numMessages,
    unreadCount = numUnread,
    attachmentsCount = numAttachments,
    expirationTime = expirationTime,
    labels = labels.toLabelContextDomainModel()
)

/**
 * Converts a correspondent api model list to sender db model list
 */
internal fun List<CorrespondentApiModel>.toMessageSender() =
    map { sender -> MessageSender(sender.name, sender.address) }

/**
 * Converts a correspondent api model list to recipient db model list
 */
internal fun List<CorrespondentApiModel>.toMessageRecipient() =
    map { recipient ->
        MessageRecipient(recipient.name, recipient.address)
    }

/**
 * Converts a sender db model list to corespondent domain model list
 */
internal fun List<MessageSender>.senderToCorespondent() =
    map { sender ->
        Correspondent(sender.name ?: "", sender.emailAddress ?: "")
    }

/**
 * Converts a recipients db model list to corespondent domain model list
 */
internal fun List<MessageRecipient>.recipientToCorespondent() =
    map { recipient ->
        Correspondent(recipient.name ?: "", recipient.emailAddress ?: "")
    }

/**
 * Converts a label context api model list to label context database model list
 */
internal fun List<LabelContextApiModel>.toLabelContextDatabaseModel() =
    map { label ->
        LabelContextDatabaseModel(
            label.id,
            label.contextNumUnread,
            label.contextNumMessages,
            label.contextTime,
            label.contextSize,
            label.contextNumAttachments
        )
    }

/**
 * Converts a label context api model list to label context domain model list
 */internal fun List<LabelContextDatabaseModel>.toLabelContextDomainModel() =
    map { label ->
        LabelContext(
            label.id,
            label.contextNumUnread,
            label.contextNumMessages,
            label.contextTime,
            label.contextSize,
            label.contextNumAttachments
        )
    }


/**
 * Converts a response conversations list to a list of local conversation modal
 */
internal fun List<ConversationApiModel>.toListLocal(userId: String) =
    map { conversation -> conversation.toLocal(userId) }

/**
 * Converts a list of conversations from db to a list of domain conversation model
 */
internal fun List<ConversationDatabaseModel>.toDomainModelList() =
    map { conversation -> conversation.toDomainModel() }


