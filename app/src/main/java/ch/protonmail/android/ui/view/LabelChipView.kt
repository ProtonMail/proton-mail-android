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
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.TextUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.withStyledAttributes
import androidx.recyclerview.widget.DiffUtil
import ch.protonmail.android.R
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel

/**
 * View for a single label - see MailboxActivity or MessageDetailsActivity
 * Inherit from [AppCompatTextView]
 */
class LabelChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    init {
        // Padding
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.padding_m)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.padding_xs)
        setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        // Text
        // TODO setTextAppearance(TODO("TextAppearance from Core - Overline.Medium"))
        setTextColor(context.getColor(R.color.text_inverted))
        ellipsize = TextUtils.TruncateAt.END
        maxLines = 1

        context.withStyledAttributes(attrs, R.styleable.LabelChipView, defStyleAttr) {
            val labelColor = getColor(R.styleable.LabelChipView_labelColor, INITIAL_BACKGROUND_COLOR)
            setLabelColor(labelColor)
        }
    }

    fun setLabel(label: LabelChipUiModel) {
        text = label.name.s
        setLabelColor(label.color)
    }

    fun setLabelColor(color: ColorStateList) {
        background = MaterialShapeDrawable(BackgroundShape)
            .apply { fillColor = color }
    }

    fun setLabelColor(color: Int) {
        setLabelColor(ColorStateList.valueOf(color))
    }

    companion object {

        val BackgroundShape = ShapeAppearanceModel
            .builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(999f)
            .build()

        private const val INITIAL_BACKGROUND_COLOR = Color.TRANSPARENT
    }
}

data class LabelChipUiModel(
    val id: Id,
    val name: Name,
    val color: Int
) {

    companion object {

        val DiffCallback = object : DiffUtil.ItemCallback<LabelChipUiModel>() {

            override fun areItemsTheSame(oldItem: LabelChipUiModel, newItem: LabelChipUiModel) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: LabelChipUiModel, newItem: LabelChipUiModel) =
                oldItem == newItem

        }
    }

}
