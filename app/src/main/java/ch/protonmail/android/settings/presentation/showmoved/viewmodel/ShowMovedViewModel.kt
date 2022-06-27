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

package ch.protonmail.android.settings.presentation.showmoved.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.settings.domain.usecase.GetMailSettings
import ch.protonmail.android.settings.domain.usecase.UpdateShowMoved
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.mailsettings.domain.entity.ShowMoved
import javax.inject.Inject

@HiltViewModel
class ShowMovedViewModel @Inject constructor(
    private val accountManager: AccountManager,
    private var updateShowMoved: UpdateShowMoved,
    private var getMailSettings: GetMailSettings
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = mutableState.asStateFlow()

    fun setSettingCurrentValue() {
        viewModelScope.launch {
            val userId = accountManager.getPrimaryUserId().first()
                ?: return@launch
            getMailSettings(userId)
                .filterIsInstance<GetMailSettings.Result.Success>()
                .onEach { result ->
                    result.mailSettings.let { mailSettings ->
                        mutableState.emit(State.Fetched(mailSettings.showMoved?.enum))
                    }
                }.launchIn(viewModelScope)
        }
    }

    fun onToggle(value: ShowMoved) {
        viewModelScope.launch {
            mutableState.emit(State.Saving)
            accountManager.getPrimaryUserId().first()?.let { userId ->
                val newState = when (updateShowMoved(userId, value)) {
                    UpdateShowMoved.Result.Success -> State.Success
                    UpdateShowMoved.Result.Error -> State.GenericError
                }
                mutableState.emit(newState)
            }
        }
    }

    sealed class State {

        object Idle : State()

        /**
         * Setting is successfully fetched
         */
        data class Fetched(public val showMoved: ShowMoved?) : State()

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
