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

package ch.protonmail.android.mailbox.data

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.repository.MessageRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

// TODO This business logic should live in the domain layer and that's where this class really belongs
internal class MarkUnreadLatestNonDraftMessageInLocation @Inject constructor(
    private val messageRepository: MessageRepository
) {

    suspend operator fun invoke(messagesSortedByNewest: List<Message>, locationId: String, userId: UserId) {
        messagesSortedByNewest
            .find { message ->
                message.isRead && !message.isDraft() && message.allLabelIDs.contains(locationId)
            }
            ?.let { lastMessageInCurrentLocation ->
                messageRepository.saveMessage(
                    userId,
                    lastMessageInCurrentLocation.apply { setIsRead(false) }
                )
            }
    }
}
