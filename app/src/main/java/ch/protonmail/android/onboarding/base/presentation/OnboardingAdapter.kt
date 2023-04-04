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

package ch.protonmail.android.onboarding.base.presentation

import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.databinding.LayoutOnboardingItemBinding
import ch.protonmail.android.onboarding.base.presentation.model.OnboardingItemUiModel
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
        binding.onboardingDescriptionTextView.movementMethod = LinkMovementMethod()
        binding.onboardingIndicatorsView.bind(position, itemCount)
        if (onboardingItemUiModel.onboardingImageBackground != null) {
            binding.onboardingBackgroundView.setBackgroundResource(onboardingItemUiModel.onboardingImageBackground)
            binding.onboardingImageView.isVisible = false
            binding.onboardingLogoImageView.isVisible = false
        } else {
            binding.onboardingBackgroundView.setBackgroundResource(R.color.onboarding_image_background)
            binding.onboardingImageView.isVisible = true
            binding.onboardingLogoImageView.isVisible = false
        }
    }
}
