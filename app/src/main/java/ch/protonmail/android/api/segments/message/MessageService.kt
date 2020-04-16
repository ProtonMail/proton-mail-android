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

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.api.models.NewMessage
import ch.protonmail.android.api.models.UnreadTotalMessagesResponse
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendResponse
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import io.reactivex.Observable
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface MessageService {

    @GET("messages/count")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchMessagesCount(@Tag retrofitTag: RetrofitTag): Call<UnreadTotalMessagesResponse>

    @PUT("messages/delete")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun delete(@Body messageIds: IDList): Call<ResponseBody>

    @PUT("messages/read")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun read(@Body messageIds: IDList): Call<ResponseBody>

    @PUT("messages/unread")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun unRead(@Body messageIds: IDList): Call<ResponseBody>

    @GET("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun messages(@Query("LabelID") location: Int,
                 @Query("Order") order: String,
                 @Query("Begin") begin: String,
                 @Query("End") end: String): Call<MessagesResponse>

    @GET("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun messages(@Query("LabelID") location: Int,
                 @Query("Order") order: String,
                 @Query("Begin") begin: String,
                 @Query("End") end: String,
                 @Tag retrofitTag: RetrofitTag): Call<MessagesResponse>

    @GET("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchMessages(@Query("LabelID") location: Int,
                      @Query("End") unixTime: Long): Call<MessagesResponse>

    @GET("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchStarredMessages(@Query("Starred") starred: Int,
                             @Query("End") unixTime: Long): Call<MessagesResponse>

    @GET("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun search(@Query("Keyword") query: String,
               @Query("Page") page: Int): Call<MessagesResponse>

    @GET("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun searchByLabel(@Query("LabelID") query: String,
                      @Query("Page") page: Int): Call<MessagesResponse>

    @GET("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun searchByLabel(@Query("LabelID") query: String,
                      @Query("End") unixTime: Long): Call<MessagesResponse>

    @POST("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun createDraft(@Body newMessage: NewMessage): Call<MessageResponse>

    @GET("messages")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchSingleMessageMetadata(@Query("ID") messageId: String): Call<MessagesResponse>

    @PUT("messages/{messageId}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateDraft(@Path("messageId") messageId: String,
                    @Body newMessage: NewMessage, @Tag retrofitTag: RetrofitTag): Call<MessageResponse>

    @POST("messages/{messageId}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun sendMessage(@Path("messageId") messageId: String,
                    @Body message: MessageSendBody, @Tag retrofitTag: RetrofitTag): Call<MessageSendResponse>

    @GET("messages/{messageId}")
    @Headers(ACCEPT_HEADER_V1)
    fun messageDetail(@Path("messageId") messageId: String): Call<MessageResponse>

    @GET("messages/{messageId}")
    @Headers(ACCEPT_HEADER_V1)
    fun messageDetail(@Path("messageId") messageId: String, @Tag retrofitTag: RetrofitTag): Call<MessageResponse>

    @GET("messages/{messageId}")
    @Headers(ACCEPT_HEADER_V1)
    fun messageDetailObservable(@Path("messageId") messageId: String): Observable<MessageResponse>

    @DELETE("messages/empty")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun emptyFolder(@Query("LabelID") labelId: String): Call<ResponseBody>

    @PUT("messages/unlabel")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun unlabelMessages(@Body body: IDList): Call<ResponseBody>

    @PUT("messages/label")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun labelMessages(@Body body: IDList): Call<MoveToFolderResponse>

}
