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
package ch.protonmail.android.views.messagesList

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import ch.protonmail.android.R
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.item_label_marginless_small.view.*

class ItemLabelMarginlessSmallView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(
    context,
    attrs,
    defStyleAttr
) {

    init {
        inflate(context, R.layout.item_label_marginless_small, this)
        layoutParams = ChipGroup.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
    }

    fun bind(text: CharSequence, @ColorInt color: Int, strokeWidth: Int) {
        nameTextView.text = text
        nameTextView.setTextColor(color)
        val gd = nameTextView.background.current as GradientDrawable
        gd.setStroke(strokeWidth, color)
    }
}
