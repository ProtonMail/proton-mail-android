/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.settings.swipe.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.settings.domain.usecase.UpdateSwipeActions
import ch.protonmail.android.settings.swipe.EXTRA_SWIPE_ID
import ch.protonmail.android.settings.swipe.SwipeType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.mailsettings.domain.entity.SwipeAction
import javax.inject.Inject

@HiltViewModel
class SwipeChooserViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val accountManager: AccountManager,
    private var updateSwipeActions: UpdateSwipeActions
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = mutableState.asStateFlow()

    private var actionToSave: SwipeAction? = null

    private val swipeType: SwipeType =
        requireNotNull(savedStateHandle.get<SwipeType>(EXTRA_SWIPE_ID)) {
            "Extra 'EXTRA_SWIPE_ID' is required"
        }

    fun setAction(swipeAction: SwipeAction) {
        actionToSave = swipeAction
    }

    fun onSaveClicked() {
        if (state.value != State.Idle) return
        if (actionToSave == null) {
            mutableState.tryEmit(State.Success)
            return
        }

        viewModelScope.launch {
            mutableState.emit(State.Saving)
            accountManager.getPrimaryUserId().first()?.let { userId ->
                val result = when (swipeType) {
                    SwipeType.LEFT -> updateSwipeActions(userId, swipeLeft = actionToSave)
                    SwipeType.RIGHT -> updateSwipeActions(userId, swipeRight = actionToSave)
                }
                val newState = when (result) {
                    UpdateSwipeActions.Result.Success -> State.Success
                    UpdateSwipeActions.Result.Error -> State.GenericError
                }
                mutableState.emit(newState)
            }
        }
    }

    sealed class State {

        object Idle : State()

        /**
         * Setting is being saved online
         */
        object Saving : State()

        /**
         * New value has been saved, we close the Activity
         */
        object Success : State()

        /**
         * New value cannot be saved because generic error, we show a message and close the Activity
         */
        object GenericError : State()
    }
}
