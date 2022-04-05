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

package ch.protonmail.android.onboarding.base.presentation

import androidx.recyclerview.widget.DiffUtil
import ch.protonmail.android.onboarding.base.presentation.model.OnboardingItemUiModel

class OnboardingItemUiModelDiffCallback : DiffUtil.ItemCallback<OnboardingItemUiModel>() {

    override fun areItemsTheSame(oldItem: OnboardingItemUiModel, newItem: OnboardingItemUiModel): Boolean =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: OnboardingItemUiModel, newItem: OnboardingItemUiModel): Boolean =
        oldItem == newItem
}
