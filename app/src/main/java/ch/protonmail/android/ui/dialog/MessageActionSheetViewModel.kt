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

package ch.protonmail.android.ui.dialog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.labels.presentation.ui.ManageLabelsActionSheet
import ch.protonmail.android.usecase.delete.DeleteMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

@HiltViewModel
class MessageActionSheetViewModel @Inject constructor(
    private val deleteMessage: DeleteMessage,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val messageDetailsRepository: MessageDetailsRepository
) : ViewModel() {

    private val actionsMutableFlow = MutableStateFlow<MessageActionSheetAction>(MessageActionSheetAction.Default)
    val actionsFlow: StateFlow<MessageActionSheetAction>
        get() = actionsMutableFlow

    fun showLabelsManager(
        messageIds: List<String>,
        currentLocation: Constants.MessageLocationType,
        labelsSheetType: ManageLabelsActionSheet.Type = ManageLabelsActionSheet.Type.LABEL
    ) {
        viewModelScope.launch {
            val showLabelsManager = MessageActionSheetAction.ShowLabelsManager(
                messageIds,
                getCheckedLabelsForAllMessages(messageIds),
                currentLocation.messageLocationTypeValue,
                labelsSheetType
            )
            actionsMutableFlow.value = showLabelsManager
        }
    }

    private suspend fun getCheckedLabelsForAllMessages(
        messageIds: List<String>
    ): List<String> {
        val checkedLabels = mutableListOf<String>()
        messageIds.forEach { messageId ->
            val message = messageDetailsRepository.findMessageByIdOnce(messageId)
            checkedLabels.addAll(message.labelIDsNotIncludingLocations)
        }
        return checkedLabels
    }

    fun deleteMessage(messageIds: List<String>) {
        viewModelScope.launch {
            deleteMessage(
                messageIds, Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
            )
        }
    }

    fun moveToInbox(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) = moveMessagesToFolder(
        messageIds, Constants.MessageLocationType.INBOX.toString(),
        currentFolder.messageLocationTypeValue.toString()
    )

    fun moveToArchive(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) = moveMessagesToFolder(
        messageIds, Constants.MessageLocationType.ARCHIVE.toString(),
        currentFolder.messageLocationTypeValue.toString()
    )

    fun moveToSpam(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) = moveMessagesToFolder(
        messageIds, Constants.MessageLocationType.SPAM.toString(),
        currentFolder.messageLocationTypeValue.toString()
    )

    fun moveToTrash(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) = moveMessagesToFolder(
        messageIds, Constants.MessageLocationType.TRASH.toString(),
        currentFolder.messageLocationTypeValue.toString()
    )

    fun starMessage(messageId: List<String>) =
        messageDetailsRepository.starMessages(messageId)

    fun unStarMessage(messageId: List<String>) =
        messageDetailsRepository.unStarMessages(messageId)

    fun markUnread(messageIds: List<String>) = messageDetailsRepository.markUnRead(messageIds)

    fun markRead(messageIds: List<String>) = messageDetailsRepository.markRead(messageIds)

    fun showMessageHeaders(messageId: String) {
        viewModelScope.launch {
            val message = messageDetailsRepository.findMessageByIdOnce(messageId)
            actionsMutableFlow.value = MessageActionSheetAction.ShowMessageHeaders(message.header ?: EMPTY_STRING)
        }
    }
}
