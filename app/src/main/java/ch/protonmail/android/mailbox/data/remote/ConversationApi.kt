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

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.segments.BaseApi
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters

class ConversationApi(private val service: ConversationService) : BaseApi(), ConversationApiSpec {

    override suspend fun fetchConversations(params: GetConversationsParameters) =
        service.fetchConversations(
            userIdTag = UserIdTag(params.userId),
            page = params.page,
            pageSize = params.pageSize,
            labelId = params.labelId,
            sort = params.sortBy.stringValue,
            desc = params.sortDirection.intValue,
            begin = params.begin,
            end = params.end,
            beginId = params.beginId,
            endId = params.endId
        )

    override suspend fun fetchConversation(
        userId: UserId,
        conversationId: String
    ) = service.fetchConversation(conversationId, userIdTag = UserIdTag(userId))

    override suspend fun markConversationsRead(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.markConversationsRead(conversationIds, userIdTag = UserIdTag(userId))

    override suspend fun markConversationsUnread(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.markConversationsUnread(conversationIds, userIdTag = UserIdTag(userId))

    override suspend fun labelConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.labelConversations(conversationIds, userIdTag = UserIdTag(userId))

    override suspend fun unlabelConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.unlabelConversations(conversationIds, userIdTag = UserIdTag(userId))

    override suspend fun deleteConversations(
        conversationIds: ConversationIdsRequestBody,
        userId: UserId
    ) = service.deleteConversations(conversationIds, userIdTag = UserIdTag(userId))
}
