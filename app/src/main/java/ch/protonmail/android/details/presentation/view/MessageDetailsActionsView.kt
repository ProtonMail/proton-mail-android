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

package ch.protonmail.android.details.presentation.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import ch.protonmail.android.R
import ch.protonmail.android.databinding.MessageDetailsActionsBinding

class MessageDetailsActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val showHistoryButton: Button
    private val moreActionsButton: ImageButton

    init {
        setPadding(context.resources.getDimensionPixelSize(R.dimen.padding_m))

        val binding = MessageDetailsActionsBinding.inflate(
            LayoutInflater.from(context),
            this
        )
        showHistoryButton = binding.detailsButtonShowHistory
        moreActionsButton = binding.detailsButtonMoreActions
    }

    fun bind(uiModel: UiModel) {
        showHistoryButton.isVisible = !uiModel.hideShowHistory
        moreActionsButton.isVisible = !uiModel.hideMoreActions
        this.isVisible = showHistoryButton.isVisible || moreActionsButton.isVisible
    }

    fun onShowHistoryClicked(callback: (View) -> Unit) {
        showHistoryButton.setOnClickListener { callback(it) }
    }

    fun onMoreActionsClicked(callback: (View) -> Unit) {
        moreActionsButton.setOnClickListener { callback(it) }
    }

    data class UiModel(
        val hideShowHistory: Boolean,
        val hideMoreActions: Boolean
    )
}
