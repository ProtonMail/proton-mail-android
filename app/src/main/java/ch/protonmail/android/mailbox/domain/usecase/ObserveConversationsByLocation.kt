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

package ch.protonmail.android.mailbox.domain.usecase

import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.loadMoreCatch
import ch.protonmail.android.domain.loadMoreMap
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.mailbox.data.NO_MORE_CONVERSATIONS_ERROR_CODE
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import me.proton.core.domain.arch.DataResult
import timber.log.Timber
import javax.inject.Inject

class ObserveConversationsByLocation @Inject constructor(
    private val conversationRepository: ConversationsRepository
) {

    /**
     * @param userId Id of the user who is currently logged in
     * @param locationId the Id of the location (eg. INBOX, ARCHIVE..)
     * or custom label / folder to get the conversations for.
     */
    operator fun invoke(
        userId: UserId,
        locationId: String
    ): LoadMoreFlow<GetConversationsResult> {
        val params = GetConversationsParameters(
            locationId = locationId,
            userId = userId,
            oldestConversationTimestamp = null
        )

        Timber.v("GetConversations with params: $params, locationId: $locationId")
        return conversationRepository.getConversations(params)
            .loadMoreMap map@{ result ->
                return@map when (result) {
                    is DataResult.Success -> {
                        GetConversationsResult.Success(
                            result.value.filter { conversation ->
                                conversation.labels.any { it.id == params.locationId }
                            }
                        )
                    }
                    is DataResult.Error.Remote -> {
                        Timber.e(result.cause, "Conversations couldn't be fetched")
                        if (result.protonCode == NO_MORE_CONVERSATIONS_ERROR_CODE) {
                            return@map GetConversationsResult.NoConversationsFound
                        }
                        return@map GetConversationsResult.Error(result.cause)
                    }
                    is DataResult.Error ->
                        GetConversationsResult.Error(result.cause)
                    else -> {
                        GetConversationsResult.Error()
                    }
                }
            }
            .loadMoreCatch {
                emit(GetConversationsResult.Error(it))
            }
    }

    @Deprecated("Call loadMore on the relative LoadMoreFlow from invoke", level = DeprecationLevel.ERROR)
    fun loadMore(
        userId: UserId,
        locationId: String,
        lastConversationTime: Long
    ) {
        val params = GetConversationsParameters(
            locationId = locationId,
            userId = userId,
            oldestConversationTimestamp = lastConversationTime
        )
        conversationRepository.loadMore(params)
    }
}
