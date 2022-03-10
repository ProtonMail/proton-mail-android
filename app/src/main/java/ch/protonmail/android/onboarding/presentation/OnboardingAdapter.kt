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

import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import ch.protonmail.android.databinding.LayoutOnboardingItemBinding
import ch.protonmail.android.onboarding.presentation.model.OnboardingItemUiModel
import me.proton.core.presentation.ui.adapter.ClickableAdapter
import me.proton.core.presentation.ui.adapter.ProtonAdapter

class OnboardingAdapter : ProtonAdapter<OnboardingItemUiModel, LayoutOnboardingItemBinding, OnboardingViewHolder>(
    {}, {}, true,
    OnboardingItemUiModelDiffCallback(),
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val binding = LayoutOnboardingItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return OnboardingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {
        holder.bind(getItem(position), position, itemCount)
    }
}

class OnboardingViewHolder(val binding: LayoutOnboardingItemBinding) :
    ClickableAdapter.ViewHolder<OnboardingItemUiModel, LayoutOnboardingItemBinding>(binding, {}, {}) {

    fun bind(onboardingItemUiModel: OnboardingItemUiModel, position: Int, itemCount: Int) {
        binding.onboardingImageView.setImageResource(onboardingItemUiModel.onboardingImage)
        binding.onboardingHeadlineTextView.setText(onboardingItemUiModel.onboardingHeadline)
        binding.onboardingDescriptionTextView.setText(onboardingItemUiModel.onboardingDescription)
        binding.onboardingDescriptionTextView.movementMethod = ScrollingMovementMethod()
        binding.onboardingIndicatorsView.bind(position, itemCount)
    }
}
