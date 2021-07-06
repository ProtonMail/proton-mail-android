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

package ch.protonmail.android.details.presentation

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.databinding.MessageDetailsActionsBinding

class MessageDetailsActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val showHistoryButton: Button
    private val replyButton: ImageButton
    private val actionsSheetButton: ImageButton

    init {
        val binding = MessageDetailsActionsBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        showHistoryButton = binding.detailsButtonShowHistory
        replyButton = binding.detailsButtonReply
        actionsSheetButton = binding.detailsActionSheetButton
    }

    fun bind(uiModel: UiModel) {
        replyButton.setImageDrawable(ContextCompat.getDrawable(context, uiModel.replyMode.drawableId))
    }

    fun displayShowHistoryButton(isVisible: Boolean) {
        showHistoryButton.isVisible = isVisible
    }

    fun onReplyClicked(callback: (View) -> Unit) {
        replyButton.setOnClickListener { callback(it) }
    }

    fun onShowHistoryClicked(callback: (View) -> Unit) {
        showHistoryButton.setOnClickListener { callback(it) }
    }

    class UiModel(
        val replyMode: ReplyMode
    )

    enum class ReplyMode(@DrawableRes val drawableId: Int) {
        REPLY(R.drawable.reply),
        REPLY_ALL(R.drawable.reply_all),
    }
}