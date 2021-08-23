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

import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.arch.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Maps from [ConversationApiModel] to [ConversationDatabaseModel]
 */
class ConversationApiModelToConversationDatabaseModelMapper @Inject constructor(
    private val messageSenderMapper: CorrespondentApiModelToMessageSenderMapper,
    private val messageRecipientMapper: CorrespondentApiModelToMessageRecipientMapper,
    private val labelMapper: LabelContextApiModelToLabelContextDatabaseModelMapper
) : Mapper<ConversationApiModel, ConversationDatabaseModel> {

    fun ConversationApiModel.toDatabaseModel(userId: UserId) = ConversationDatabaseModel(
        id = id,
        order = order,
        userId = userId.id,
        subject = subject,
        senders = senders.map(messageSenderMapper) { it.toDatabaseModel() },
        recipients = recipients.map(messageRecipientMapper) { it.toDatabaseModel() },
        numMessages = numMessages,
        numUnread = numUnread,
        numAttachments = numAttachments,
        expirationTime = expirationTime,
        size = size,
        labels = labels.map(labelMapper) { it.toDatabaseModel() }
    )
}
