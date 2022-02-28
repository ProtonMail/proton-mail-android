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

import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * A use case that handles changing the starred status of a conversation
 */
class ChangeConversationsStarredStatus @Inject constructor(
    private val conversationsRepository: ConversationsRepository
) {

    suspend operator fun invoke(
        conversationIds: List<String>,
        userId: UserId,
        action: Action
    ): ConversationsActionResult {
        return if (action == Action.ACTION_STAR) {
            conversationsRepository.star(conversationIds, userId)
        } else {
            conversationsRepository.unstar(conversationIds, userId)
        }
    }

    enum class Action {
        ACTION_STAR,
        ACTION_UNSTAR
    }
}
