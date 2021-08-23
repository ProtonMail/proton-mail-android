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

package ch.protonmail.android.mailbox.data.mapper

import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import ch.protonmail.android.mailbox.domain.model.Conversation
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.arch.map
import javax.inject.Inject

/**
 * Maps from [ConversationApiModel] to [Conversation] Domain model
 */
class ConversationApiModelToConversationMapper @Inject constructor(
    private val correspondentMapper: CorrespondentApiModelToCorrespondentMapper,
    private val labelsMapper: LabelContextApiModelToLabelContextMapper
) : Mapper<ConversationApiModel, Conversation> {

    fun ConversationApiModel.toDomainModel() = Conversation(
        id = id,
        subject = subject,
        senders = senders.map(correspondentMapper) { it.toDomainModel() },
        receivers = recipients.map(correspondentMapper) { it.toDomainModel() },
        messagesCount = numMessages,
        unreadCount = numUnread,
        attachmentsCount = numAttachments,
        expirationTime = expirationTime,
        labels = labels.map(labelsMapper) { it.toDomainModel() },
        messages = null
    )
}
