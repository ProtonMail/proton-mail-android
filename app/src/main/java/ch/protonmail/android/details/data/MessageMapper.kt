/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonMail.
 *
 * ProtonMail is free software= you can redistribute it and/or modify
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
 * along with ProtonMail. If not, see https=//www.gnu.org/licenses/.
 */

package ch.protonmail.android.details.data

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.mailbox.data.recipientToCorespondent
import ch.protonmail.android.mailbox.domain.model.Correspondent
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel

internal fun Message.toDomainModel() = MessageDomainModel(
    id = messageId.orEmpty(),
    conversationId = conversationId.orEmpty(),
    subject = subject.orEmpty(),
    isUnread = Unread,
    sender = Correspondent(sender?.name.orEmpty(), sender?.emailAddress.orEmpty()),
    receivers = toList.recipientToCorespondent(),
    time = time,
    attachmentsCount = numAttachments,
    expirationTime = expirationTime,
    isReplied = isReplied ?: false,
    isRepliedAll = isRepliedAll ?: false,
    isForwarded = isForwarded ?: false,
    ccReceivers = ccList.recipientToCorespondent(),
    bccReceivers = bccList.recipientToCorespondent(),
    labelsIds = allLabelIDs
)

/**
 * Converts a list of messages from db to a list of domain message model
 */
internal fun List<Message>.toDomainModelList() =
    map { message -> message.toDomainModel() }


