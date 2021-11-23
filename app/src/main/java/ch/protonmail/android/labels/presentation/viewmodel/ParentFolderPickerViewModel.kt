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
import javax.inject.Inject

// TODO: uncomment after test with dummy state @HiltViewModel
class ParentFolderPickerViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    // TODO: for test only, replace with use case
    initialState: ParentFolderPickerState = ParentFolderPickerState(
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
            }
            state.emit(newState)
        }
    }

    private suspend fun setSelected(folderId: LabelId?): ParentFolderPickerState =
        withContext(dispatchers.Comp) {
            val prevState = state.value
            if (folderId == prevState.selectedItemId) {
                return@withContext prevState
            }

            val newItems = prevState.items.map { item ->
                val shouldItBeSelected = item.id == folderId

                if (shouldItBeSelected == item.isSelected) item
                else when (item) {
                    is ParentFolderPickerItemUiModel.Folder -> item.copy(isSelected = shouldItBeSelected)
                    is ParentFolderPickerItemUiModel.None -> item.copy(isSelected = shouldItBeSelected)
                }
            }
            return@withContext prevState.copy(selectedItemId = folderId, items = newItems)
        }
}
