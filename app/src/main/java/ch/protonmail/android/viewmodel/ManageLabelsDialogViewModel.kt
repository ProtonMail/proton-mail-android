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
import androidx.lifecycle.ViewModelProvider
import ch.protonmail.android.adapters.LabelsAdapter
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.HideLabelCreationViews
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.HideLabelsView
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.SelectedLabelsArchiveEvent
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.SelectedLabelsChangedEvent
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowApplicableLabelsThresholdExceededError
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowLabelCreatedEvent
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowLabelCreationViews
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowLabelNameDuplicatedError
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowMissingColorError
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel.ViewState.ShowMissingNameError
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
            viewState.value = createLabel(labelName, labelColor, labelItemsList)
            return
        }

        if (userManager.didReachLabelsThreshold(checkedLabelIds.size)) {
            liveDataShowLabelsThresholdError(userManager.getMaxLabelsAllowed())
            return
        }

        if (isArchiveOptionSelected(archiveCheckboxState)) {
            viewState.value = SelectedLabelsArchiveEvent
        } else {
            viewState.value = SelectedLabelsChangedEvent
        }

        viewState.value = HideLabelsView
    }

    fun onTextChanged(labelName: String, creationViewsVisible: Boolean) {

        if (labelName.isNotEmpty()) {
            if (creationViewsVisible) {
                return
            }

            viewState.value = ShowLabelCreationViews
            return
        }

        viewState.value = HideLabelCreationViews
    }

    private fun liveDataShowLabelsThresholdError(maxLabelsAllowed: Int) {
        viewState.value = ShowApplicableLabelsThresholdExceededError(maxLabelsAllowed)
    }

    private fun isArchiveOptionSelected(archiveCheckboxState: Int) =
        archiveCheckboxState == ThreeStateButton.STATE_CHECKED ||
            archiveCheckboxState == ThreeStateButton.STATE_PRESSED

    private fun createLabel(
        labelName: String,
        labelColor: String?,
        labelItemsList: List<LabelsAdapter.LabelItem>
    ): ViewState {
        if (labelColor.isNullOrEmpty()) {
            return ShowMissingColorError
        }

        if (labelName.isEmpty()) {
            return ShowMissingNameError
        }

        labelItemsList
            .find { it.name == labelName }
            ?.let {
                return ShowLabelNameDuplicatedError
            }

        return ShowLabelCreatedEvent(labelName)
    }

    sealed class ViewState {
        object ShowMissingColorError : ViewState()
        object ShowMissingNameError : ViewState()
        object ShowLabelNameDuplicatedError : ViewState()
        object SelectedLabelsChangedEvent : ViewState()
        object SelectedLabelsArchiveEvent : ViewState()
        object HideLabelsView : ViewState()
        object ShowLabelCreationViews : ViewState()
        object HideLabelCreationViews : ViewState()
        class ShowApplicableLabelsThresholdExceededError(val maxLabelsAllowed: Int) : ViewState()
        class ShowLabelCreatedEvent(val labelName: String) : ViewState()
    }

    class ManageLabelsDialogViewModelFactory @Inject constructor(
        private val manageLabelsViewModel: ManageLabelsDialogViewModel,
        @Named("messages") private val messagesDao: MessagesDao
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ManageLabelsDialogViewModel::class.java)) {
                return manageLabelsViewModel as T
            }
            throw IllegalArgumentException("Cannot assign ManageLabelsDialogViewModel from given class name")
        }
    }
}
