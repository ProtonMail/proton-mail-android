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

    fun toDatabaseModel(
        conversationApiModel: ConversationApiModel,
        userId: UserId
    ) = ConversationDatabaseModel(
        id = conversationApiModel.id,
        order = conversationApiModel.order,
        userId = userId.id,
        subject = conversationApiModel.subject,
        senders = conversationApiModel.senders.map(messageSenderMapper) { toDatabaseModel(it) },
        recipients = conversationApiModel.recipients.map(messageRecipientMapper) { toDatabaseModel(it) },
        numMessages = conversationApiModel.numMessages,
        numUnread = conversationApiModel.numUnread,
        numAttachments = conversationApiModel.numAttachments,
        expirationTime = conversationApiModel.expirationTime,
        size = conversationApiModel.size,
        labels = conversationApiModel.labels.map(labelMapper) { toDatabaseModel(it) }
    )

    fun toDatabaseModels(
        conversationApiModels: List<ConversationApiModel>,
        userId: UserId
    ): List<ConversationDatabaseModel> =
        conversationApiModels.map { toDatabaseModel(it, userId) }
}
