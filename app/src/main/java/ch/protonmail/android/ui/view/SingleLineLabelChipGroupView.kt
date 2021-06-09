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
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StyleRes
import ch.protonmail.android.R
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.ui.layout.MoreItemsLinearLayout
import ch.protonmail.android.utils.extensions.isInPreviewMode

/**
 * Displays, on a single line, the labels that can fit, plus "+N" for the missing labels.
 * The last labels, could be truncated in order to fill the line, but only if 3+ characters can be displayed.
 *
 * This View is supposed to be "static", in a way that it won't change form, for cases where the View can be expanded,
 *  like for message details, an ExpandableLabelChipGroupView ( not implemented yet ) must be used instead.
 *
 * Due to time limitations, the current behaviour is to show only one or two labels ( full or truncated ), plus the
 *  "+N" if there are more labels
 */
class SingleLineLabelChipGroupView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : MoreItemsLinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    private var allLabels = emptyList<LabelChipUiModel>()

    override val minTextViewWidth = resources.getDimensionPixelSize(R.dimen.min_width_label)

    init {
        orientation = HORIZONTAL

        if (isInPreviewMode()) {
            val previewLabels = listOf(
                LabelChipUiModel(Id("1"), Name("first very long label"), Color.BLUE),
                LabelChipUiModel(Id("2"), Name("second very long label"), Color.GREEN),
                LabelChipUiModel(Id("3"), Name("third very long label"), Color.RED),
                LabelChipUiModel(Id("4"), Name("forth very long label"), Color.MAGENTA),
                LabelChipUiModel(Id("5"), Name("fifth very long label"), Color.BLACK),
            )
            setLabels(previewLabels)
        }
    }

    fun setLabels(labels: List<LabelChipUiModel>) {
        if (labels.isNotEmpty() && labels == allLabels) return
        allLabels = labels

        removeAllViews()
        labels.map(::buildLabelChipView).forEach(::addView)
    }

    private fun buildLabelChipView(label: LabelChipUiModel): View {
        val chipView = LabelChipView(context).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginEnd = resources.getDimensionPixelSize(R.dimen.padding_s)
            }
            setLabel(label)
        }
        // Wrap in Frame Layout for avoid margin's problems on MoreItemsLinearLayout
        return FrameLayout(context).apply {
            addView(chipView)
        }
    }
}
