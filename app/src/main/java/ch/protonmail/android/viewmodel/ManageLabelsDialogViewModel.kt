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

package ch.protonmail.android.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ch.protonmail.android.adapters.LabelsAdapter
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowMissingColorError
import ch.protonmail.android.views.ThreeStateButton
import javax.inject.Inject
import javax.inject.Named

class ManageLabelsDialogViewModel @Inject constructor(
    @Named("messages") private val messagesDao: MessagesDao,
    private val userManager: UserManager
) : ViewModel() {

    val viewState: MutableLiveData<ViewState> = MutableLiveData()

    fun onDoneClicked(
        isCreationMode: Boolean,
        labelColor: String?,
        checkedLabelIds: List<String>,
        archiveCheckboxState: Int,
        labelName: String,
        labelItemsList: List<LabelsAdapter.LabelItem>
    ) {

        if (isCreationMode) {
            createLabel(labelName, labelColor, labelItemsList)
            return
        }

        if (userManager.didReachLabelsThreshold(checkedLabelIds.size)) {
            liveDataShowLabelsThresholdError(userManager.getMaxLabelsAllowed())
            return
        }

        if (isArchiveOptionSelected(archiveCheckboxState)) {
            liveDataPostSelectedLabelsChangedArchive()
        } else {
            liveDataPostSelectedLabelsChanged()
        }

        liveDataHideLabelsView()
    }

    fun onTextChanged(labelName: String, creationViewsVisible: Boolean) {

        if (labelName.isNotEmpty()) {
            if (creationViewsVisible) {
                return
            }

            liveDataShowLabelCreationViews()
            return
        }

        liveDataHideLabelCreationViews()
    }

    private fun liveDataHideLabelCreationViews() {
        viewState.value = ViewState.HideLabelCreationViews
    }

    private fun liveDataShowLabelCreationViews() {
        viewState.value = ViewState.ShowLabelCreationViews
    }

    private fun liveDataHideLabelsView() {
        viewState.value = ViewState.HideLabelsView
    }

    private fun liveDataPostSelectedLabelsChanged() {
        viewState.value = ViewState.SelectedLabelsChangedEvent
    }

    private fun liveDataPostSelectedLabelsChangedArchive() {
        viewState.value = ViewState.SelectedLabelsChangedArchive
    }

    private fun liveDataShowLabelsThresholdError(maxLabelsAllowed: Int) {
        viewState.value = ViewState.ShowApplicableLabelsThresholdExceededError(maxLabelsAllowed)
    }

    private fun isArchiveOptionSelected(archiveCheckboxState: Int) =
        archiveCheckboxState == ThreeStateButton.STATE_CHECKED ||
            archiveCheckboxState == ThreeStateButton.STATE_PRESSED

    private fun createLabel(
        labelName: String,
        labelColor: String?,
        labelItemsList: List<LabelsAdapter.LabelItem>
    ) {
        if (labelColor.isNullOrEmpty()) {
            liveDataShowMissingColorError()
            return
        }

        if (labelName.isEmpty()) {
            liveDataShowMissingNameError()
            return
        }

        labelItemsList
            .find { it.name == labelName }
            ?.let {
                liveDataShowDuplicatedNameError()
                return
            }

        liveDataShowLabelCreatedEvent(labelName)
    }

    private fun liveDataShowLabelCreatedEvent(labelName: String) {
        viewState.value = ViewState.ShowLabelCreatedEvent(labelName)
    }

    private fun liveDataShowDuplicatedNameError() {
        viewState.value = ViewState.ShowLabelNameDuplicatedError
    }

    private fun liveDataShowMissingNameError() {
        viewState.value = ViewState.ShowMissingNameError
    }

    private fun liveDataShowMissingColorError() {
        viewState.value = ShowMissingColorError
    }

    sealed class ViewState {
        object ShowMissingColorError : ViewState()
        object ShowMissingNameError : ViewState()
        object ShowLabelNameDuplicatedError : ViewState()
        object SelectedLabelsChangedEvent : ViewState()
        object SelectedLabelsChangedArchive : ViewState()
        object HideLabelsView : ViewState()
        object ShowLabelCreationViews : ViewState()
        object HideLabelCreationViews : ViewState()
        class ShowApplicableLabelsThresholdExceededError(val maxLabelsAllowed: Int) : ViewState()
        class ShowLabelCreatedEvent(val labelName: String) : ViewState()
    }

}
