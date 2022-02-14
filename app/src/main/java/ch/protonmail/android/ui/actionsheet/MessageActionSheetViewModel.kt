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

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import ch.protonmail.android.usecase.IsAppInDarkMode
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.message.ChangeMessagesReadStatus
import ch.protonmail.android.usecase.message.ChangeMessagesStarredStatus
import ch.protonmail.android.details.domain.usecase.GetViewInDarkModeMessagePreference
import ch.protonmail.android.details.domain.usecase.SetViewInDarkModeMessagePreference
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
internal class MessageActionSheetViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val deleteMessage: DeleteMessage,
    private val deleteConversations: DeleteConversations,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val moveConversationsToFolder: MoveConversationsToFolder,
    private val messageRepository: MessageRepository,
    private val changeMessagesReadStatus: ChangeMessagesReadStatus,
    private val changeConversationsReadStatus: ChangeConversationsReadStatus,
    private val changeMessagesStarredStatus: ChangeMessagesStarredStatus,
    private val changeConversationsStarredStatus: ChangeConversationsStarredStatus,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val isAppInDarkMode: IsAppInDarkMode,
    private val getViewInDarkModeMessagePreference: GetViewInDarkModeMessagePreference,
    private val setViewInDarkModeMessagePreference: SetViewInDarkModeMessagePreference,
    private val accountManager: AccountManager
) : ViewModel() {


    private val mutableStateFlow = MutableStateFlow<MessageActionSheetState>(MessageActionSheetState.Initial)
    val stateFlow: StateFlow<MessageActionSheetState>
        get() = mutableStateFlow

    private val actionsMutableFlow = MutableStateFlow<MessageActionSheetAction>(MessageActionSheetAction.Default)
    val actionsFlow: StateFlow<MessageActionSheetAction>
        get() = actionsMutableFlow

    fun setupViewState(
        context: Context,
        messageIds: List<String>,
        messageLocation: Constants.MessageLocationType,
        mailboxLocationId: String,
        actionsTarget: ActionSheetTarget
    ) {
        viewModelScope.launch {
            val manageSectionState = computeManageSectionState(
                context,
                messageLocation,
                mailboxLocationId,
                messageIds,
                actionsTarget
            )
            val moveSectionState = computeMoveSectionState(
                actionsTarget,
                messageLocation,
                mailboxLocationId,
                messageIds
            )
            mutableStateFlow.value = MessageActionSheetState.Data(manageSectionState, moveSectionState)
        }
    }

    fun showLabelsManager(
        messageIds: List<String>,
        currentLocation: Constants.MessageLocationType,
        currentLocationId: String,
        labelsSheetType: LabelType = LabelType.MESSAGE_LABEL
    ) {
        viewModelScope.launch {
            val showLabelsManager = MessageActionSheetAction.ShowLabelsManager(
                messageIds,
                currentLocation.messageLocationTypeValue,
                currentLocationId,
                labelsSheetType,
                getActionsTargetInputArg()
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
        currentFolder: Constants.MessageLocationType,
        shallIgnoreLocationInConversationResolution: Boolean
    ) {
        viewModelScope.launch {
            accountManager.getPrimaryUserId().first()?.let { userId ->
                if (isActionAppliedToConversation(currentFolder, shallIgnoreLocationInConversationResolution)) {
                    deleteConversations(
                        ids,
                        userId,
                        currentFolder.messageLocationTypeValue.toString()
                    )
                } else {
                    deleteMessage(
                        ids,
                        currentFolder.messageLocationTypeValue.toString(),
                        userId
                    )
                }
            }
        }.invokeOnCompletion {
            val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
            actionsMutableFlow.value = MessageActionSheetAction.DismissActionSheet(
                dismissBackingActivity,
                areMailboxItemsMovedFromLocation = true
            )
        }
    }

    fun moveToInbox(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) {
        moveMessagesToFolderAndDismiss(messageIds, Constants.MessageLocationType.INBOX, currentFolder)
    }

    fun moveToArchive(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) {
        moveMessagesToFolderAndDismiss(messageIds, Constants.MessageLocationType.ARCHIVE, currentFolder)
    }

    fun moveToSpam(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) {
        moveMessagesToFolderAndDismiss(messageIds, Constants.MessageLocationType.SPAM, currentFolder)
    }

    fun moveToTrash(
        messageIds: List<String>,
        currentFolder: Constants.MessageLocationType
    ) {
        moveMessagesToFolderAndDismiss(messageIds, Constants.MessageLocationType.TRASH, currentFolder)
    }

    fun starMessage(
        ids: List<String>,
        location: Constants.MessageLocationType,
        shallIgnoreLocationInConversationResolution: Boolean = false
    ) {
        viewModelScope.launch {
            val primaryUserId = accountManager.getPrimaryUserId().first()
            if (primaryUserId != null) {
                if (isActionAppliedToConversation(location, shallIgnoreLocationInConversationResolution)) {
                    val result = changeConversationsStarredStatus(
                        ids,
                        primaryUserId,
                        ChangeConversationsStarredStatus.Action.ACTION_STAR
                    )
                    if (result is ConversationsActionResult.Error) {
                        cancel("Could not complete the action")
                    }
                } else {
                    changeMessagesStarredStatus(
                        primaryUserId,
                        ids,
                        ChangeMessagesStarredStatus.Action.ACTION_STAR
                    )
                }
            } else {
                Timber.e("Primary user id is null. Cannot star message/conversation")
            }
        }.invokeOnCompletion { cancellationException ->
            if (cancellationException != null) {
                actionsMutableFlow.value = MessageActionSheetAction.ChangeStarredStatus(
                    starredStatus = true, isSuccessful = false, areMailboxItemsMovedFromLocation = false
                )
            } else {
                actionsMutableFlow.value = MessageActionSheetAction.ChangeStarredStatus(
                    starredStatus = true, isSuccessful = true, areMailboxItemsMovedFromLocation = false
                )
            }
        }
    }

    fun unStarMessage(
        ids: List<String>,
        location: Constants.MessageLocationType,
        shallIgnoreLocationInConversationResolution: Boolean = false
    ) {
        viewModelScope.launch {
            val primaryUserId = accountManager.getPrimaryUserId().first()
            if (primaryUserId != null) {
                if (isActionAppliedToConversation(location, shallIgnoreLocationInConversationResolution)) {
                    val result = changeConversationsStarredStatus(
                        ids,
                        primaryUserId,
                        ChangeConversationsStarredStatus.Action.ACTION_UNSTAR
                    )
                    if (result is ConversationsActionResult.Error) {
                        cancel("Could not complete the action")
                    }
                } else {
                    changeMessagesStarredStatus(
                        primaryUserId,
                        ids,
                        ChangeMessagesStarredStatus.Action.ACTION_UNSTAR
                    )
                }
            } else {
                Timber.e("Primary user id is null. Cannot unstar message/conversation")
            }
        }.invokeOnCompletion { cancellationException ->
            if (cancellationException != null) {
                actionsMutableFlow.value = MessageActionSheetAction.ChangeStarredStatus(
                    starredStatus = false, isSuccessful = false, areMailboxItemsMovedFromLocation = false
                )
            } else {
                actionsMutableFlow.value = MessageActionSheetAction.ChangeStarredStatus(
                    starredStatus = false,
                    isSuccessful = true,
                    areMailboxItemsMovedFromLocation = location == Constants.MessageLocationType.STARRED
                )
            }
        }
    }

    fun markUnread(
        ids: List<String>,
        location: Constants.MessageLocationType,
        locationId: String,
        shallIgnoreLocationInConversationResolution: Boolean = false
    ) {
        viewModelScope.launch {
            val primaryUserId = accountManager.getPrimaryUserId().first()
            if (primaryUserId != null) {
                if (isActionAppliedToConversation(location, shallIgnoreLocationInConversationResolution)) {
                    val result = changeConversationsReadStatus(
                        ids,
                        ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                        primaryUserId,
                        locationId
                    )
                    if (result is ConversationsActionResult.Error) {
                        cancel("Could not complete the action")
                    }
                } else {
                    changeMessagesReadStatus(
                        ids,
                        ChangeMessagesReadStatus.Action.ACTION_MARK_UNREAD,
                        primaryUserId
                    )
                }
            } else {
                Timber.e("Primary user id is null. Cannot mark message/conversation unread")
            }
        }.invokeOnCompletion { cancellationException ->
            if (cancellationException != null) {
                actionsMutableFlow.value = MessageActionSheetAction.CouldNotCompleteActionError
            } else {
                val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
                actionsMutableFlow.value = MessageActionSheetAction.DismissActionSheet(
                    shallDismissBackingActivity = dismissBackingActivity,
                    areMailboxItemsMovedFromLocation = false
                )
            }
        }
    }

    fun markRead(
        ids: List<String>,
        location: Constants.MessageLocationType,
        locationId: String,
        shallIgnoreLocationInConversationResolution: Boolean = false
    ) {
        viewModelScope.launch {
            val primaryUserId = accountManager.getPrimaryUserId().first()
            if (primaryUserId != null) {
                if (isActionAppliedToConversation(location, shallIgnoreLocationInConversationResolution)) {
                    val result = changeConversationsReadStatus(
                        ids,
                        ChangeConversationsReadStatus.Action.ACTION_MARK_READ,
                        primaryUserId,
                        locationId
                    )
                    if (result is ConversationsActionResult.Error) {
                        cancel("Could not complete the action")
                    }
                } else {
                    changeMessagesReadStatus(
                        ids,
                        ChangeMessagesReadStatus.Action.ACTION_MARK_READ,
                        primaryUserId
                    )
                }
            } else {
                Timber.e("Primary user id is null. Cannot mark message/conversation read")
            }
        }.invokeOnCompletion { cancellationException ->
            if (cancellationException != null) {
                actionsMutableFlow.value = MessageActionSheetAction.CouldNotCompleteActionError
            } else {
                val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
                actionsMutableFlow.value = MessageActionSheetAction.DismissActionSheet(
                    shallDismissBackingActivity = dismissBackingActivity,
                    areMailboxItemsMovedFromLocation = false
                )
            }
        }
    }

    fun showMessageHeaders(messageId: String) {
        viewModelScope.launch {
            val message = messageRepository.findMessageById(messageId)
            actionsMutableFlow.value = MessageActionSheetAction.ShowMessageHeaders(message?.header ?: EMPTY_STRING)
        }
    }

    fun viewInLightMode(messageId: String) {
        viewModelScope.launch {
            accountManager.getPrimaryUserId().first()?.let {
                setViewInDarkModeMessagePreference(it, messageId, viewInDarkMode = false)
            }
        }.invokeOnCompletion {
            actionsMutableFlow.value = MessageActionSheetAction.ViewMessageInLightDarkMode(
                messageId = messageId
            )
        }
    }

    fun viewInDarkMode(messageId: String) {
        viewModelScope.launch {
            accountManager.getPrimaryUserId().first()?.let {
                setViewInDarkModeMessagePreference(it, messageId, viewInDarkMode = true)
            }
        }.invokeOnCompletion {
            actionsMutableFlow.value = MessageActionSheetAction.ViewMessageInLightDarkMode(
                messageId = messageId
            )
        }
    }

    private fun moveMessagesToFolderAndDismiss(
        ids: List<String>,
        newFolderLocationId: Constants.MessageLocationType,
        currentFolder: Constants.MessageLocationType
    ) {
        viewModelScope.launch {
            val primaryUserId = accountManager.getPrimaryUserId().first()
            if (isActionAppliedToConversation(currentFolder, true)) {
                Timber.v("Move conversation to folder: $newFolderLocationId")
                if (primaryUserId != null) {
                    val result = moveConversationsToFolder(
                        ids,
                        primaryUserId,
                        newFolderLocationId.messageLocationTypeValue.toString()
                    )
                    if (result is ConversationsActionResult.Error) {
                        cancel("Could not complete the action")
                    }
                } else {
                    Timber.e("Primary user id is null. Cannot move message/conversation to folder")
                }
            } else {
                Timber.v("Move message to folder: $newFolderLocationId")
                moveMessagesToFolder(
                    ids,
                    newFolderLocationId.messageLocationTypeValue.toString(),
                    currentFolder.messageLocationTypeValue.toString(),
                    requireNotNull(primaryUserId)
                )
            }
        }.invokeOnCompletion { cancellationException ->
            if (cancellationException != null) {
                actionsMutableFlow.value = MessageActionSheetAction.CouldNotCompleteActionError
            } else {
                val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
                actionsMutableFlow.value = MessageActionSheetAction.DismissActionSheet(
                    dismissBackingActivity,
                    areMailboxItemsMovedFromLocation = when (currentFolder) {
                        Constants.MessageLocationType.LABEL,
                        Constants.MessageLocationType.STARRED
                        -> newFolderLocationId == Constants.MessageLocationType.TRASH
                        Constants.MessageLocationType.ALL_MAIL -> false
                        else -> newFolderLocationId != currentFolder
                    }
                )
            }
        }
    }

    private fun isActionAppliedToConversation(
        location: Constants.MessageLocationType?,
        shallIgnoreLocationInConversationResolution: Boolean = false
    ): Boolean {
        val locationForTypeDetermination = if (shallIgnoreLocationInConversationResolution) {
            null
        } else {
            location
        }
        return conversationModeEnabled(locationForTypeDetermination) &&
            !isApplyingActionToMessageWithinAConversation() &&
            !isApplyingActionToMessageItemInDetailScreen()
    }

    private fun isApplyingActionToMessageWithinAConversation(): Boolean {
        val actionsTarget = getActionsTargetInputArg()
        return actionsTarget == ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN
    }

    private fun isApplyingActionToMessageItemInDetailScreen(): Boolean {
        val actionsTarget = getActionsTargetInputArg()
        return actionsTarget == ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN
    }

    private fun getActionsTargetInputArg() = savedStateHandle.get<ActionSheetTarget>(
        MessageActionSheet.EXTRA_ARG_ACTION_TARGET
    ) ?: ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN

    private fun isAppInDarkMode(context: Context) = isAppInDarkMode.invoke(context)

    private suspend fun isWebViewInDarkMode(context: Context, messageId: String): Boolean {
        accountManager.getPrimaryUserId().first()?.let {
            return getViewInDarkModeMessagePreference(context, it, messageId)
        }
        return false
    }

    private suspend fun computeManageSectionState(
        context: Context,
        messageLocation: Constants.MessageLocationType,
        mailboxLocationId: String,
        messageIds: List<String>,
        actionsTarget: ActionSheetTarget
    ): MessageActionSheetState.ManageSectionState {
        val isStarred = savedStateHandle.get<Boolean>(MessageActionSheet.EXTRA_ARG_IS_STARRED) ?: false

        val showStarAction = actionsTarget in arrayOf(
            ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN,
            ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
        ) || !isStarred

        val showUnstarAction = actionsTarget in arrayOf(
            ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN,
            ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
        ) || isStarred

        val showMarkReadAction = actionsTarget == ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN

        val showViewInLightModeAction = actionsTarget in arrayOf(
            ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN,
            ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN
        ) && isAppInDarkMode(context) && isWebViewInDarkMode(context, messageIds.first())

        val showViewInDarkModeAction = actionsTarget in arrayOf(
            ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN,
            ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN
        ) && isAppInDarkMode(context) && !isWebViewInDarkMode(context, messageIds.first())

        return MessageActionSheetState.ManageSectionState(
            messageIds,
            messageLocation,
            mailboxLocationId,
            actionsTarget,
            showStarAction,
            showUnstarAction,
            showMarkReadAction,
            showViewInLightModeAction,
            showViewInDarkModeAction
        )
    }

    private fun computeMoveSectionState(
        actionsTarget: ActionSheetTarget,
        messageLocation: Constants.MessageLocationType,
        mailboxLocationId: String,
        messageIds: List<String>
    ): MessageActionSheetState.MoveSectionState {
        val isMoveToInboxVisible = if (actionsTarget != ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN) {
            messageLocation in Constants.MessageLocationType.values()
                .filter { type ->
                    type == Constants.MessageLocationType.ARCHIVE ||
                        type == Constants.MessageLocationType.SPAM ||
                        type == Constants.MessageLocationType.TRASH

                }
        } else true

        val isMoveToTrashVisible = messageLocation in Constants.MessageLocationType.values()
            .filter { type -> type != Constants.MessageLocationType.TRASH }

        val isMoveToArchiveVisible = if (actionsTarget != ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN) {
            messageLocation in Constants.MessageLocationType.values()
                .filter { type ->
                    type != Constants.MessageLocationType.ARCHIVE &&
                        type != Constants.MessageLocationType.SPAM
                }
        } else true

        val isMoveToSpamVisible = if (actionsTarget != ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN) {
            messageLocation in Constants.MessageLocationType.values()
                .filter { type ->
                    type != Constants.MessageLocationType.SPAM &&
                        type != Constants.MessageLocationType.DRAFT &&
                        type != Constants.MessageLocationType.SENT &&
                        type != Constants.MessageLocationType.TRASH
                }
        } else true

        val isDeleteVisible = messageLocation in Constants.MessageLocationType.values()
            .filter { type ->
                type == Constants.MessageLocationType.DRAFT ||
                    type == Constants.MessageLocationType.SENT ||
                    type == Constants.MessageLocationType.TRASH ||
                    type == Constants.MessageLocationType.SPAM
            }

        val moveSectionState = MessageActionSheetState.MoveSectionState(
            messageIds,
            messageLocation,
            mailboxLocationId,
            actionsTarget,
            isMoveToInboxVisible,
            isMoveToTrashVisible,
            isMoveToArchiveVisible,
            isMoveToSpamVisible,
            isDeleteVisible
        )
        return moveSectionState
    }

}
