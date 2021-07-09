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

package ch.protonmail.android.details.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.databinding.LayoutMessageDetailsIconsBinding

class MessageDetailsHeaderIcons @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val attachmentsImageView: ImageView
    private val starredToggleButton: ImageView

    init {
        val binding = LayoutMessageDetailsIconsBinding.inflate(
            LayoutInflater.from(context),
            this
        )

        attachmentsImageView = binding.attachmentsImageView
        starredToggleButton = binding.starredToggleButton
    }

    fun bind(message: Message) {
        attachmentsImageView.isVisible = message.numAttachments > 0
        starredToggleButton.isVisible = message.isStarred ?: false
    }
}