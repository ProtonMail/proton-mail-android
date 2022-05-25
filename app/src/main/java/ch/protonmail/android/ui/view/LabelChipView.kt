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
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.utils.extensions.fromAttributesOrPreviewOrNull
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import me.proton.core.util.kotlin.takeIfNotBlank

/**
 * View for a single label - see MailboxActivity or MessageDetailsActivity
 * Inherit from [AppCompatTextView]
 */
class LabelChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var labelId: LabelId? = null

    @get:ColorInt
    private val defaultBackgroundTint: Int get() =
        context.getColor(R.color.shade_60)

    init {
        // Padding
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.padding_m)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.padding_xs)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        // Text
        setTextAppearance(R.style.Proton_Text_Overline_Strong)
        setTextColor(context.getColor(R.color.text_inverted))
        ellipsize = TextUtils.TruncateAt.END
        maxLines = 1

        context.withStyledAttributes(attrs, R.styleable.LabelChipView, defStyleAttr) {
            val labelName = fromAttributesOrPreviewOrNull(
                fromAttributes = text?.toString()?.takeIfNotBlank(),
                forPreview = "label"
            )
            val labelColor = fromAttributesOrPreviewOrNull(
                fromAttributes = getColor(R.styleable.LabelChipView_labelColor, -1).takeIf { it != -1 },
                forPreview = Color.BLUE
            )

            labelName?.let(::setText)
            setLabelColor(labelColor ?: INITIAL_BACKGROUND_COLOR)
        }
    }

    fun setLabel(label: LabelChipUiModel) {
        labelId = label.id
        text = label.name.s
        setLabelColor(label.color)
    }

    fun setLabelColor(color: ColorStateList) {
        background = MaterialShapeDrawable(buildBackgroundShape())
            .apply { fillColor = color }
    }

    fun setLabelColor(@ColorInt color: Int?) {
        setLabelColor(ColorStateList.valueOf(color ?: defaultBackgroundTint))
    }

    private fun buildBackgroundShape() = ShapeAppearanceModel
        .builder()
        .setAllCorners(RoundedCornerTreatment())
        .setAllCornerSizes(999f)
        .build()

    private companion object {

        const val INITIAL_BACKGROUND_COLOR = Color.TRANSPARENT
    }
}

