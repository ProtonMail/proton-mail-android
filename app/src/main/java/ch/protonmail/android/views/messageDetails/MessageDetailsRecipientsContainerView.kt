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
import android.graphics.text.LineBreaker
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.details.RecipientContextMenuFactory
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import me.proton.core.presentation.utils.inflate

private const val MARGIN_TOP_RECIPIENT_TEXT_VIEW = 8

/**
 * A container view for the recipients in message details
 */
class MessageDetailsRecipientsContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(R.layout.layout_message_details_recipients_container, true)
    }

    private var recipientViewIds: HashMap<Int, Int> = hashMapOf()

    fun bind(recipients: List<MessageRecipient>) {
        removeOldViews()
        if (recipients.isEmpty()) {
            val recipientTextView = createRecipientTextView()
            recipientTextView.text = context.getString(R.string.undisclosed_recipients)
            addView(recipientTextView)

            val constraintSet = createConstraintSet(recipientTextView, 0)
            constraintSet.applyTo(this)
            return
        }

        recipients.forEachIndexed { index, messageRecipient ->
            val recipientTextView = createRecipientTextView()
            val onRecipientClickListener = getOnRecipientClickListener(messageRecipient)
            recipientTextView.setOnClickListener(onRecipientClickListener)
            recipientTextView.text = getFormattedRecipientString(messageRecipient)
            recipientViewIds[index] = recipientTextView.id
            addView(recipientTextView)

            val constraintSet = createConstraintSet(recipientTextView, index)
            constraintSet.applyTo(this)
        }
    }

    private fun removeOldViews() {
        removeAllViews()
        recipientViewIds.clear()
    }

    private fun createRecipientTextView(): TextView {
        val recipientTextView = TextView(
            context,
            null,
            R.style.Proton_Text_DefaultSmall_Interaction,
            R.style.Proton_Text_DefaultSmall_Interaction
        )
        recipientTextView.layoutParams = LayoutParams(LayoutParams.MATCH_CONSTRAINT, LayoutParams.WRAP_CONTENT)
        recipientTextView.breakStrategy = LineBreaker.BREAK_STRATEGY_SIMPLE
        recipientTextView.id = View.generateViewId()
        return recipientTextView
    }

    private fun createConstraintSet(recipientTextView: TextView, index: Int): ConstraintSet {
        val constraintSet = ConstraintSet()
        constraintSet.clone(this)
        constraintSet.connect(recipientTextView.id, ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT)
        constraintSet.connect(recipientTextView.id, ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT)
        constraintSet.connect(
            recipientTextView.id,
            ConstraintSet.TOP,
            if (index == 0) ConstraintSet.PARENT_ID else recipientViewIds[index - 1]!!,
            if (index == 0) ConstraintSet.TOP else ConstraintSet.BOTTOM,
            if (index == 0) 0 else MARGIN_TOP_RECIPIENT_TEXT_VIEW
        )
        return constraintSet
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
