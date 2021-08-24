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
import io.reactivex.Observable
import me.proton.core.domain.entity.UserId
import java.io.IOException

interface MessageApiSpec {

    @Throws(IOException::class)
    fun fetchMessagesCount(userIdTag: UserIdTag): UnreadTotalMessagesResponse

    suspend fun getMessages(
        userId: UserId,
        page: Int = 0,
        labelId: String? = null,
        begin: Long? = null,
        end: Long? = null,
        beginId: String? = null,
        endId: String? = null,
        keyword: String? = null
    ): MessagesResponse

    suspend fun fetchMessageMetadata(messageId: String, userIdTag: UserIdTag): MessagesResponse

    @Throws(IOException::class)
    fun markMessageAsRead(messageIds: IDList)

    @Throws(IOException::class)
    fun markMessageAsUnRead(messageIds: IDList)

    suspend fun deleteMessage(messageDeleteRequest: MessageDeleteRequest): DeleteResponse

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
    fun fetchMessageDetailsBlocking(messageId: String): MessageResponse

    suspend fun fetchMessageDetails(messageId: String, userIdTag: UserIdTag): MessageResponse

    @WorkerThread
    fun fetchMessageDetailsBlocking(messageId: String, userIdTag: UserIdTag): MessageResponse?

    @WorkerThread
    @Throws(Exception::class)
    fun messageDetailObservable(messageId: String): Observable<MessageResponse>

    suspend fun createDraft(draftBody: DraftBody): MessageResponse

    suspend fun updateDraft(
        messageId: String,
        draftBody: DraftBody,
        userIdTag: UserIdTag
    ): MessageResponse

    suspend fun sendMessage(
        messageId: String,
        message: MessageSendBody,
        userIdTag: UserIdTag
    ): MessageSendResponse

    @Throws(IOException::class)
    fun unlabelMessages(idList: IDList)

    @Throws(IOException::class)
    fun labelMessages(body: IDList): MoveToFolderResponse?

}
