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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerAction
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerItemUiModel
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

// TODO: MAILAND-2614 uncomment after test with dummy state @HiltViewModel
class ParentFolderPickerViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    // TODO: MAILAND-2614 for test only, replace with use case
    initialState: ParentFolderPickerState = ParentFolderPickerState.Editing(
        selectedItemId = null,
        items = listOf(
            ParentFolderPickerItemUiModel.None(isSelected = true)
        )
    )
) : ViewModel() {

    val state = MutableStateFlow(initialState)

    fun process(action: ParentFolderPickerAction) {
        viewModelScope.launch {
            val newState = when (action) {
                is ParentFolderPickerAction.SetSelected -> setSelected(action.folderId)
                is ParentFolderPickerAction.SaveAndClose -> saveAndClose()
            }
            state.emit(newState)
        }
    }

    private suspend fun setSelected(folderId: LabelId?): ParentFolderPickerState {
        val prevState = state.value
        if (folderId == prevState.selectedItemId) {
            return prevState
        }

        return when (prevState) {
            is ParentFolderPickerState.Editing -> prevState.updateSelectedItem(folderId)
            is ParentFolderPickerState.SavingAndClose -> {
                Timber.w("Previous state is 'SavingAndClose', ignoring the current change")
                prevState
            }
        }
    }

    private suspend fun ParentFolderPickerState.Editing.updateSelectedItem(
        folderId: LabelId?
    ): ParentFolderPickerState.Editing = withContext(dispatchers.Comp) {
        val newItems = items.map { item ->
            val shouldItBeSelected = item.id == folderId

            if (shouldItBeSelected == item.isSelected) item
            else when (item) {
                is ParentFolderPickerItemUiModel.Folder -> item.copy(isSelected = shouldItBeSelected)
                is ParentFolderPickerItemUiModel.None -> item.copy(isSelected = shouldItBeSelected)
            }
        }
        copy(selectedItemId = folderId, items = newItems)
    }

    private suspend fun saveAndClose(): ParentFolderPickerState =
        withContext(dispatchers.Comp) {
            val prevState = state.value
            ParentFolderPickerState.SavingAndClose(selectedItemId = prevState.selectedItemId)
        }
}
