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

package ch.protonmail.android.labels.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageLocationType.ARCHIVE
import ch.protonmail.android.core.Constants.MessageLocationType.INVALID
import ch.protonmail.android.core.Constants.MessageLocationType.TRASH
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.labels.domain.model.ManageLabelActionResult
import ch.protonmail.android.labels.domain.usecase.GetAllLabels
import ch.protonmail.android.labels.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.labels.domain.usecase.UpdateLabels
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.UpdateConversationsLabels
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LabelsActionSheetViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getAllLabels: GetAllLabels,
    private val userManager: UserManager,
    private val updateLabels: UpdateLabels,
    private val updateConversationsLabels: UpdateConversationsLabels,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val moveConversationsToFolder: MoveConversationsToFolder,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val messageRepository: MessageRepository,
    private val conversationsRepository: ConversationsRepository
) : ViewModel() {

    private val labelsSheetType = savedStateHandle.get<LabelsActionSheet.Type>(
        LabelsActionSheet.EXTRA_ARG_ACTION_SHEET_TYPE
    ) ?: LabelsActionSheet.Type.LABEL

    private val currentMessageFolder =
        Constants.MessageLocationType.fromInt(
            savedStateHandle.get<Int>(LabelsActionSheet.EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID) ?: 0
        )

    private val messageIds = savedStateHandle.get<List<String>>(LabelsActionSheet.EXTRA_ARG_MESSAGES_IDS)
        ?: emptyList()

    private val actionSheetTarget = savedStateHandle.get<ActionSheetTarget>(LabelsActionSheet.EXTRA_ARG_ACTION_TARGET)
        ?: ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN

    private val labelsMutableFlow = MutableStateFlow(emptyList<LabelActonItemUiModel>())
    private val actionsResultMutableFlow = MutableStateFlow<ManageLabelActionResult>(ManageLabelActionResult.Default)

    val labels: StateFlow<List<LabelActonItemUiModel>>
        get() = labelsMutableFlow

    val actionsResult: StateFlow<ManageLabelActionResult>
        get() = actionsResultMutableFlow

    init {
        viewModelScope.launch {
            labelsMutableFlow.value = getAllLabels(
                getCheckedLabelsForAllMailboxItems(messageIds),
                labelsSheetType,
                if (actionSheetTarget != ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN ||
                    currentMessageFolder == TRASH
                ) currentMessageFolder
                else INVALID
            )
        }
    }

    fun onLabelClicked(model: LabelActonItemUiModel, currentFolderLocationId: Int) {

        if (model.labelType == LabelsActionSheet.Type.FOLDER.typeInt) {
            onFolderClicked(model.labelId)
        } else {
            // label type clicked
            val updatedLabels = labels.value
                .filter { it.labelType == LabelsActionSheet.Type.LABEL.typeInt }
                .map { label ->
                    if (label.labelId == model.labelId) {
                        Timber.v("Label: ${label.labelId} was clicked")
                        label.copy(isChecked = model.isChecked?.not())
                    } else {
                        label
                    }
                }

            val selectedLabelsCount = updatedLabels.filter { it.isChecked == true }
            if (selectedLabelsCount.isNotEmpty() &&
                userManager.didReachLabelsThreshold(selectedLabelsCount.size)
            ) {
                actionsResultMutableFlow.value =
                    ManageLabelActionResult.ErrorLabelsThresholdReached(userManager.getMaxLabelsAllowed())
            } else {
                labelsMutableFlow.value = updatedLabels
                actionsResultMutableFlow.value = ManageLabelActionResult.Default
            }
        }
    }

    fun onDoneClicked(shallMoveToArchive: Boolean = false) {
        if (labelsSheetType == LabelsActionSheet.Type.LABEL) {
            onLabelDoneClicked(messageIds, shallMoveToArchive)
        } else {
            throw IllegalStateException("This action is unsupported for type $labelsSheetType")
        }
    }

    private fun onLabelDoneClicked(ids: List<String>, shallMoveToArchive: Boolean) {
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                val selectedLabels = labels.value
                    .filter { it.isChecked == true }
                    .map { it.labelId }
                val unselectedLabels = labels.value
                    .filter { it.isChecked == false }
                    .map { it.labelId }
                Timber.v("Selected labels: $selectedLabels messageId: $ids")
                if (isActionAppliedToConversation(currentMessageFolder)) {
                    val result = updateConversationsLabels(
                        ids,
                        UserId(userManager.requireCurrentUserId().s),
                        selectedLabels,
                        unselectedLabels
                    )
                    if (result is ConversationsActionResult.Error) {
                        cancel("Could not complete the action")
                    }
                } else {
                    ids.forEach { id ->
                        updateLabels(
                            id,
                            selectedLabels
                        )
                    }
                }

                if (shallMoveToArchive) {
                    if (isActionAppliedToConversation(currentMessageFolder)) {
                        val result = moveConversationsToFolder(
                            ids,
                            UserId(userManager.requireCurrentUserId().s),
                            ARCHIVE.messageLocationTypeValue.toString(),
                        )
                        if (result is ConversationsActionResult.Error) {
                            cancel("Could not complete the action")
                        }
                    } else {
                        moveMessagesToFolder(
                            ids,
                            ARCHIVE.messageLocationTypeValue.toString(),
                            currentMessageFolder.messageLocationTypeValue.toString()
                        )
                    }
                }
            }.invokeOnCompletion { cancellationException ->
                if (cancellationException != null) {
                    actionsResultMutableFlow.value = ManageLabelActionResult.ErrorUpdatingLabels
                } else {
//                    actionsResultMutableFlow.value = ManageLabelActionResult.LabelsSuccessfullySaved()
                    val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
                    actionsResultMutableFlow.value = ManageLabelActionResult.LabelsSuccessfullySaved(
                        dismissBackingActivity
                    )
                }
            }
        } else {
            Timber.i("Cannot continue messages list is null or empty!")
        }
    }

    private fun onFolderClicked(selectedFolderId: String) {
        viewModelScope.launch {
            // ignore location here, otherwise custom folder case does not work
            if (isActionAppliedToConversation(null)) {
                userManager.currentUserId?.let {
                    val result = moveConversationsToFolder(messageIds, UserId(it.s), selectedFolderId)
                    if (result is ConversationsActionResult.Error) {
                        cancel("Could not complete the action")
                    }
                }
            } else {
                moveMessagesToFolder(
                    messageIds, selectedFolderId, currentMessageFolder.messageLocationTypeValue.toString()
                )
            }
        }.invokeOnCompletion { cancellationException ->
            if (cancellationException != null) {
                actionsResultMutableFlow.value = ManageLabelActionResult.ErrorMovingToFolder
            } else {
                val dismissBackingActivity = !isApplyingActionToMessageWithinAConversation()
                actionsResultMutableFlow.value = ManageLabelActionResult.MessageSuccessfullyMoved(
                    dismissBackingActivity
                )
            }
        }
    }

    private suspend fun getCheckedLabelsForAllMailboxItems(
        ids: List<String>
    ): List<String> {
        val checkedLabels = mutableListOf<String>()
        ids.forEach { id ->
            if (isActionAppliedToConversation(currentMessageFolder)) {
                conversationsRepository.findConversation(id, userManager.requireCurrentUserId())?.let { conversation ->
                    checkedLabels.addAll(conversation.labels.map { label -> label.id })
                }
            } else {
                val message = messageRepository.findMessageById(id)
                Timber.v("Checking message labels: ${message?.labelIDsNotIncludingLocations}")
                message?.labelIDsNotIncludingLocations?.let {
                    checkedLabels.addAll(it)
                }
            }
        }
        return checkedLabels
    }

    private fun isActionAppliedToConversation(location: Constants.MessageLocationType?) =
        conversationModeEnabled(location) && !isApplyingActionToMessageWithinAConversation()

    private fun isApplyingActionToMessageWithinAConversation(): Boolean {
        val actionsTarget = getActionsTargetInputArg()
        return actionsTarget == ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN
    }

    private fun getActionsTargetInputArg() = savedStateHandle.get<ActionSheetTarget>(
        LabelsActionSheet.EXTRA_ARG_ACTION_TARGET
    ) ?: ActionSheetTarget.MAILBOX_ITEM_IN_DETAIL_SCREEN

}
