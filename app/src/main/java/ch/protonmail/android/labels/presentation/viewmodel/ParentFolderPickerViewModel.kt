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
import ch.protonmail.android.labels.domain.usecase.ObserveFoldersEligibleAsParent
import ch.protonmail.android.labels.presentation.mapper.ParentFolderPickerItemUiModelMapper
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerAction
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerItemUiModel
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ParentFolderPickerViewModel @Inject constructor(
    private val dispatchers: DispatcherProvider,
    accountManager: AccountManager,
    private val observeFoldersEligibleAsParent: ObserveFoldersEligibleAsParent,
    private val mapper: ParentFolderPickerItemUiModelMapper
) : ViewModel() {

    val state: StateFlow<ParentFolderPickerState> get() =
        mutableState.asStateFlow()

    private val mutableState: MutableStateFlow<ParentFolderPickerState> =
        MutableStateFlow(ParentFolderPickerState.Loading(selectedItemId = null))

    init {
        accountManager.getPrimaryUserId()
            .filterNotNull()
            .flatMapLatest { observeFoldersEligibleAsParent(userId = it) }
            .mapLatest { folders ->
                val currentSelectedFolder = state.value.selectedItemId
                mapper.toUiModels(folders, currentSelectedFolder, includeNoneUiModel = true)
            }
            .onEach { mutableState.updateItemsIfNeeded(newItems = it) }
            .launchIn(viewModelScope)
    }

    fun process(action: ParentFolderPickerAction) {
        viewModelScope.launch {
            val newState = when (action) {
                is ParentFolderPickerAction.SetSelected -> setSelected(action.folderId)
                is ParentFolderPickerAction.SaveAndClose -> saveAndClose()
            }
            mutableState.emit(newState)
        }
    }

    private suspend fun setSelected(folderId: LabelId?): ParentFolderPickerState {
        val prevState = state.value
        if (folderId == prevState.selectedItemId) {
            return prevState
        }

        return when (prevState) {
            is ParentFolderPickerState.Loading -> prevState.copy(selectedItemId = folderId)
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

private fun MutableStateFlow<ParentFolderPickerState>.updateItemsIfNeeded(
    newItems: List<ParentFolderPickerItemUiModel>
) {
    val prevState = value
    val newState = when (prevState) {
        is ParentFolderPickerState.Loading -> ParentFolderPickerState.Editing(
            selectedItemId = prevState.selectedItemId,
            items = newItems
        )
        is ParentFolderPickerState.Editing -> prevState.copy(items = newItems)
        is ParentFolderPickerState.SavingAndClose -> prevState
    }
    if (newState !== prevState) tryEmit(newState)
}
