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
import android.widget.LinearLayout
import androidx.annotation.ColorInt
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.withStyledAttributes
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.utils.extensions.fromAttributesOrPreviewOrNull

class CollapsedMessageLabelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatImageView(context, attrs, defStyleAttr) {

    var labelId: LabelId? = null
        private set

    @get:ColorInt
    private val defaultBackgroundTint: Int get() =
        context.getColor(R.color.shade_60)

    init {
        val diameter = resources.getDimensionPixelSize(R.dimen.diameter_collapsed_label)
        layoutParams = LinearLayout.LayoutParams(diameter, diameter).apply {
            marginEnd = resources.getDimensionPixelSize(R.dimen.padding_xs)
        }

        context.withStyledAttributes(attrs, R.styleable.LabelChipView, defStyleAttr) {
            val labelColor = fromAttributesOrPreviewOrNull(
                fromAttributes = getColor(R.styleable.LabelChipView_labelColor, -1).takeIf { it != -1 },
                forPreview = Color.BLUE
            )

            setLabelColor(labelColor ?: INITIAL_BACKGROUND_COLOR)
        }
    }

    fun setLabel(label: LabelChipUiModel) {
        labelId = label.id
        setLabelColor(label.color)
    }

    private fun setLabelColor(@ColorInt color: Int?) {
        setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.bg_collapsed_label))
        setColorFilter(color ?: defaultBackgroundTint)
    }

    private companion object {

        const val INITIAL_BACKGROUND_COLOR = Color.TRANSPARENT
    }
}
