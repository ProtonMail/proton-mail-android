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
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.withStyledAttributes
import ch.protonmail.android.R
import ch.protonmail.android.databinding.ActionsheetAddAttachmentsItemBinding
import ch.protonmail.android.utils.extensions.fromAttributesOrPreviewOrThrow
import ch.protonmail.android.utils.extensions.getDrawableOrThrow

/**
 * Item for [AddAttachmentsActionSheet]
 */
class AddAttachmentsActionSheetItem @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr, defStyleRes) {

    private val textView: TextView
    private val imageView: ImageView

    init {
        val binding = ActionsheetAddAttachmentsItemBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        textView = binding.addAttachmentsItemTextView
        imageView = binding.addAttachmentsItemImageView

        context.withStyledAttributes(attrs, R.styleable.AddAttachmentsActionSheetItem, defStyleAttr, defStyleRes) {
            val text = fromAttributesOrPreviewOrThrow(
                fromAttributes = getString(R.styleable.AddAttachmentsActionSheetItem_itemText),
                forPreview = context.getText(R.string.actionsheet_add_attachments_gallery)
            )
            val icon = fromAttributesOrPreviewOrThrow(
                getDrawable(R.styleable.AddAttachmentsActionSheetItem_itemIcon),
                context.getDrawableOrThrow(R.drawable.ic_photo)
            )
            setText(text)
            setIcon(icon)
            setIconContentDescription(getString(R.styleable.AddAttachmentsActionSheetItem_itemIconDescription))
        }
    }

    private fun setText(charSequence: CharSequence) {
        textView.text = charSequence
    }

    private fun setIcon(drawable: Drawable) {
        imageView.setImageDrawable(drawable)
    }

    private fun setIconContentDescription(description: CharSequence?) {
        imageView.contentDescription = description
    }
}
