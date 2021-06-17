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
import ch.protonmail.android.api.segments.RetrofitConstants
import ch.protonmail.android.details.data.remote.model.ConversationResponse
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import ch.protonmail.android.mailbox.data.remote.model.ConversationsActionResponses
import ch.protonmail.android.mailbox.data.remote.model.ConversationsResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface ConversationService {

    @GET("mail/v4/conversations")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun fetchConversations(
        @Query("End") end: Long?,
        @Query("PageSize") pageSize: Int?,
        @Query("LabelID") labelId: String?,
        @Query("Sort") sort: String = "Time",
        @Query("Desc") desc: Int = 1,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationsResponse

    @GET("mail/v4/conversations/{conversationId}")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun fetchConversation(
        @Path("conversationId") conversationId: String,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationResponse

    @PUT("mail/v4/conversations/read")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun markConversationsRead(
        @Body conversationIds: ConversationIdsRequestBody
    ): ConversationsActionResponses

    @PUT("mail/v4/conversations/unread")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun markConversationsUnread(
        @Body conversationIds: ConversationIdsRequestBody
    ): ConversationsActionResponses

    @PUT("mail/v4/conversations/label")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun labelConversations(
        @Body conversationIds: ConversationIdsRequestBody,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationsActionResponses

    @PUT("mail/v4/conversations/unlabel")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun unlabelConversations(
        @Body conversationIds: ConversationIdsRequestBody,
        @Tag userIdTag: UserIdTag? = null
    ): ConversationsActionResponses
}
