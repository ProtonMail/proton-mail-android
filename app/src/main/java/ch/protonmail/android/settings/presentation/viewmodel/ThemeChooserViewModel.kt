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

    val state: StateFlow<AppThemeSettings> get() = mutableState.asStateFlow()
    private val mutableState = MutableStateFlow(AppThemeSettings.FOLLOW_SYSTEM)

    init {
        viewModelScope.launch {
            mutableState.value = getAppThemeSettings()
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

    enum class Action {
        SetLightTheme,
        SetDarkTheme,
        SetSystemTheme
    }
}
