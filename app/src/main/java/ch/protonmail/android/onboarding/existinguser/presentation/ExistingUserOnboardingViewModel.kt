/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.onboarding.existinguser.presentation

import android.content.SharedPreferences
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
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
class ExistingUserOnboardingViewModel @Inject constructor(
    @DefaultSharedPreferences private val defaultSharedPreferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) : OnboardingViewModel() {

    override val onboardingState: Flow<OnboardingState> = flowOf(
        OnboardingState(
            listOf(
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_encryption,
                    R.string.existing_user_onboarding_new_look_headline,
                    R.string.existing_user_onboarding_new_look_description
                ),
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_new_features,
                    R.string.existing_user_onboarding_new_features_headline,
                    R.string.existing_user_onboarding_new_features_description
                ),
                OnboardingItemUiModel(
                    R.drawable.ic_logo_mail,
                    R.string.existing_user_onboarding_updated_proton_headline,
                    R.string.existing_user_onboarding_updated_proton_description,
                    R.drawable.welcome_header_mail
                )
            )
        )
    ).stateIn(viewModelScope, SharingStarted.Lazily, OnboardingState(emptyList()))

    override fun saveOnboardingShown() {
        viewModelScope.launch(dispatchers.Io) {
            defaultSharedPreferences.edit()
                .putBoolean(Constants.Prefs.PREF_EXISTING_USER_ONBOARDING_SHOWN, true)
                .apply()
        }
    }
}
