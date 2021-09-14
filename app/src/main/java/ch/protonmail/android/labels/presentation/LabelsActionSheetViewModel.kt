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

package ch.protonmail.android.labels.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.labels.data.remote.worker.UpdateConversationsLabelsWorker
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.domain.model.ManageLabelActionResult
import ch.protonmail.android.labels.domain.usecase.GetLabelsByType
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.labels.domain.usecase.UpdateMessageLabels
import ch.protonmail.android.labels.presentation.mapper.LabelDomainActionItemUiMapper
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.model.StandardFolderLocation
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.domain.arch.onSuccess
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

private const val MAX_NUMBER_OF_SELECTED_LABELS = 100

@HiltViewModel
internal class LabelsActionSheetViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val getLabelsByType: GetLabelsByType,
    private val userManager: UserManager,
    private val updateMessageLabels: UpdateMessageLabels,
    private val updateConversationsLabels: UpdateConversationsLabelsWorker.Enqueuer,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val moveConversationsToFolder: MoveConversationsToFolder,
    private val conversationModeEnabled: ConversationModeEnabled,
    private val messageRepository: MessageRepository,
    private val conversationsRepository: ConversationsRepository,
    private val labelDomainUiMapper: LabelDomainActionItemUiMapper
) : ViewModel() {

    private val labelsSheetType = savedStateHandle.get<LabelType>(
        LabelsActionSheet.EXTRA_ARG_ACTION_SHEET_TYPE
    ) ?: LabelType.MESSAGE_LABEL

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
            val allLabels = getLabelsByType(labelsSheetType)

            val savedModels = allLabels.map { label ->
                labelDomainUiMapper.toActionItemUi(
                    label,
                    getCheckedLabelsForAllMailboxItems(messageIds),
                    labelsSheetType
                )
            }

            labelsMutableFlow.value = getAllModels(
                if (actionSheetTarget != ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN ||
                    currentMessageFolder == Constants.MessageLocationType.TRASH
                ) currentMessageFolder
                else Constants.MessageLocationType.INVALID,
                labelsSheetType,
                savedModels
            )

        }
    }

    private fun getAllModels(
        currentMessageFolder: Constants.MessageLocationType?,
        labelsSheetType: LabelType,
        uiLabelsFromDb: List<LabelActonItemUiModel>
    ): List<LabelActonItemUiModel> {
        return if (labelsSheetType == LabelType.FOLDER) {
            requireNotNull(currentMessageFolder)
            uiLabelsFromDb + getStandardFolders(currentMessageFolder)
        } else
            uiLabelsFromDb
    }


    private fun getStandardFolders(
        currentMessageFolder: Constants.MessageLocationType
    ): List<LabelActonItemUiModel> {
        return when (currentMessageFolder) {
            Constants.MessageLocationType.INBOX,
            Constants.MessageLocationType.ARCHIVE,
            Constants.MessageLocationType.SPAM,
            Constants.MessageLocationType.TRASH -> getListWithoutType(currentMessageFolder)
            else -> getListWithoutType()
        }
    }

    private fun getListWithoutType(
        currentMessageFolder: Constants.MessageLocationType = Constants.MessageLocationType.INVALID
    ) = StandardFolderLocation.values()
        .filter {
            it.id != currentMessageFolder.messageLocationTypeValue.toString()
        }
        .map { location ->
            LabelActonItemUiModel(
                labelId = LabelId(location.id),
                iconRes = location.iconRes,
                titleRes = location.title,
                labelType = LabelType.FOLDER
            )
        }

    fun onLabelClicked(model: LabelActonItemUiModel) {

        if (model.labelType == LabelType.FOLDER) {
            onFolderClicked(model.labelId.id)
        } else {
            // label type clicked
            val updatedLabels = labels.value
                .filter { it.labelType == LabelType.MESSAGE_LABEL }
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
                selectedLabelsCount.size > MAX_NUMBER_OF_SELECTED_LABELS
            ) {
                actionsResultMutableFlow.value =
                    ManageLabelActionResult.ErrorLabelsThresholdReached(MAX_NUMBER_OF_SELECTED_LABELS)
            } else {
                labelsMutableFlow.value = updatedLabels
                actionsResultMutableFlow.value = ManageLabelActionResult.Default
            }
        }
    }

    fun onDoneClicked(shallMoveToArchive: Boolean = false) {
        if (labelsSheetType == LabelType.MESSAGE_LABEL) {
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
                Timber.v("Selected labels: $selectedLabels messageId: $ids")
                if (isActionAppliedToConversation(currentMessageFolder)) {
                    updateConversationsLabels.enqueue(
                        ids,
                        userManager.requireCurrentUserId(),
                        selectedLabels.map { it.id }
                    )
                } else {
                    ids.forEach { id ->
                        updateMessageLabels(
                            id,
                            selectedLabels.map { it.id }
                        )
                    }
                }

                if (shallMoveToArchive) {
                    if (isActionAppliedToConversation(currentMessageFolder)) {
                        val result = moveConversationsToFolder(
                            ids,
                            UserId(userManager.requireCurrentUserId().id),
                            Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString(),
                        )
                        if (result is ConversationsActionResult.Error) {
                            cancel("Could not complete the action")
                        }
                    } else {
                        moveMessagesToFolder(
                            ids,
                            Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString(),
                            currentMessageFolder.messageLocationTypeValue.toString(),
                            userManager.requireCurrentUserId()
                        )
                    }
                }
            }.invokeOnCompletion { cancellationException ->
                if (cancellationException != null) {
                    actionsResultMutableFlow.value = ManageLabelActionResult.ErrorUpdatingLabels
                } else {
                    actionsResultMutableFlow.value = ManageLabelActionResult.LabelsSuccessfullySaved
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
                    val result = moveConversationsToFolder(messageIds, UserId(it.id), selectedFolderId)
                    if (result is ConversationsActionResult.Error) {
                        cancel("Could not complete the action")
                    }
                }
            } else {
                moveMessagesToFolder(
                    messageIds,
                    selectedFolderId,
                    currentMessageFolder.messageLocationTypeValue.toString(),
                    userManager.requireCurrentUserId()
                )
            }
        }.invokeOnCompletion { cancellationException ->
            if (cancellationException != null) {
                actionsResultMutableFlow.value = ManageLabelActionResult.ErrorMovingToFolder
            } else {
                val dismissBackingActivity = !isApplyingActionToMsgWithinAConversation()
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
                conversationsRepository.getConversation(userManager.requireCurrentUserId(), id)
                    .first()
                    .onSuccess { conversation ->
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
        conversationModeEnabled(location) &&
            !isApplyingActionToMsgWithinAConversation() &&
            !isApplyingActionToMsgItemInDetailScreen()

    private fun isApplyingActionToMsgWithinAConversation(): Boolean {
        val actionsTarget = getActionsTargetInputArg()
        return actionsTarget == ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN
    }

    private fun isApplyingActionToMsgItemInDetailScreen(): Boolean {
        val actionsTarget = getActionsTargetInputArg()
        return actionsTarget == ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN
    }

    private fun getActionsTargetInputArg() = savedStateHandle.get<ActionSheetTarget>(
        LabelsActionSheet.EXTRA_ARG_ACTION_TARGET
    ) ?: ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN

}
