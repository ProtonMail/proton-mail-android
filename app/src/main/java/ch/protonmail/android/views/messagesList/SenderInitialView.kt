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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import ch.protonmail.android.databinding.LayoutSenderInitialBinding
import me.proton.core.util.kotlin.EMPTY_STRING
import java.util.Locale

/**
 * A view for the selectable sender initial
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

    fun bind(senderText: String, isMultiSelectionMode: Boolean = false) {
        senderInitialTextView.text = if (senderText.isNotEmpty()) {
            senderText.capitalize(Locale.getDefault()).subSequence(0, 1)
        } else EMPTY_STRING

        if (isMultiSelectionMode) {
            senderInitialTextView.visibility = View.INVISIBLE
            checkImageView.visibility = View.VISIBLE
        } else {
            checkImageView.visibility = View.INVISIBLE
            senderInitialTextView.visibility = View.VISIBLE
        }
    }
}
