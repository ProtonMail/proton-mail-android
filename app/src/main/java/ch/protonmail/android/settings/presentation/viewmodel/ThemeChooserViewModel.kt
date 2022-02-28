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

package ch.protonmail.android.settings.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.settings.domain.model.AppThemeSettings
import ch.protonmail.android.settings.domain.usecase.ApplyAppThemeFromSettings
import ch.protonmail.android.settings.domain.usecase.GetAppThemeSettings
import ch.protonmail.android.settings.domain.usecase.SaveAppThemeSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThemeChooserViewModel @Inject constructor(
    private val getAppThemeSettings: GetAppThemeSettings,
    private val saveAppThemeSettings: SaveAppThemeSettings,
    private val applyAppThemeFromSettings: ApplyAppThemeFromSettings
) : ViewModel() {

    val state: StateFlow<State> get() = mutableState.asStateFlow()
    private val mutableState = MutableStateFlow<State>(State.Loading)

    init {
        viewModelScope.launch {
            mutableState.value = State.Data(getAppThemeSettings())
        }
    }

    fun process(action: Action) {
        val theme = when (action) {
            Action.SetLightTheme -> AppThemeSettings.LIGHT
            Action.SetDarkTheme -> AppThemeSettings.DARK
            Action.SetSystemTheme -> AppThemeSettings.FOLLOW_SYSTEM
        }
        viewModelScope.launch {
            saveAppThemeSettings(theme)
            applyAppThemeFromSettings()
        }
    }

    sealed class State {

        object Loading : State()

        data class Data(val settings: AppThemeSettings) : State()
    }

    enum class Action {
        SetLightTheme,
        SetDarkTheme,
        SetSystemTheme
    }
}
