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

package ch.protonmail.android.views.messageDetails

import android.content.Context
import android.graphics.text.LineBreaker
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.details.RecipientContextMenuFactory
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity
import me.proton.core.presentation.utils.inflate

private const val MARGIN_TOP_RECIPIENT_TEXT_VIEW = 8

/**
 * A container view for the recipients in message details
 */
class MessageDetailsRecipientsContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    init {
        orientation = VERTICAL
        inflate(R.layout.layout_message_details_recipients_container, true)
    }

    fun bind(recipients: List<MessageRecipient>) {
        removeAllViews()
        if (recipients.isEmpty()) {
            val recipientTextView = createRecipientTextView(shouldIncludeTopMargin = false)
            recipientTextView.text = context.getString(R.string.undisclosed_recipients)
            addView(recipientTextView)
            return
        }

        recipients.forEachIndexed { index, messageRecipient ->
            val recipientTextView = createRecipientTextView(shouldIncludeTopMargin = index != 0)
            val onRecipientClickListener = getOnRecipientClickListener(messageRecipient)
            recipientTextView.setOnClickListener(onRecipientClickListener)
            recipientTextView.text = getFormattedRecipientString(messageRecipient)
            addView(recipientTextView)
        }
    }

    private fun createRecipientTextView(shouldIncludeTopMargin: Boolean): TextView {
        val recipientTextView = TextView(
            context,
            null,
            R.style.Proton_Text_DefaultSmall_Interaction,
            R.style.Proton_Text_DefaultSmall_Interaction
        )
        recipientTextView.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
            if (shouldIncludeTopMargin) setMargins(0, MARGIN_TOP_RECIPIENT_TEXT_VIEW, 0, 0)
        }
        recipientTextView.breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
        recipientTextView.id = View.generateViewId()
        return recipientTextView
    }

    private fun getOnRecipientClickListener(messageRecipient: MessageRecipient): OnClickListener {
        val recipientContextMenuFactory = RecipientContextMenuFactory(context as MessageDetailsActivity)
        return recipientContextMenuFactory.invoke(messageRecipient.emailAddress)
    }

    private fun getFormattedRecipientString(messageRecipient: MessageRecipient): String {
        return if (
            messageRecipient.name.isEmpty() ||
            messageRecipient.name.equals(messageRecipient.emailAddress)
        ) {
            context.getString(R.string.recipient_email_format, messageRecipient.emailAddress)
        } else {
            context.getString(
                R.string.recipient_name_email_format,
                messageRecipient.name,
                messageRecipient.emailAddress
            )
        }
    }
}
