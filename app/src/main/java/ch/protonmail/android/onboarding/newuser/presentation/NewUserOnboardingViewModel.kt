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

package ch.protonmail.android.onboarding.newuser.presentation

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN
import ch.protonmail.android.core.Constants.Prefs.PREF_NEW_USER_ONBOARDING_SHOWN
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.onboarding.base.presentation.OnboardingViewModel
import ch.protonmail.android.onboarding.base.presentation.model.OnboardingItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

@HiltViewModel
class NewUserOnboardingViewModel @Inject constructor(
    @DefaultSharedPreferences private val defaultSharedPreferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) : OnboardingViewModel() {

    override val onboardingState: Flow<OnboardingState> = flowOf(
        OnboardingState(
            listOf(
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_encryption,
                    R.string.new_user_onboarding_privacy_for_all_headline,
                    R.string.new_user_onboarding_privacy_for_all_description
                ),
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_labels_folders,
                    R.string.new_user_onboarding_neat_and_tidy_headline,
                    R.string.new_user_onboarding_neat_and_tidy_description
                )
            )
        )
    ).stateIn(viewModelScope, SharingStarted.Lazily, OnboardingState(emptyList()))

    override fun saveOnboardingShown() {
        viewModelScope.launch(dispatchers.Io) {
            defaultSharedPreferences.edit().putBoolean(PREF_NEW_USER_ONBOARDING_SHOWN, true).apply()
            // After new user onboarding is shown, we need to make sure the onboarding for existing users isn't shown
            defaultSharedPreferences.edit().putBoolean(PREF_EXISTING_USER_ONBOARDING_SHOWN, true).apply()
        }
    }
}
