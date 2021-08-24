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

package ch.protonmail.android.mailbox.data

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import me.proton.core.domain.arch.DataResult

/**
 * A representation of a bookmark that defines the last items we fetched and what we should fetch next.
 *
 * @property time the time of the last Mailbox item we fetched
 *  `null` only if [FetchMailboxBookmark.Initial]
 * @property itemId the ID of the last Mailbox item we fetched
 *  `null` only if [FetchMailboxBookmark.Initial]
 */
sealed class FetchMailboxBookmark {
    open val time: Long? = null
    open val itemId: String? = null

    data class Data(
        override val time: Long,
        override val itemId: String
    ) : FetchMailboxBookmark()

    object Initial : FetchMailboxBookmark()
}

/**
 * @return a [FetchMailboxBookmark] created from [DataResult] if [DataResult.Success] and the list is not empty,
 *  otherwise [currentBookmark]
 *
 * Note: since we're using [Message.time] for fetch progressively, we assume that the messages are already ordered by
 *  time, so we pick directly the last in the list, without adding unneeded computation
 */
@JvmName("createBookmarkOrForMessages")
fun DataResult<List<Message>>.createBookmarkOr(
    currentBookmark: FetchMailboxBookmark
): FetchMailboxBookmark {
    return if (this is DataResult.Success && value.isNotEmpty()) {
        val lastMessage = value.last()
        FetchMailboxBookmark.Data(
            time = lastMessage.time,
            itemId = checkNotNull(lastMessage.messageId) { "Can't create bookmark: messageId is null" }
        )
    } else {
        currentBookmark
    }
}

/**
 * @return a [FetchMailboxBookmark] created from [DataResult] if [DataResult.Success] and the list is not empty,
 *  otherwise [currentBookmark]
 *
 * Note: since we're using [Message.time] for fetch progressively, we assume that the messages are already ordered by
 *  time, so we pick directly the last in the list, without adding unneeded computation
 */
@JvmName("createBookmarkOrForConversations")
fun DataResult<List<ConversationApiModel>>.createBookmarkOr(
    currentBookmark: FetchMailboxBookmark
): FetchMailboxBookmark {
    return if (this is DataResult.Success && value.isNotEmpty()) {
        val lastConversation = value.last()
        FetchMailboxBookmark.Data(
            time = lastConversation.time,
            itemId = lastConversation.id
        )
    } else {
        currentBookmark
    }
}
