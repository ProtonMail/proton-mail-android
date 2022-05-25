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

package ch.protonmail.android.mailbox.domain

import ch.protonmail.android.event.data.remote.model.ConversationsEventResponse
import ch.protonmail.android.event.domain.model.ActionType
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class HandleChangeToConversations @Inject constructor(
    private val conversationRepository: ConversationsRepository,
    private val dispatchers: DispatcherProvider
) {

    /**
     * @param userId Id of the user who is currently logged in
     * @param conversations list oof conversation that we need to handle a change for
     */
    suspend operator fun invoke(
        userId: UserId,
        conversations: List<ConversationsEventResponse>
    ) = withContext(dispatchers.Io) {
        conversations.forEach { response ->

            when (ActionType.fromInt(response.action)) {
                ActionType.CREATE,
                ActionType.UPDATE,
                ActionType.UPDATE_FLAGS -> {
                    conversationRepository.saveConversationsApiModels(
                        userId,
                        listOf(response.conversation)
                    )
                }
                ActionType.DELETE -> {
                    conversationRepository.deleteConversations(listOf(response.id), userId)
                }
                else -> Timber.v("Unhandled ActionType ${response.action}")
            }
        }
    }
}
