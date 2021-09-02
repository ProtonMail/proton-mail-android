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

import ch.protonmail.android.data.remote.NoMoreItemsDataResult
import ch.protonmail.android.data.remote.OfflineDataResult
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.loadMoreCatch
import ch.protonmail.android.domain.loadMoreMap
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
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

    operator fun invoke(
        userId: UserId,
        labelId: String
    ): LoadMoreFlow<GetConversationsResult> =
        conversationRepository.observeConversations(buildGetParams(userId, labelId))
            .mapToResult()
            .loadMoreCatch {
                emit(GetConversationsResult.Error(it))
            }

    private fun buildGetParams(
        userId: UserId,
        labelId: String?
    ) = GetAllConversationsParameters(
        userId = userId,
        labelId = labelId
    )

    private fun LoadMoreFlow<DataResult<List<Conversation>>>.mapToResult(): LoadMoreFlow<GetConversationsResult> {

        fun Success<List<Conversation>>.successToResult(): GetConversationsResult =
            when (source) {
                ResponseSource.Local ->
                    GetConversationsResult.Success(value)
                ResponseSource.Remote ->
                    GetConversationsResult.DataRefresh(value)
            }

        fun Error.Remote.remoteErrorToResult(): GetConversationsResult {
            Timber.e(cause, "Messages couldn't be fetched")
            return if (this == NoMoreItemsDataResult) {
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
    }
}
