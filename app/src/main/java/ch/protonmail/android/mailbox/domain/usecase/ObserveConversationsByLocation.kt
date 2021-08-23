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

import ch.protonmail.android.data.remote.OfflineDataResult
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.loadMoreCatch
import ch.protonmail.android.domain.loadMoreMap
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.repository.NO_MORE_MESSAGES_ERROR_CODE
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Processing
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
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
        val params = GetAllConversationsParameters(
            userId = userId,
            labelId = locationId,
        )

        Timber.v("GetConversations with params: $params, locationId: $locationId")
        return conversationRepository.observeConversations(params,)
            .mapToResult()
            .loadMoreCatch {
                emit(GetConversationsResult.Error(it))
            }
    }

<<<<<<< HEAD
    @Deprecated("Call loadMore on the relative LoadMoreFlow from invoke", level = DeprecationLevel.ERROR)
    fun loadMore(
        userId: UserId,
        locationId: String,
        lastConversationTime: Long
    ) {
        val params = GetAllConversationsParameters(
            userId = userId,
            labelId = locationId,
        )
        conversationRepository.loadMore(params)
=======
    private fun LoadMoreFlow<DataResult<List<Conversation>>>.mapToResult(): LoadMoreFlow<GetConversationsResult> {

        fun Success<List<Conversation>>.successToResult(): GetConversationsResult =
            when (source) {
                ResponseSource.Local ->
                    GetConversationsResult.Success(value)
                ResponseSource.Remote ->
                    GetConversationsResult.ApiRefresh(value)
            }

        fun Error.Remote.remoteErrorToResult(): GetConversationsResult {
            Timber.e(cause, "Conversations couldn't be fetched")
            return if (protonCode == NO_MORE_MESSAGES_ERROR_CODE) {
                GetConversationsResult.NoConversationsFound
            } else {
                GetConversationsResult.Error(cause, isOffline = this == OfflineDataResult)
            }
        }

        return loadMoreMap { result ->
            when (result) {
                is Success -> result.successToResult()
                is Error.Remote -> result.remoteErrorToResult()
                is Error -> GetConversationsResult.Error(result.cause)
                is Processing -> GetConversationsResult.Loading
            }
        }
>>>>>>> 9874f3e45 (Apply ProtonStore.kt to Conversations)
    }

}
