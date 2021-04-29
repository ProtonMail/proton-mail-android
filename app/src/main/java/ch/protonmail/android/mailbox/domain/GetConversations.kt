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

package ch.protonmail.android.mailbox.domain

import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.NO_MORE_CONVERSATIONS_ERROR_CODE
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.arch.DataResult
import javax.inject.Inject

class GetConversations @Inject constructor(
    private val conversationRepository: ConversationsRepository
) {

    operator fun invoke(
        userId: Id,
        location: MessageLocationType,
        customLabelId: String?
    ): Flow<GetConversationsResult> {
        val locationId = customLabelId ?: location.messageLocationTypeValue.toString()
        val params = GetConversationsParameters(
            locationId = locationId,
            userId = userId,
            oldestConversationTimestamp = null
        )

        return conversationRepository.getConversations(params)
            .map { result ->
                return@map when (result) {
                    is DataResult.Success -> {
                        GetConversationsResult.Success(
                            result.value.filter { conversation ->
                                conversation.labels.any { it.id == params.locationId }
                            }
                        )
                    }
                    is DataResult.Error.Remote -> {
                        if (result.protonCode == NO_MORE_CONVERSATIONS_ERROR_CODE) {
                            return@map GetConversationsResult.NoConversationsFound
                        }
                        return@map GetConversationsResult.Error
                    }
                    else -> {
                        GetConversationsResult.Error
                    }
                }
            }
    }

    fun loadMore(
        userId: Id,
        location: MessageLocationType,
        lastConversationTime: Long
    ) {
        val params = GetConversationsParameters(
            locationId = location.messageLocationTypeValue.toString(),
            userId = userId,
            oldestConversationTimestamp = lastConversationTime
        )
        conversationRepository.loadMore(params)
    }

}
