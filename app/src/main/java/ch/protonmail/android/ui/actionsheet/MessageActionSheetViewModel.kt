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

package ch.protonmail.android.ui.actionsheet

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.usecase.delete.DeleteMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessageActionSheetViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val deleteMessage: DeleteMessage,
    private val deleteConversations: DeleteConversations,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val moveConversationsToFolder: MoveConversationsToFolder,
    private val messageRepository: MessageRepository,
    private val changeConversationsReadStatus: ChangeConversationsReadStatus,
    private val changeConversationsStarredStatus: ChangeConversationsStarredStatus,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val accountManager: AccountManager
) : ViewModel() {

    private val actionsMutableFlow = MutableStateFlow<MessageActionSheetAction>(MessageActionSheetAction.Default)
    val actionsFlow: StateFlow<MessageActionSheetAction>
        get() = actionsMutableFlow

    fun showLabelsManager(
        messageIds: List<String>,
        currentLocation: Constants.MessageLocationType,
        labelsSheetType: LabelsActionSheet.Type = LabelsActionSheet.Type.LABEL
    ) {
        viewModelScope.launch {
            val showLabelsManager = MessageActionSheetAction.ShowLabelsManager(
                messageIds,
                currentLocation.messageLocationTypeValue,
                labelsSheetType
            )
            actionsMutableFlow.value = showLabelsManager
        }
    }

    /**
     * If conversation mode is on, this will delete conversations with the given [ids] regardless of [currentFolder]
     * (Example: all selected conversations in mailbox selection mode;
     * or the conversation that is currently opened in the details)
     * If conversation mode is off, this will delete messages with the given [ids] regardless of [currentFolder]
     * (Example: all selected messages in mailbox selection mode;
     * or the message that is currently opened in the details)
     */
    fun delete(
        ids: List<String>,
        currentFolder: Constants.MessageLocationType
    ) {
        viewModelScope.launch {
            if (conversationModeEnabled(currentFolder)) {
                accountManager.getPrimaryUserId().first()?.let {
                    deleteConversations(
                        ids,
                        it,
                        currentFolder.messageLocationTypeValue.toString()
                    )
                }
            } else {
                deleteMessage(
                    ids,
                    currentFolder.messageLocationTypeValue.toString()
                )
            }
        }.invokeOnCompletion {
            actionsMutableFlow.value = MessageActionSheetAction.Delete
        }
    }

    fun moveToInbox(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) = moveMessagesToFolderAndDismiss(
        messageIds, Constants.MessageLocationType.INBOX.toString(), currentFolder
    )

    fun moveToArchive(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) = moveMessagesToFolderAndDismiss(
        messageIds, Constants.MessageLocationType.ARCHIVE.toString(), currentFolder
    )

    fun moveToSpam(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) = moveMessagesToFolderAndDismiss(
        messageIds, Constants.MessageLocationType.SPAM.toString(), currentFolder
    )

    fun moveToTrash(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) = moveMessagesToFolderAndDismiss(
        messageIds, Constants.MessageLocationType.TRASH.toString(), currentFolder
    )

    fun starMessage(
        ids: List<String>,
        location: Constants.MessageLocationType
    ) {
        viewModelScope.launch {
            if (isActionAppliedToConversation(location)) {
                val primaryUserId = accountManager.getPrimaryUserId().first()
                if (primaryUserId != null) {
                    changeConversationsStarredStatus(
                        ids,
                        primaryUserId,
                        ChangeConversationsStarredStatus.Action.ACTION_STAR
                    )
                } else {
                    Timber.e("Primary user id is null. Cannot star message/conversation")
                }
            } else {
                messageRepository.starMessages(ids)
            }
        }.invokeOnCompletion {
            actionsMutableFlow.value = MessageActionSheetAction.ChangeStarredStatus(true)
        }
    }

    fun unStarMessage(
        ids: List<String>,
        location: Constants.MessageLocationType
    ) {
        viewModelScope.launch {
            if (isActionAppliedToConversation(location)) {
                val primaryUserId = accountManager.getPrimaryUserId().first()
                if (primaryUserId != null) {
                    changeConversationsStarredStatus(
                        ids,
                        primaryUserId,
                        ChangeConversationsStarredStatus.Action.ACTION_UNSTAR
                    )
                } else {
                    Timber.e("Primary user id is null. Cannot unstar message/conversation")
                }
            } else {
                messageRepository.unStarMessages(ids)
            }
        }.invokeOnCompletion {
            actionsMutableFlow.value = MessageActionSheetAction.ChangeStarredStatus(false)
        }
    }

    fun markUnread(
        ids: List<String>,
        location: Constants.MessageLocationType
    ) {
        viewModelScope.launch {
            if (isActionAppliedToConversation(location)) {
                val primaryUserId = accountManager.getPrimaryUserId().first()
                if (primaryUserId != null) {
                    changeConversationsReadStatus(
                        ids,
                        ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                        primaryUserId,
                        location
                    )
                } else {
                    Timber.e("Primary user id is null. Cannot mark message/conversation unread")
                }
            } else {
                messageRepository.markUnRead(ids)
            }
        }.invokeOnCompletion {
            val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
            actionsMutableFlow.value = MessageActionSheetAction.ShouldDismiss(dismissBackingActivity)
        }
    }

    fun markRead(
        ids: List<String>,
        location: Constants.MessageLocationType
    ) {
        viewModelScope.launch {
            if (isActionAppliedToConversation(location)) {
                val primaryUserId = accountManager.getPrimaryUserId().first()
                if (primaryUserId != null) {
                    changeConversationsReadStatus(
                        ids,
                        ChangeConversationsReadStatus.Action.ACTION_MARK_READ,
                        primaryUserId,
                        location
                    )
                } else {
                    Timber.e("Primary user id is null. Cannot mark message/conversation read")
                }
            } else {
                messageRepository.markRead(ids)
            }
        }.invokeOnCompletion {
            val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
            actionsMutableFlow.value = MessageActionSheetAction.ShouldDismiss(dismissBackingActivity)
        }
    }

    fun showMessageHeaders(messageId: String) {
        viewModelScope.launch {
            val message = messageRepository.findMessageById(messageId)
            actionsMutableFlow.value = MessageActionSheetAction.ShowMessageHeaders(message?.header ?: EMPTY_STRING)
        }
    }

    private fun moveMessagesToFolderAndDismiss(
        ids: List<String>, newFolderLocationId: String,
        currentFolder: Constants.MessageLocationType
    ) {
        viewModelScope.launch {
            if (isActionAppliedToConversation(currentFolder)) {
                val primaryUserId = accountManager.getPrimaryUserId().first()
                if (primaryUserId != null) {
                    moveConversationsToFolder(
                        ids,
                        primaryUserId,
                        newFolderLocationId
                    )
                } else {
                    Timber.e("Primary user id is null. Cannot move message/conversation to folder")
                }
            } else {
                moveMessagesToFolder(
                    ids,
                    newFolderLocationId,
                    currentFolder.messageLocationTypeValue.toString()
                )
            }
        }.invokeOnCompletion {
            val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
            actionsMutableFlow.value = MessageActionSheetAction.ShouldDismiss(dismissBackingActivity)
        }
    }

    private fun isActionAppliedToConversation(location: Constants.MessageLocationType) =
        conversationModeEnabled(location) && !isApplyingActionToMessageWithinAConversation()

    private fun isApplyingActionToMessageWithinAConversation(): Boolean {
        val originatorId = getOriginatorIdInputArg()
        return originatorId == MessageActionSheet.ARG_ORIGINATOR_SCREEN_CONVERSATION_DETAILS_ID
    }

    private fun getOriginatorIdInputArg() = savedStateHandle.get(MessageActionSheet.EXTRA_ARG_ORIGINATOR_SCREEN_ID)
        ?: MessageActionSheet.ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID

}
