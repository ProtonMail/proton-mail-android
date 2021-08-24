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
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.repository.MessageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case for observe Messages by location
 */
class ObserveMessagesByLocation @Inject constructor(
    private val messageRepository: MessageRepository
) {

    operator fun invoke(
        mailboxLocation: Constants.MessageLocationType,
        labelId: String?,
        userId: UserId
    ): Flow<GetMessagesResult> =
        when (mailboxLocation) {
            Constants.MessageLocationType.LABEL,
            Constants.MessageLocationType.LABEL_FOLDER ->
                messageRepository.observeMessagesByLabelId(requireNotNull(labelId), userId)
            Constants.MessageLocationType.STARRED,
            Constants.MessageLocationType.DRAFT,
            Constants.MessageLocationType.ARCHIVE,
            Constants.MessageLocationType.INBOX,
            Constants.MessageLocationType.SEARCH,
            Constants.MessageLocationType.SPAM,
            Constants.MessageLocationType.TRASH,
            Constants.MessageLocationType.ALL_MAIL ->
                messageRepository.observeMessagesByLocation(
                    userId,
                    mailboxLocation
                )
            // Since a message can be self-sent which from BE makes the message have INBOX and SENT both as a location
            //  we decided that for now it's best we treat SENT as label
            Constants.MessageLocationType.SENT ->
                messageRepository.observeMessagesByLabelId(mailboxLocation.messageLocationTypeValue.toString(), userId)
            Constants.MessageLocationType.INVALID -> throw IllegalArgumentException("Invalid location.")
            else -> throw IllegalArgumentException("Unknown location: $mailboxLocation")
        }
            .map {
                Timber.v("GetMessagesByLocation new messages size: ${it.size}, location: $mailboxLocation")
                GetMessagesResult.Success(it) as GetMessagesResult
            }
            .catch {
                emit(GetMessagesResult.Error(it))
            }
}
