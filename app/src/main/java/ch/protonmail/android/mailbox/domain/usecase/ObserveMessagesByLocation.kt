/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.mailbox.domain.usecase

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.remote.OfflineDataResult
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.loadMoreCatch
import ch.protonmail.android.domain.loadMoreMap
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.repository.MessageRepository
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Processing
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for observe Messages by location
 */
internal class ObserveMessagesByLocation @Inject constructor(
    private val messageRepository: MessageRepository
) {

    operator fun invoke(params: GetAllMessagesParameters): LoadMoreFlow<GetMessagesResult> {
        return messageRepository.observeMessages(params)
            .mapToResult()
            .loadMoreCatch {
                emit(GetMessagesResult.Error(it))
            }
    }

    private fun LoadMoreFlow<DataResult<List<Message>>>.mapToResult(): LoadMoreFlow<GetMessagesResult> {

        fun Success<List<Message>>.successToResult(): GetMessagesResult =
            when (source) {
                ResponseSource.Local ->
                    GetMessagesResult.Success(value)
                ResponseSource.Remote ->
                    GetMessagesResult.DataRefresh(value)
            }

        fun Error.Remote.remoteErrorToResult(): GetMessagesResult {
            Timber.e(cause, "Messages couldn't be fetched")
            return GetMessagesResult.Error(cause, isOffline = this == OfflineDataResult)
        }

        return loadMoreMap { result ->
            when (result) {
                is Success -> result.successToResult()
                is Error.Remote -> result.remoteErrorToResult()
                is Error -> GetMessagesResult.Error(result.cause)
                is Processing -> GetMessagesResult.Loading
            }
        }
    }
}
