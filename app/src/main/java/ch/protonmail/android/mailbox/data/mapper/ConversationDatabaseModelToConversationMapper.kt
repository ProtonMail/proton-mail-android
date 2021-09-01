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

import ch.protonmail.android.data.ProtonStoreMapper
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.MessageDomainModel
import me.proton.core.domain.arch.map
import javax.inject.Inject

/**
 * [ProtonStoreMapper] that maps from [ConversationDatabaseModel] to [Conversation] Domain model
 */
class ConversationDatabaseModelToConversationMapper @Inject constructor(
    private val senderMapper: MessageSenderToCorrespondentMapper,
    private val recipientMapper: MessageRecipientToCorrespondentMapper,
    private val labelMapper: LabelContextDatabaseModelToLabelContextMapper
) : ProtonStoreMapper<GetAllConversationsParameters, ConversationDatabaseModel, Conversation> {

    override fun ConversationDatabaseModel.toOut(key: GetAllConversationsParameters): Conversation =
        toDomainModel(conversation = this, messages = null)

    fun toDomainModel(
        conversation: ConversationDatabaseModel,
        messages: List<MessageDomainModel>?
    ) = Conversation(
        id = conversation.id,
        subject = conversation.subject,
        senders = conversation.senders.map(senderMapper) { it.toDomainModel() },
        receivers = conversation.recipients.map(recipientMapper) { it.toDomainModel() },
        messagesCount = conversation.numMessages,
        unreadCount = conversation.numUnread,
        attachmentsCount = conversation.numAttachments,
        expirationTime = conversation.expirationTime,
        labels = conversation.labels.map(labelMapper) { it.toDomainModel() },
        messages = messages
    )

    fun toDomainModels(conversations: List<ConversationDatabaseModel>): List<Conversation> =
        conversations.map { toDomainModel(it, messages = null) }
}
