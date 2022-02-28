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

package ch.protonmail.android.details.data

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.mailbox.data.mapper.CorrespondentToMessageRecipientMapper
import ch.protonmail.android.mailbox.data.mapper.CorrespondentToMessageSenderMapper
import ch.protonmail.android.mailbox.data.mapper.MessageRecipientToCorrespondentMapper
import ch.protonmail.android.mailbox.data.mapper.MessageSenderToCorrespondentMapper
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import me.proton.core.domain.arch.map
import me.proton.core.util.kotlin.invoke

internal fun Message.toDomainModel(): MessageDomainModel {
    val senderToCorrespondentMapper = MessageSenderToCorrespondentMapper()
    val recipientToCorrespondentMapper = MessageRecipientToCorrespondentMapper()
    return MessageDomainModel(
        id = messageId.orEmpty(),
        conversationId = conversationId.orEmpty(),
        subject = subject.orEmpty(),
        isUnread = Unread,
        sender = senderToCorrespondentMapper { toDomainModelOrEmpty(sender) },
        receivers = toList.map(recipientToCorrespondentMapper) { toDomainModel(it) },
        time = time,
        attachmentsCount = numAttachments,
        expirationTime = expirationTime,
        isReplied = isReplied ?: false,
        isRepliedAll = isRepliedAll ?: false,
        isForwarded = isForwarded ?: false,
        ccReceivers = ccList.map(recipientToCorrespondentMapper) { toDomainModel(it) },
        bccReceivers = bccList.map(recipientToCorrespondentMapper) { toDomainModel(it) },
        labelsIds = allLabelIDs
    )
}

internal fun MessageDomainModel.toDbModel(): Message {
    val correspondentToSenderMapper = CorrespondentToMessageSenderMapper()
    val correspondentToRecipientMapper = CorrespondentToMessageRecipientMapper()
    return Message(
        messageId = id,
        conversationId = conversationId,
        subject = subject,
        Unread = isUnread,
        sender = correspondentToSenderMapper { toDatabaseModel(sender) },
        toList = receivers.map(correspondentToRecipientMapper) { toDatabaseModel(it) },
        time = time,
        numAttachments = attachmentsCount,
        expirationTime = expirationTime,
        isReplied = isReplied,
        isRepliedAll = isRepliedAll,
        isForwarded = isForwarded,
        ccList = ccReceivers.map(correspondentToRecipientMapper) { toDatabaseModel(it) },
        bccList = bccReceivers.map(correspondentToRecipientMapper) { toDatabaseModel(it) },
        allLabelIDs = labelsIds
    )
}

/**
 * Converts a list of messages from db to a list of domain message model
 */
internal fun List<Message>.toDomainModelList() =
    map { message -> message.toDomainModel() }

internal fun List<MessageDomainModel>.toDbModelList() =
    map { message -> message.toDbModel() }

