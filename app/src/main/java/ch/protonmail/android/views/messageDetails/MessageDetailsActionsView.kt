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
import androidx.constraintlayout.widget.ConstraintLayout
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import kotlinx.android.synthetic.main.layout_message_details_actions.view.*
import me.proton.core.presentation.utils.inflate

/**
 * A bottom action view in message details.
 */
class MessageDetailsActionsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(R.layout.layout_message_details_actions, true)
    }

    private fun hasMoreThanOneRecipient(message: Message) = message.toList.size + message.ccList.size > 1

    fun bind(message: Message) {
        if (hasMoreThanOneRecipient(message)) {
            replyActionImageView.setImageDrawable(ContextCompat.getDrawable(context, R.drawable.ic_reply_all))
        }
    }

    fun setOnReplyActionClickListener(
        message: Message,
        onReplyActionClickListener: ((Constants.MessageActionType) -> Unit)
    ) {
        replyActionImageView.setOnClickListener {
            if (hasMoreThanOneRecipient(message)) {
                onReplyActionClickListener(Constants.MessageActionType.REPLY_ALL)
            } else {
                onReplyActionClickListener(Constants.MessageActionType.REPLY)
            }
        }
    }

    fun setOnMarkUnreadActionClickListener(onMarkUnreadActionClickListener: () -> Unit) {
        markUnreadActionImageView.setOnClickListener { onMarkUnreadActionClickListener() }
    }

    fun setOnTrashActionClickListener(onTrashActionClickListener: () -> Unit) {
        trashActionImageView.setOnClickListener { onTrashActionClickListener() }
    }

    fun setOnMoreActionClickListener(onMoreActionClickListener: () -> Unit) {
        moreActionImageView.setOnClickListener { onMoreActionClickListener() }
    }
}
