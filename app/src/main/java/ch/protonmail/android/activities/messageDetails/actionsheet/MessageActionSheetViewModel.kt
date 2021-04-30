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

package ch.protonmail.android.activities.messageDetails.actionsheet

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.messageDetails.labelactions.domain.MoveMessagesToFolder
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.delete.DeleteMessage
import kotlinx.coroutines.launch
import timber.log.Timber

class MessageActionSheetViewModel @ViewModelInject constructor(
    @Assisted private val savedStateHandle: SavedStateHandle,
    private val deleteMessageUseCase: DeleteMessage,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val messageDetailsRepository: MessageDetailsRepository
) : ViewModel() {

    private val messageIds: List<String> =
        savedStateHandle.get<List<String>>(MessageActionSheet.EXTRA_ARG_MESSAGE_IDS)
            ?: throw IllegalStateException("messageIds in MessageActionSheetViewModel are Empty!")

    private val currentFolder =
        Constants.MessageLocationType.fromInt(
            savedStateHandle.get<Int>(
                MessageActionSheet.EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID
            ) ?: 0
        )

    fun handleAction(
        action: MessageActionSheetActions
    ) {
        Timber.v("Handle action: $action")
        when (action) {
            MessageActionSheetActions.DELETE_MESSAGE -> deleteMessage(messageIds)
            MessageActionSheetActions.MARK_UNREAD -> markUnread(messageIds)
            MessageActionSheetActions.MARK_READ -> markRead(messageIds)
            MessageActionSheetActions.MOVE_TO_ARCHIVE -> moveToArchive(messageIds)
            MessageActionSheetActions.MOVE_TO_INBOX -> moveToInbox(messageIds)
            MessageActionSheetActions.MOVE_TO_SPAM -> moveToSpam(messageIds)
            MessageActionSheetActions.MOVE_TO_TRASH -> moveToTrash(messageIds)
            MessageActionSheetActions.STAR_MESSAGE -> starMessage(messageIds)
            MessageActionSheetActions.UNSTAR_MESSAGE -> unStarMessage(messageIds)
        }
    }

    fun deleteMessage(messageIds: List<String>) {
        viewModelScope.launch {
            deleteMessageUseCase(
                messageIds, Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
            )
        }
    }

    private fun moveToInbox(messageIds: List<String>) =
        moveMessagesToFolder(
            messageIds, Constants.MessageLocationType.INBOX.toString(),
            currentFolder.messageLocationTypeValue.toString()
        )

    private fun moveToArchive(messageIds: List<String>) =
        moveMessagesToFolder(
            messageIds, Constants.MessageLocationType.ARCHIVE.toString(),
            currentFolder.messageLocationTypeValue.toString()
        )

    private fun moveToSpam(messageIds: List<String>) =
        moveMessagesToFolder(
            messageIds, Constants.MessageLocationType.SPAM.toString(),
            currentFolder.messageLocationTypeValue.toString()
        )

    private fun moveToTrash(messageIds: List<String>) =
        moveMessagesToFolder(
            messageIds, Constants.MessageLocationType.TRASH.toString(),
            currentFolder.messageLocationTypeValue.toString()
        )

    private fun starMessage(messageId: List<String>) =
        messageDetailsRepository.starMessages(messageId)

    private fun unStarMessage(messageId: List<String>) =
        messageDetailsRepository.unStarMessages(messageId)

    private fun markUnread(messageIds: List<String>) = messageDetailsRepository.markUnRead(messageIds)

    private fun markRead(messageIds: List<String>) = messageDetailsRepository.markRead(messageIds)
}
