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

package ch.protonmail.android.ui.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.StyleRes
import androidx.core.view.isVisible
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.ui.layout.MoreItemsLinearLayout
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.utils.extensions.isInPreviewMode

/**
 * Displays, on a single line, up to 3 collapsed labels, followed by "+N" label in case there are more labels.
 */
class SingleLineCollapsedLabelGroupView @JvmOverloads constructor (
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @StyleRes defStyleRes: Int = 0
) : MoreItemsLinearLayout(context, attrs, defStyleAttr, defStyleRes) {

    override val maxVisibleChildrenCount: Int = 3

    private var allLabels = emptyList<LabelChipUiModel>()

    init {
        orientation = HORIZONTAL

        if (isInPreviewMode()) {
            val previewLabels = listOf(
                LabelChipUiModel(LabelId("1"), Name("first very long label"), Color.BLUE),
                LabelChipUiModel(LabelId("2"), Name("second very long label"), Color.GREEN),
                LabelChipUiModel(LabelId("3"), Name("third very long label"), Color.RED),
                LabelChipUiModel(LabelId("4"), Name("forth very long label"), Color.MAGENTA),
                LabelChipUiModel(LabelId("5"), Name("fifth very long label"), Color.BLACK),
            )
            setLabels(previewLabels)
        }
    }

    fun setLabels(labels: List<LabelChipUiModel>) {
        isVisible = labels.isNotEmpty()
        if (labels.isNotEmpty() && labels == allLabels) return
        allLabels = labels

        removeAllViewsInLayout()
        labels.map(::buildLabelView).forEach(::addViewInLayout)
        requestLayout()
    }

    private fun buildLabelView(label: LabelChipUiModel): View {
        val view = CollapsedMessageLabelView(context).apply {
            setLabel(label)
        }

        return FrameLayout(context).apply {
            addView(view)
        }
    }
}
