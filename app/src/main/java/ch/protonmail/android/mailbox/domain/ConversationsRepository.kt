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

package ch.protonmail.android.mailbox.domain

import ch.protonmail.android.core.Constants
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.entity.UserId

interface ConversationsRepository {

    /**
     * Emits a List of [Conversation] when the repository could successfully get conversations from some data source,
     *  or an empty optional when the repository encounters a handled failure getting conversations
     *
     * @param params a model representing the params needed to define which conversations to get
     *
     * @return [LoadMoreFlow] of [DataResult] or [List] of [Conversation]
     * @throws Exception when the repository fails getting conversations for any unhandled reasons
     */
    fun getConversations(params: GetConversationsParameters): LoadMoreFlow<DataResult<List<Conversation>>>

    /**
     * @param conversationId the encrypted id of the conversation to get
     * @param userId the id of the user the conversation to get belongs to
     *
     * @return a Conversation object containing list of messages when the repository could successfully
     * get it from some data source.
     * @return an empty optional when the repository encounters a handled failure getting the given conversation
     * @throws exception when the repository fails getting this conversation for any unhandled reasons
     */
    fun getConversation(userId: UserId, conversationId: String): Flow<DataResult<Conversation>>

    /**
     * @param conversationId the encrypted id of the conversation to find
     * @param userId the id of the user the conversation to find belongs to
     *
     * @return a ConversationDatabaseModel object representing the needed conversation when the
     * repository could successfully get it from some data source.
     * @return an empty optional when the repository encounters a handled failure getting the given conversation
     * @throws exception when the repository fails getting this conversation for any unhandled reasons
     */
    suspend fun findConversation(conversationId: String, userId: UserId): ConversationDatabaseModel?

    /**
     * @param conversations a list representing the conversations we want to save to the local data source
     *
     * @throws exception when the repository fails to insert conversations for any unhandled reasons into local storage
     */
    suspend fun saveConversations(conversations: List<ConversationDatabaseModel>, userId: UserId)

    /**
     * Deletes all the conversations from the [TABLE_CONVERSATIONS] inside the local storage
     */
    suspend fun clearConversations()

    /**
     * Deletes a list of conversations from the [TABLE_CONVERSATIONS] inside the local storage
     */
    suspend fun deleteConversations(conversationIds: List<String>, userId: UserId)

    fun loadMore(params: GetConversationsParameters)

    suspend fun markRead(conversationIds: List<String>, userId: UserId): ConversationsActionResult

    suspend fun markUnread(
        conversationIds: List<String>,
        userId: UserId,
        location: Constants.MessageLocationType,
        locationId: String
    ): ConversationsActionResult

    suspend fun star(conversationIds: List<String>, userId: UserId): ConversationsActionResult

    suspend fun unstar(conversationIds: List<String>, userId: UserId): ConversationsActionResult

    suspend fun moveToFolder(
        conversationIds: List<String>,
        userId: UserId,
        folderId: String
    ): ConversationsActionResult

    suspend fun delete(conversationIds: List<String>, userId: UserId, currentFolderId: String)

    suspend fun label(
        conversationIds: List<String>,
        userId: UserId,
        labelId: String
    ): ConversationsActionResult

    suspend fun unlabel(
        conversationIds: List<String>,
        userId: UserId,
        labelId: String
    ): ConversationsActionResult
}
