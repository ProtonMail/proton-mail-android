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
package ch.protonmail.android.api.segments.message

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DeleteResponse
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.api.models.UnreadTotalMessagesResponse
import ch.protonmail.android.api.models.messages.delete.MessageDeleteRequest
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendResponse
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Tag

interface MessageService {

    @GET("mail/v4/messages/count")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchMessagesCount(@Tag userIdTag: UserIdTag): Call<UnreadTotalMessagesResponse>

    @PUT("mail/v4/messages/delete")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun delete(@Body messages: MessageDeleteRequest): DeleteResponse

    @PUT("mail/v4/messages/read")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun read(@Body messageIds: IDList): Call<ResponseBody>

    @PUT("mail/v4/messages/unread")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun unRead(@Body messageIds: IDList): Call<ResponseBody>

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun messages(
        @Query("LabelID") location: Int,
        @Query("Order") order: String,
        @Query("Begin") begin: String,
        @Query("End") end: String
    ): Call<MessagesResponse>

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun messages(
        @Query("LabelID") location: Int,
        @Query("Order") order: String,
        @Query("Begin") begin: String,
        @Query("End") end: String,
        @Tag userIdTag: UserIdTag
    ): Call<MessagesResponse>

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun getMessages(
        @Tag userIdTag: UserIdTag,
        @Query("LabelID") location: String,
        @Query("Order") order: String,
        @Query("Begin") begin: Long?,
        @Query("End") end: Long?
    ): MessagesResponse

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchMessages(
        @Query("LabelID") location: Int,
        @Query("End") unixTime: Long
    ): Call<MessagesResponse>

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun search(
        @Query("Keyword") query: String,
        @Query("Page") page: Int
    ): Call<MessagesResponse>

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun searchByLabel(
        @Query("LabelID") query: String,
        @Query("Page") page: Int
    ): Call<MessagesResponse>

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun getMessagesByLabel(
        @Query("LabelID") query: String,
        @Query("Page") page: Int
    ): MessagesResponse

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun searchByLabel(
        @Query("LabelID") query: String,
        @Query("End") unixTime: Long
    ): Call<MessagesResponse>

    @POST("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun createDraft(@Body draftBody: DraftBody): MessageResponse

    @GET("mail/v4/messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun fetchMessageMetadata(
        @Query("ID") messageId: String,
        @Tag userIdTag: UserIdTag
    ): MessagesResponse

    @PUT("mail/v4/messages/{messageId}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun updateDraft(
        @Path("messageId") messageId: String,
        @Body draftBody: DraftBody,
        @Tag userIdTag: UserIdTag
    ): MessageResponse

    @POST("mail/v4/messages/{messageId}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun sendMessage(
        @Path("messageId") messageId: String,
        @Body message: MessageSendBody,
        @Tag userIdTag: UserIdTag
    ): MessageSendResponse

    @GET("mail/v4/messages/{messageId}")
    @Headers(ACCEPT_HEADER_V1)
    fun fetchMessageDetailsBlocking(@Path("messageId") messageId: String): Call<MessageResponse>

    @GET("mail/v4/messages/{messageId}")
    @Headers(ACCEPT_HEADER_V1)
    suspend fun fetchMessageDetails(
        @Path("messageId") messageId: String,
        @Tag userIdTag: UserIdTag
    ): MessageResponse

    @GET("mail/v4/messages/{messageId}")
    @Headers(ACCEPT_HEADER_V1)
    fun fetchMessageDetailsBlocking(
        @Path("messageId") messageId: String,
        @Tag userIdTag: UserIdTag
    ): Call<MessageResponse>

    @GET("mail/v4/messages/{messageId}")
    @Headers(ACCEPT_HEADER_V1)
    fun messageDetailObservable(@Path("messageId") messageId: String): Observable<MessageResponse>

    @DELETE("mail/v4/messages/empty")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun emptyFolder(@Query("LabelID") labelId: String): Call<ResponseBody>

    @PUT("mail/v4/messages/unlabel")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun unlabelMessages(@Body body: IDList): Call<ResponseBody>

    @PUT("mail/v4/messages/label")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun labelMessages(@Body body: IDList): Call<MoveToFolderResponse>
}
