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

package ch.protonmail.android.mailbox.presentation.ui

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import androidx.appcompat.widget.LinearLayoutCompat
import ch.protonmail.android.databinding.LayoutMailboxNoMessagesBinding
import ch.protonmail.android.mailbox.presentation.model.EmptyMailboxUiModel

class EmptyMailboxView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {

    private val titleTextView: TextView
    private val subtitleTextView: TextView

    init {
        val binding = LayoutMailboxNoMessagesBinding.inflate(LayoutInflater.from(context), this)

        titleTextView = binding.layoutNoMessagesTitleTextView
        subtitleTextView = binding.layoutNoMessagesSubtitleTextView

        (binding.root as LinearLayoutCompat).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                .apply { setGravity(Gravity.CENTER) }
            orientation = VERTICAL
        }
    }

    fun bind(uiModel: EmptyMailboxUiModel) {
        titleTextView.setCompoundDrawablesWithIntrinsicBounds(0, uiModel.imageRes, 0, 0)
        titleTextView.setText(uiModel.titleRes)
        subtitleTextView.setText(uiModel.subtitleRes)
    }
}
