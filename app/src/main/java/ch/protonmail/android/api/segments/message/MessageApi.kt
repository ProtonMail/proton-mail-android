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
import ch.protonmail.android.api.models.DraftBody
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.MoveToFolderResponse
import ch.protonmail.android.api.models.messages.delete.MessageDeleteRequest
import ch.protonmail.android.api.models.messages.receive.MessageResponse
import ch.protonmail.android.api.models.messages.receive.MessagesResponse
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendResponse
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.mailbox.data.remote.model.CountsResponse
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import io.reactivex.Observable
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import java.io.IOException

class MessageApi(private val service: MessageService) : BaseApi(), MessageApiSpec {

    override suspend fun fetchMessagesCounts(userId: UserId): CountsResponse =
        service.fetchMessagesCounts(UserIdTag(userId))

    override suspend fun getMessages(params: GetAllMessagesParameters): MessagesResponse =
        service.getMessages(
            userIdTag = UserIdTag(params.userId),
            page = params.page,
            pageSize = params.pageSize,
            labelId = params.labelId,
            sort = params.sortBy.stringValue,
            desc = params.sortDirection.intValue,
            begin = params.begin,
            end = params.end,
            beginId = params.beginId,
            endId = params.endId,
            keyword = params.keyword
        )

    override suspend fun fetchMessageMetadata(messageId: String, userIdTag: UserIdTag): MessagesResponse =
        service.fetchMessageMetadata(messageId, userIdTag)

    @Throws(IOException::class)
    override fun markMessageAsRead(messageIds: IDList) {
        service.read(messageIds).execute()
    }

    @Throws(IOException::class)
    override fun markMessageAsUnRead(messageIds: IDList) {
        service.unRead(messageIds).execute()
    }

    override suspend fun deleteMessage(messageDeleteRequest: MessageDeleteRequest) =
        service.delete(messageDeleteRequest)

    override suspend fun emptyFolder(userIdTag: UserIdTag, labelId: LabelId) {
        service.emptyFolder(userIdTag, labelId.id)
    }

    @WorkerThread
    @Throws(Exception::class)
    override fun fetchMessageDetailsBlocking(messageId: String): MessageResponse =
        ParseUtils.parse(service.fetchMessageDetailsBlocking(messageId).execute())

    override suspend fun fetchMessageDetails(messageId: String, userIdTag: UserIdTag): MessageResponse =
        service.fetchMessageDetails(messageId, userIdTag)

    @WorkerThread
    override fun fetchMessageDetailsBlocking(messageId: String, userIdTag: UserIdTag): MessageResponse? =
        try {
            ParseUtils.parse(service.fetchMessageDetailsBlocking(messageId, userIdTag).execute())
        } catch (exc: Exception) {
            Timber.e(exc, "An exception was thrown while fetching message details")
            null
        }

    @WorkerThread
    @Throws(Exception::class)
    override fun messageDetailObservable(messageId: String): Observable<MessageResponse> =
        service.messageDetailObservable(messageId)

    override suspend fun createDraft(draftBody: DraftBody): MessageResponse = service.createDraft(draftBody)

    override suspend fun updateDraft(
        messageId: String,
        draftBody: DraftBody,
        userIdTag: UserIdTag
    ): MessageResponse = service.updateDraft(messageId, draftBody, userIdTag)

    override suspend fun sendMessage(
        messageId: String,
        message: MessageSendBody,
        userIdTag: UserIdTag
    ): MessageSendResponse = service.sendMessage(messageId, message, userIdTag)

    @Throws(IOException::class)
    override fun unlabelMessages(idList: IDList) {
        service.unlabelMessages(idList).execute()
    }

    @Throws(IOException::class)
    override fun labelMessages(body: IDList): MoveToFolderResponse? =
        ParseUtils.parse(service.labelMessages(body).execute())
}
