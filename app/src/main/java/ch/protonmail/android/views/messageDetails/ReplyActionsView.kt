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

package ch.protonmail.android.views.messageDetails

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import ch.protonmail.android.R
import ch.protonmail.android.databinding.LayoutMessageDetailsReplyActionsBinding

/**
 * A view containing reply, reply all and forward actions
 */
class ReplyActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val replyButton: FrameLayout
    private val replyAllButton: FrameLayout
    private val forwardButton: FrameLayout

    init {
        setPadding(context.resources.getDimensionPixelSize(R.dimen.padding_l))
        clipToPadding = false

        val binding = LayoutMessageDetailsReplyActionsBinding.inflate(
            LayoutInflater.from(context),
            this
        )

        replyButton = binding.replyButton
        replyAllButton = binding.replyAllButton
        forwardButton = binding.forwardButton
    }

    fun bind(shouldShowReplyAllAction: Boolean, shouldHideAllActions: Boolean) {
        replyAllButton.isVisible = shouldShowReplyAllAction
        this.isVisible = !shouldHideAllActions
    }

    fun onReplyActionClicked(callback: (View) -> Unit) {
        replyButton.setOnClickListener { callback(it) }
    }

    fun onReplyAllActionClicked(callback: (View) -> Unit) {
        replyAllButton.setOnClickListener { callback(it) }
    }

    fun onForwardActionClicked(callback: (View) -> Unit) {
        forwardButton.setOnClickListener { callback(it) }
    }
}
