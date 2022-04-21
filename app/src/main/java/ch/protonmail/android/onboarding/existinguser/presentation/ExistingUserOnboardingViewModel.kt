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

package ch.protonmail.android.onboarding.existinguser.presentation

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.core.Constants
import ch.protonmail.android.di.DefaultSharedPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

@HiltViewModel
class ExistingUserOnboardingViewModel @Inject constructor(
    @DefaultSharedPreferences private val defaultSharedPreferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    fun saveOnboardingShown() {
        viewModelScope.launch(dispatchers.Io) {
            defaultSharedPreferences.edit()
                .putBoolean(Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN, true)
                .apply()
        }
    }
}
