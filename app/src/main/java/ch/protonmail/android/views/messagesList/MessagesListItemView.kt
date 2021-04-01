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
import android.graphics.Color
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.mailbox.presentation.MailboxUiItem
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.UiUtil
import kotlinx.android.synthetic.main.list_item_mailbox.view.*
import me.proton.core.presentation.utils.inflate
import me.proton.core.util.kotlin.EMPTY_STRING

private const val MAX_LABELS_WITH_TEXT = 1

/**
 * A view that represents one item in the mailbox list when conversation mode is turned off.
 */
class MessagesListItemView constructor(
    context: Context
) : ConstraintLayout(context) {

    init {
        inflate(R.layout.list_item_mailbox, true)
        layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private var allFolders = HashMap<String, Label>()

    private val strokeWidth by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            1f,
            context
                .resources.displayMetrics
        ).toInt()
    }

    private fun setIconsTint(isRead: Boolean) {
        val iconTint = if (isRead) context.getColor(R.color.icon_weak) else context.getColor(R.color.icon_norm)

        replyImageView.setColorFilter(iconTint)
        replyAllImageView.setColorFilter(iconTint)
        forwardImageView.setColorFilter(iconTint)

        firstLocationImageView.setColorFilter(iconTint)
        secondLocationImageView.setColorFilter(iconTint)
        thirdLocationImageView.setColorFilter(iconTint)
    }

    private fun setTextViewStyles(isRead: Boolean) {
        if (isRead) {
            senderTextView.setTextAppearance(R.style.Proton_Text_Default)
            subjectTextView.setTextAppearance(R.style.Proton_Text_DefaultSmall_Weak)
            timeDateTextView.setTextAppearance(R.style.Proton_Text_Caption_Weak)
        } else {
            senderTextView.setTextAppearance(R.style.Proton_Text_Default_Bold)
            subjectTextView.setTextAppearance(R.style.Proton_Text_DefaultSmall_Medium)
            timeDateTextView.setTextAppearance(R.style.Proton_Text_Caption_Strong)
        }
    }

    private fun getSenderText(messageLocation: Constants.MessageLocationType, message: Message) =
        if (isDraft(mailboxUiItem)) {
            mailboxUiItem.recipients
        } else {
            mailboxUiItem.senderName
        }
    }

    private fun getIconForMessageLocation(messageLocation: Constants.MessageLocationType) = when (messageLocation) {
        Constants.MessageLocationType.INBOX -> R.drawable.ic_inbox
        Constants.MessageLocationType.SENT -> R.drawable.ic_send
        Constants.MessageLocationType.DRAFT -> R.drawable.ic_draft
        Constants.MessageLocationType.ALL_DRAFT -> R.drawable.ic_draft
        Constants.MessageLocationType.ALL_SENT -> R.drawable.ic_send
        Constants.MessageLocationType.ARCHIVE -> R.drawable.ic_archive
        Constants.MessageLocationType.TRASH -> R.drawable.ic_trash
        else -> null
    }

    fun bind(
        mailboxUiItem: MailboxUiItem,
        labels: List<Label>,
        isMultiSelectionMode: Boolean,
        mailboxLocation: Constants.MessageLocationType,
        isBeingSent: Boolean,
        isAttachmentsBeingUploaded: Boolean
    ) {
        val readStatus = mailboxUiItem.isRead
        val messageLocation = Constants.MessageLocationType.fromInt(mailboxUiItem.messageData?.location)

        setTextViewStyles(readStatus)
        setIconsTint(readStatus)

        val senderText = getSenderText(messageLocation, message)
        // Sender text can only be empty in drafts where we show recipients instead of senders
        senderTextView.text =
            if (senderText.isNullOrEmpty()) context.getString(R.string.empty_recipients) else senderText
        senderInitialView.bind(senderText ?: EMPTY_STRING, isMultiSelectionMode)

        subjectTextView.text = message.subject

        timeDateTextView.text = DateUtil.formatDateTime(context, message.timeMs)

        replyImageView.visibility =
            if (message.isReplied == true && message.isRepliedAll != true) View.VISIBLE else View.GONE
        replyAllImageView.visibility = if (message.isRepliedAll == true) View.VISIBLE else View.GONE
        forwardImageView.visibility = if (message.isForwarded == true) View.VISIBLE else View.GONE

        draftImageView.visibility = if (mailboxLocation in arrayOf(
                Constants.MessageLocationType.DRAFT,
                Constants.MessageLocationType.ALL_DRAFT
            )
        ) View.VISIBLE else View.GONE

        // TODO: Currently there's a bug with showing the location on certain messages.
        //  Revisit the logic with MAILAND-1422
        if (mailboxLocation in arrayOf(
                Constants.MessageLocationType.ALL_MAIL,
                Constants.MessageLocationType.STARRED,
                Constants.MessageLocationType.LABEL,
                Constants.MessageLocationType.SEARCH
            )
        ) {
            val icon = getIconForMessageLocation(messageLocation)
            if (icon != null) {
                firstLocationImageView.visibility = View.VISIBLE
                firstLocationImageView.setImageDrawable(context.getDrawable(icon))
            }
        }

        val hasAttachments = message.Attachments.isNotEmpty() || message.numAttachments >= 1
        attachmentImageView.visibility = if (hasAttachments) View.VISIBLE else View.GONE

        starImageView.visibility = if (message.isStarred == true) View.VISIBLE else View.GONE

        emptySpaceView.visibility = if (
            attachmentImageView.visibility == View.VISIBLE ||
            starImageView.visibility == View.VISIBLE
        ) View.VISIBLE else View.GONE

        expirationImageView.visibility = if (message.expirationTime > 0) View.VISIBLE else View.GONE

        showLabels(labels)
    }

    private fun showLabels(labels: List<Label>) {
        // TODO: This is the old labels logic and it should be changed with MAILAND-1502
        labelsLayout.removeAllViews()
        labelsLayout.visibility = View.GONE
        labels.forEach { allFolders[it.id] = it }
        val nonExclusiveLabels = labels.filter { !it.exclusive }

        var commonHeight = 20
        nonExclusiveLabels.forEachIndexed { i, (_, name, colorString) ->
            val labelItemView = ItemLabelMarginlessSmallView(context)
            val color = if (colorString.isNotEmpty()) {
                val normalizedColor = UiUtil.normalizeColor(colorString)
                Color.parseColor(normalizedColor)
            } else 0

            if (i < MAX_LABELS_WITH_TEXT) {
                labelItemView.bind(name, color, strokeWidth)
                labelsLayout.visibility = View.VISIBLE
                labelsLayout.addView(labelItemView)
                labelItemView.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED)
                commonHeight = labelItemView.measuredHeight
            } else {
                val imageView = ImageView(context)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.setMargins(0, 0, 0, 0)
                imageView.layoutParams = lp
                imageView.setImageResource(R.drawable.mail_label_collapsed)
                imageView.setColorFilter(color)
                imageView.layoutParams.height = commonHeight
                labelsLayout.visibility = View.VISIBLE
                labelsLayout.addView(imageView)
            }
            labelItemView.requestLayout()
        }
    }
}
