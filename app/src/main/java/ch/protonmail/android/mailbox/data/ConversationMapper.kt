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

import ch.protonmail.android.mailbox.data.local.model.ConversationEntity
import ch.protonmail.android.mailbox.data.remote.model.ConversationRemote
import ch.protonmail.android.mailbox.domain.Conversation


internal fun ConversationRemote.toLocal(userId: String) = ConversationEntity(
    id = id,
    order = order,
    userId = userId,
    subject = subject,
//        listOf(),
//        listOf(),
    numMessages = numMessages,
    numUnread = numUnread,
    numAttachments = numAttachments,
    expirationTime = expirationTime,
//        EMPTY_STRING,
    size = size,
//        labelIds,
//        listOf()
)

internal fun ConversationEntity.toDomainModel() = Conversation(
    id = id,
    subject = subject,
    listOf(),
    listOf(),
    messagesCount = numMessages,
    unreadCount = numUnread,
    attachmentsCount = numAttachments,
    expirationTime = expirationTime
//        EMPTY_STRING,
//        labelIds,
//        listOf()
)


/**
 * Converts the response list to a list of local conversation modal
 */
internal fun List<ConversationRemote>.toListLocal(userId: String) =
    map { conversation -> conversation.toLocal(userId) }

internal fun List<ConversationEntity>.toDomainModelList() =
    map { conversation -> conversation.toDomainModel() }


