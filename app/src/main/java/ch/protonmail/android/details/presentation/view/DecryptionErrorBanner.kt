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
package ch.protonmail.android.details.presentation.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ch.protonmail.android.R

class DecryptionErrorBanner @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        val backgroundDrawable = ContextCompat.getDrawable(context, R.drawable.message_details_banner_background)
        val padding = resources.getDimensionPixelSize(R.dimen.message_details_banner_margin_horizontal)
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setPadding(padding, padding, padding, padding)
        background = backgroundDrawable

        text = resources.getString(R.string.decryption_of_message_failed)
        setTextColor(ContextCompat.getColor(context, R.color.text_inverted))

        val warningDrawable = ContextCompat.getDrawable(context, R.drawable.ic_exclamation_triangle)
        setCompoundDrawablesRelativeWithIntrinsicBounds(warningDrawable, null, null, null)
        compoundDrawablePadding = resources.getDimensionPixelSize(R.dimen.message_details_banner_drawable_padding)
        compoundDrawableTintList = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.icon_inverted))
    }

    fun bind(showDecryptionError: Boolean) {
        isVisible = showDecryptionError
    }
}
