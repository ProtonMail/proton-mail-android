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

import android.content.Context
import androidx.room.withTransaction
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.event.data.remote.model.ConversationsEventResponse
import ch.protonmail.android.event.domain.model.ActionType
import ch.protonmail.android.mailbox.data.toLocal
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class HandleChangeToConversations @Inject constructor(
    private val applicationContext: Context,
    private val conversationRepository: ConversationsRepository,
    private val dispatchers: DispatcherProvider
) {

    /**
     * @param userId Id of the user who is currently logged in
     * @param conversations list oof conversation that we need to handle a change for
     */
    suspend operator fun invoke(
        userId: Id,
        conversations: List<ConversationsEventResponse>
    ) = withContext(dispatchers.Io) {
        conversations.forEach { response ->

            when (ActionType.fromInt(response.action)) {
                ActionType.CREATE -> {
                    val conversation = response.conversation.toLocal(userId = userId.s)
                    conversationRepository.saveConversations(listOf(conversation), userId)
                }
                ActionType.UPDATE,
                ActionType.UPDATE_FLAGS -> {
                    if (!MessageDatabase.getInstance(applicationContext, userId).withTransaction {
                        conversationRepository.updateConversation(response, userId)
                    }
                    ) {
                        conversationRepository.getConversation(response.id, userId)
                    }
                }

                ActionType.DELETE -> {
                    conversationRepository.deleteConversations(listOf(response.id), userId)
                }
            }
        }
    }
}
