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

package ch.protonmail.android.views.messagesList

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.databinding.LayoutSenderInitialBinding
import me.proton.core.util.kotlin.EMPTY_STRING
import java.util.Locale

/**
 * A view for the selectable sender initial(s).
 */
class SenderInitialView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val senderInitialTextView: TextView
    private val checkImageView: ImageView

    init {
        val binding = LayoutSenderInitialBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )

        senderInitialTextView = binding.senderInitialTextView
        checkImageView = binding.checkImageView
    }

    fun bind(
        senderText: String,
        showDraftIcon: Boolean,
        isMultiSelectionMode: Boolean = false,
        @ColorInt customBackgroundColor: Int? = null
    ) {
        senderInitialTextView.text = if (senderText.isNotEmpty()) {
            senderText.capitalize(Locale.getDefault())
        } else EMPTY_STRING

        senderInitialTextView.isVisible = !isMultiSelectionMode
        checkImageView.isVisible = isMultiSelectionMode

        customBackgroundColor?.let {
            senderInitialTextView.setBackgroundColor(it)
        }

        if (showDraftIcon) {
            senderInitialTextView.text = EMPTY_STRING
            senderInitialTextView.background = context.getDrawable(R.drawable.ic_proton_pen)
        } else {
            senderInitialTextView.background = context.getDrawable(R.drawable.background_sender_initial)
        }
    }
}
