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

import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.remote.OfflineDataResult
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.loadMoreCatch
import ch.protonmail.android.domain.loadMoreMap
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.repository.NO_MORE_MESSAGES_ERROR_CODE
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.DataResult.Error
import me.proton.core.domain.arch.DataResult.Processing
import me.proton.core.domain.arch.DataResult.Success
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.takeIfNotBlank
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for observe Messages by location
 */
class ObserveMessagesByLocation @Inject constructor(
    private val messageRepository: MessageRepository
) {

    operator fun invoke(
        userId: UserId,
        mailboxLocation: Constants.MessageLocationType,
        labelId: String?
    ): LoadMoreFlow<GetMessagesResult> =
        messageRepository.observeMessages(buildGetParams(userId, mailboxLocation, labelId))
            .mapToResult()
            .loadMoreCatch {
                emit(GetMessagesResult.Error(it))
            }

    private fun buildGetParams(
        userId: UserId,
        mailboxLocation: Constants.MessageLocationType,
        labelId: String?
    ) = GetAllMessagesParameters(
        userId = userId,
        labelId = labelId?.takeIfNotBlank() ?: mailboxLocation.asLabelId()
    )

    private fun LoadMoreFlow<DataResult<List<Message>>>.mapToResult(): LoadMoreFlow<GetMessagesResult> {

        fun Success<List<Message>>.successToResult(): GetMessagesResult =
            when (source) {
                ResponseSource.Local ->
                    GetMessagesResult.Success(value)
                ResponseSource.Remote ->
                    GetMessagesResult.ApiRefresh(value)
            }

        fun Error.Remote.remoteErrorToResult(): GetMessagesResult {
            Timber.e(cause, "Messages couldn't be fetched")
            return if (protonCode == NO_MORE_MESSAGES_ERROR_CODE) {
                GetMessagesResult.NoMessagesFound
            } else {
                GetMessagesResult.Error(cause, isOffline = this == OfflineDataResult)
            }
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
