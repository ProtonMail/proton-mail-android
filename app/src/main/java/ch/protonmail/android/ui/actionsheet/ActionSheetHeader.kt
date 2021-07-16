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

package ch.protonmail.android.ui.actionsheet

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.annotation.StyleRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.databinding.ViewActionSheetHeaderBinding

/**
 * Header part of action sheets used in the app.
 */
class ActionSheetHeader @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var binding = ViewActionSheetHeaderBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    fun setTitle(title: String) {
        binding.textviewActionSheetTitle.text = title
    }

    fun setSubTitle(subTitle: String) {
        binding.textviewActionSheetSubtitle.apply {
            text = subTitle
            isVisible = true
        }
    }

    fun shiftTitleToRightBy(shiftValue: Float) {
        binding.textviewActionSheetTitle.translationX = shiftValue

        with(binding.textviewActionSheetSubtitle) {
            if (isVisible) {
                translationX = shiftValue
            }
        }
    }

    fun setCloseIconVisibility(shouldBeVisible: Boolean) {
        binding.viewActionSheetClose.isVisible = shouldBeVisible
    }

    fun setOnCloseClickListener(listener: OnClickListener) =
        binding.viewActionSheetClose.setOnClickListener(listener)

    fun setRightActionClickListener(listener: OnClickListener) =
        binding.textviewActionsSheetRightAction.apply {
            isVisible = true
            setOnClickListener(listener)
        }

}
