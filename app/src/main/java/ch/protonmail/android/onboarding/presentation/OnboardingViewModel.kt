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

package ch.protonmail.android.onboarding.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.R
import ch.protonmail.android.onboarding.presentation.model.OnboardingItemUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor() : ViewModel() {

    val onboardingState = flowOf(
        OnboardingState(
            listOf(
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_encryption,
                    R.string.onboarding_privacy_for_all_headline,
                    R.string.onboarding_privacy_for_all_description
                ),
                OnboardingItemUiModel(
                    R.drawable.img_onboarding_labels_folders,
                    R.string.onboarding_neat_and_tidy_headline,
                    R.string.onboarding_neat_and_tidy_description
                )
            )
        )
    ).stateIn(viewModelScope, SharingStarted.Lazily, OnboardingState(emptyList()))

    data class OnboardingState(val onboardingItemsList: List<OnboardingItemUiModel>)
}
