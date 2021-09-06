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

package ch.protonmail.android.mailbox.data.remote

import ch.protonmail.android.details.data.remote.model.ConversationResponse
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import ch.protonmail.android.mailbox.data.remote.model.ConversationsActionResponses
import ch.protonmail.android.mailbox.data.remote.model.ConversationsCountsResponse
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetOneConversationParameters

interface ConversationApiSpec {

    suspend fun fetchConversations(
        params: GetAllConversationsParameters
    ): ConversationsResponse

    suspend fun fetchConversation(
        params: GetOneConversationParameters
    ): ConversationResponse

    suspend fun fetchConversationsCounts(userId: UserId): ConversationsCountsResponse

    suspend fun markConversationsRead(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses

    suspend fun markConversationsUnread(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses

    suspend fun labelConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses

    suspend fun unlabelConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses

    suspend fun deleteConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ): ConversationsActionResponses
}
