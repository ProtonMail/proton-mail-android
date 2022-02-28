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

package ch.protonmail.android.usecase.message

import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * A use case that handles changing the read/unread status of a message
 */
class ChangeMessagesReadStatus @Inject constructor(
    private val messageRepository: MessageRepository,
    private val conversationsRepository: ConversationsRepository
) {

    suspend operator fun invoke(
        messageIds: List<String>,
        action: Action,
        userId: UserId
    ) {
        if (action == Action.ACTION_MARK_READ) {
            messageRepository.markRead(messageIds)
        } else {
            messageRepository.markUnRead(messageIds)
        }

        conversationsRepository.updateConvosBasedOnMessagesReadStatus(
            userId,
            messageIds,
            action
        )
    }

    enum class Action {
        ACTION_MARK_READ,
        ACTION_MARK_UNREAD
    }
}
