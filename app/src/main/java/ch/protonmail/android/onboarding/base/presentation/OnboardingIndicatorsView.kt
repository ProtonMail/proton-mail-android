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

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.annotation.StyleRes
import androidx.appcompat.content.res.AppCompatResources
import ch.protonmail.android.R
import ch.protonmail.android.utils.extensions.isInPreviewMode

/**
 * A container view for the dots indicating which page of the onboarding we are on.
 */
class OnboardingIndicatorsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : LinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    init {
        orientation = HORIZONTAL
        if (isInPreviewMode()) {
            bind(0, 2)
        }
    }

    fun bind(positionOfCurrentIndicator: Int, numberOfIndicators: Int) {
        repeat(numberOfIndicators) {
            val imageDrawable = if (it == positionOfCurrentIndicator) {
                R.drawable.circle_onboarding_indicator_active
            } else {
                R.drawable.circle_onboarding_indicator_inactive
            }
            val imageView = ImageView(context)
            imageView.setImageDrawable(AppCompatResources.getDrawable(context, imageDrawable))
            if (it != numberOfIndicators - 1) {
                val layoutParams = LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
                layoutParams.marginEnd = context.resources.getDimensionPixelSize(R.dimen.margin_m)
                imageView.layoutParams = layoutParams
            }
            addView(imageView)
        }
    }
}
