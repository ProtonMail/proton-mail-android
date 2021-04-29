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
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import ch.protonmail.android.R
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.RecipientType
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.ui.locks.SenderLockIcon
import kotlinx.android.synthetic.main.layout_message_details_header.view.*
import kotlinx.android.synthetic.main.layout_message_details_header.view.senderInitialTextView
import kotlinx.android.synthetic.main.layout_message_details_header.view.senderTextView
import kotlinx.android.synthetic.main.layout_message_details_header.view.timeDateTextView
import me.proton.core.presentation.utils.inflate

/**
 * A view for the collapsible header in message details
 */
class MessageDetailsHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        inflate(R.layout.layout_message_details_header, true)
        val typefacePgp = Typeface.createFromAsset(context.assets, "pgp-icons-android.ttf")
        lockIconTextView.typeface = typefacePgp
    }

    private fun setTextViewStyles() {
        senderTextView.setTextAppearance(R.style.Text_Default)
        timeDateTextView.setTextAppearance(R.style.Text_Caption_Weak)
        toTextView.setTextAppearance(R.style.Text_DefaultSmall)
        recipientsTextView.setTextAppearance(R.style.Text_DefaultSmall_Weak)
    }

    // TODO: Showing the location is buggy, review together with MAILAND-1422
    private fun getIconForMessageLocation(messageLocation: Constants.MessageLocationType) = when (messageLocation) {
        Constants.MessageLocationType.INBOX -> R.drawable.ic_inbox
        Constants.MessageLocationType.SENT -> R.drawable.ic_send
        Constants.MessageLocationType.DRAFT -> R.drawable.ic_draft
        Constants.MessageLocationType.ALL_DRAFT -> R.drawable.ic_draft
        Constants.MessageLocationType.ALL_SENT -> R.drawable.ic_send
        Constants.MessageLocationType.ARCHIVE -> R.drawable.ic_archive
        Constants.MessageLocationType.TRASH -> R.drawable.ic_trash
        Constants.MessageLocationType.SPAM -> R.drawable.ic_spam
        Constants.MessageLocationType.LABEL_FOLDER -> R.drawable.ic_folder
        else -> null
    }

    fun bind(message: Message) {
        setTextViewStyles()

        val senderText = message.senderDisplayName ?: message.senderName
        senderInitialTextView.text =
            if (senderText.isNullOrEmpty()) "D" else senderText.capitalize().subSequence(0, 1)
        senderTextView.text = senderText

        val senderLockIcon = SenderLockIcon(message, message.hasValidSignature, message.hasInvalidSignature)
        lockIconTextView.text = context.getText(senderLockIcon.icon)
        lockIconTextView.setTextColor(senderLockIcon.color)

        val locationIcon = getIconForMessageLocation(Constants.MessageLocationType.fromInt(message.location))
        if (locationIcon != null) {
            locationImageView.setImageDrawable(ContextCompat.getDrawable(context, locationIcon))
        }

        timeDateTextView.text = DateUtil.formatDateTime(context, message.timeMs)

        val toList = message.getList(RecipientType.TO)
        val ccList = message.getList(RecipientType.CC)
        recipientsTextView.text = toList.truncateToList(ccList)
    }

    // Old logic for getting recipients for collapsed view copied from MessageDetailsRecipientView
    private fun List<MessageRecipient>.truncateToList(ccList: List<MessageRecipient>): String {
        val ccListSize = ccList.size
        var listForShowing = this

        if (isEmpty()) {
            listForShowing = ccList
            if (ccList.isEmpty()) {
                return context.getString(R.string.undisclosed_recipients)
            }
        }

        var firstName: String? = listForShowing[0].name
        if (firstName.isNullOrEmpty()) {
            firstName = listForShowing[0].emailAddress
        }

        if (firstName!!.length > 40) {
            firstName = firstName.substring(0, 40) + "..."
        }

        val extraNamesCount = if (!isEmpty()) {
            listForShowing.size - 1 + ccListSize
        } else {
            listForShowing.size - 1
        }
        return if (extraNamesCount == 0) firstName else "$firstName, +$extraNamesCount "
    }
}
