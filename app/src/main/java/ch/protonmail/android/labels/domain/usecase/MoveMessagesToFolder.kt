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

package ch.protonmail.android.labels.domain.usecase

import ch.protonmail.android.core.Constants
import ch.protonmail.android.repository.MessageRepository
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class MoveMessagesToFolder @Inject constructor(
    private val messagesRepository: MessageRepository
) {

    operator fun invoke(
        messageIds: List<String>,
        newFolderLocationId: String,
        currentFolderLabelId: String = EMPTY_STRING,
    ) {
        when (newFolderLocationId) {
            Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString() ->
                messagesRepository.moveToTrash(messageIds, currentFolderLabelId)
            Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString() ->
                messagesRepository.moveToArchive(messageIds, currentFolderLabelId)
            Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString() ->
                messagesRepository.moveToInbox(messageIds, currentFolderLabelId)
            Constants.MessageLocationType.SPAM.messageLocationTypeValue.toString() ->
                messagesRepository.moveToSpam(messageIds)
            else ->
                messagesRepository.moveToCustomFolderLocation(messageIds, newFolderLocationId)
        }
    }
}
