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

import androidx.annotation.WorkerThread
import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.DeleteContactResponse
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.UnreadTotalMessagesResponse
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendResponse
import io.reactivex.Observable
import retrofit2.Call
import java.io.IOException

interface MessageApiSpec {

    @Throws(IOException::class)
    fun fetchMessagesCount(retrofitTag: RetrofitTag): UnreadTotalMessagesResponse

    @Throws(IOException::class)
    fun messages(location: Int): MessagesResponse?

    @Throws(IOException::class)
    fun messages(location: Int, retrofitTag: RetrofitTag): MessagesResponse?

    @Throws(IOException::class)
    fun fetchMessages(location: Int, time: Long): MessagesResponse?

    @Throws(IOException::class)
    fun fetchSingleMessageMetadata(messageId: String): MessagesResponse?

    @Throws(IOException::class)
    fun markMessageAsRead(messageIds: IDList)

    @Throws(IOException::class)
    fun markMessageAsUnRead(messageIds: IDList)

    suspend fun deleteMessage(messageIds: IDList): DeleteContactResponse

    @Throws(IOException::class)
    fun emptyDrafts()

    @Throws(IOException::class)
    fun emptySpam()

    @Throws(IOException::class)
    fun emptyTrash()

    @Throws(IOException::class)
    fun emptyCustomFolder(labelId: String)

    @WorkerThread
    @Throws(Exception::class)
    fun messageDetail(messageId: String): MessageResponse

    @WorkerThread
    fun messageDetail(messageId: String, retrofitTag: RetrofitTag): MessageResponse?

    @WorkerThread
    @Throws(Exception::class)
    fun messageDetailObservable(messageId: String): Observable<MessageResponse>

    @WorkerThread
    @Throws(Exception::class)
    fun search(query: String, page: Int): MessagesResponse

    @Throws(IOException::class)
    fun searchByLabelAndPage(query: String, page: Int): MessagesResponse

    @Throws(IOException::class)
    fun searchByLabelAndTime(query: String, unixTime: Long): MessagesResponse

    @Throws(IOException::class)
    fun createDraft(draftBody: DraftBody): MessageResponse?

    @Throws(IOException::class)
    fun updateDraft(messageId: String, draftBody: DraftBody, retrofitTag: RetrofitTag): MessageResponse?

    fun sendMessage(messageId: String, message: MessageSendBody, retrofitTag: RetrofitTag): Call<MessageSendResponse>

    @Throws(IOException::class)
    fun unlabelMessages(idList: IDList)

    @Throws(IOException::class)
    fun labelMessages(body: IDList): MoveToFolderResponse?

}
