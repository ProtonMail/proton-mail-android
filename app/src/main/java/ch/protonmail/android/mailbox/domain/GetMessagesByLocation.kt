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

import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesByLocation @Inject constructor(
    private val messageRepository: MessageRepository
) {

    operator fun invoke(
        mailboxLocation: Constants.MessageLocationType,
        labelId: String?,
        userId: Id
    ): Flow<List<Message>> =
        when (mailboxLocation) {
            Constants.MessageLocationType.LABEL,
            Constants.MessageLocationType.LABEL_OFFLINE,
            Constants.MessageLocationType.LABEL_FOLDER ->
                messageRepository.fetchMessagesByLabelId(requireNotNull(labelId), userId)
            Constants.MessageLocationType.STARRED,
            Constants.MessageLocationType.DRAFT,
            Constants.MessageLocationType.SENT,
            Constants.MessageLocationType.ARCHIVE,
            Constants.MessageLocationType.INBOX,
            Constants.MessageLocationType.SEARCH,
            Constants.MessageLocationType.SPAM,
            Constants.MessageLocationType.TRASH ->
                messageRepository.fetchMessagesByLocation(
                    mailboxLocation,
                    userId
                )
            Constants.MessageLocationType.ALL_MAIL -> messageRepository.observeAllMessages(userId)
            Constants.MessageLocationType.INVALID -> throw IllegalArgumentException("Invalid location.")
            else -> throw IllegalArgumentException("Unknown location: $mailboxLocation")
        }
}
