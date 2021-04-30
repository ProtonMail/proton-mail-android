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

package ch.protonmail.android.ui.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import ch.protonmail.android.databinding.LayoutSingleLineLabelChipGroupBinding

/**
 * Displays, on a single line, the labels that can fit, plus "+N" for the missing labels.
 * The last labels, could be truncated in order to fill the line, but only if 3+ characters can be displayed.
 *
 * This View is supposed to be "static", in a way that it won't change form, for cases where the View can be expanded,
 *  like for message details, an ExpandableLabelChipGroupView ( not implemented yet ) must be used instead.
 *
 * Due to time limitations, the current behaviour is to show only one label ( full or truncated ), plus the "+N" if
 *  there are more labels
 */
class SingleLineLabelChipGroupView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : FrameLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val labelView: LabelChipView
    private val moreView: TextView

    init {
        val binding = LayoutSingleLineLabelChipGroupBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        labelView = binding.singleLineLabelChipGroupLabel
        moreView = binding.singleLineLabelChipGroupMore
    }

    fun setLabels(labels: List<LabelChipUiModel>) {
        labelView.isVisible = labels.isNotEmpty()
        moreView.isVisible = labels.size >= 2

        labels.firstOrNull()?.let(labelView::setLabel)
        moreView.text = getMoreLabelsText(labels.size)
    }

    private fun getMoreLabelsText(labelsCount: Int): String =
        "${labelsCount - 1}+"
}
