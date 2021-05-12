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
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.UiUtil
import kotlinx.android.synthetic.main.list_item_mailbox.view.*
import me.proton.core.presentation.utils.inflate

private const val MAX_LABELS_WITH_TEXT = 1

class MailboxItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

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

        reply_image_view.setColorFilter(iconTint)
        reply_all_image_view.setColorFilter(iconTint)
        forward_image_view.setColorFilter(iconTint)

        first_location_image_view.setColorFilter(iconTint)
        second_location_image_view.setColorFilter(iconTint)
        third_location_image_view.setColorFilter(iconTint)
    }

    private fun setTextViewStyles(isRead: Boolean) {
        if (isRead) {
            sender_text_view.setTextAppearance(R.style.Proton_Text_Default)
            subject_text_view.setTextAppearance(R.style.Proton_Text_DefaultSmall_Weak)
            time_date_text_view.setTextAppearance(R.style.Proton_Text_Caption_Weak)
        } else {
            sender_text_view.setTextAppearance(R.style.Proton_Text_Default_Bold)
            subject_text_view.setTextAppearance(R.style.Proton_Text_DefaultSmall_Medium)
            time_date_text_view.setTextAppearance(R.style.Proton_Text_Caption_Strong)
        }
    }

    private fun getSenderText(mailboxUiItem: MailboxUiItem) =
        if (isDraft(mailboxUiItem)) {
            mailboxUiItem.recipients
        } else {
            mailboxUiItem.senderName
        }

    private fun isDraft(mailboxUiItem: MailboxUiItem): Boolean {
        val messageLocation = mailboxUiItem.messageData?.location
            ?: Constants.MessageLocationType.INVALID.messageLocationTypeValue
        return Constants.MessageLocationType.fromInt(messageLocation) in arrayOf(
            Constants.MessageLocationType.DRAFT,
            Constants.MessageLocationType.SENT
        )
    }

    private fun getIconForMessageLocation(messageLocation: Constants.MessageLocationType) = when (messageLocation) {
        Constants.MessageLocationType.INBOX -> R.drawable.ic_inbox
        Constants.MessageLocationType.SENT -> R.drawable.ic_paper_plane
        Constants.MessageLocationType.DRAFT -> R.drawable.ic_pencil
        Constants.MessageLocationType.ALL_DRAFT -> R.drawable.ic_pencil
        Constants.MessageLocationType.ALL_SENT -> R.drawable.ic_paper_plane
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
        areAttachmentsBeingUploaded: Boolean
    ) {
        val readStatus = mailboxUiItem.isRead
        val messageLocation = Constants.MessageLocationType.fromInt(
            mailboxUiItem.messageData?.location
                ?: Constants.MessageLocationType.INVALID.messageLocationTypeValue
        )

        setTextViewStyles(readStatus)
        setIconsTint(readStatus)

        val senderText = getSenderText(mailboxUiItem)
        // Sender text can only be empty in drafts where we show recipients instead of senders
        sender_text_view.text =
            if (senderText.isEmpty()) context.getString(R.string.empty_recipients) else senderText
        sender_initial_view.bind(senderText, isMultiSelectionMode)

        subject_text_view.text = mailboxUiItem.subject

        time_date_text_view.text = DateUtil.formatDateTime(context, mailboxUiItem.lastMessageTimeMs)
        if (areAttachmentsBeingUploaded) {
            time_date_text_view.text = context.getString(R.string.draft_label_attachments_uploading)
        }
        if (isBeingSent) { // overwrite attachment text so there's no flickering between them
            time_date_text_view.text = context.getString(R.string.draft_label_message_uploading)
        }

        reply_image_view.isVisible = mailboxUiItem.messageData?.isReplied == true &&
            !mailboxUiItem.messageData.isRepliedAll
        reply_all_image_view.isVisible = mailboxUiItem.messageData?.isRepliedAll == true
        forward_image_view.isVisible = mailboxUiItem.messageData?.isForwarded == true

        draft_image_view.isVisible = mailboxLocation in arrayOf(
            Constants.MessageLocationType.DRAFT,
            Constants.MessageLocationType.ALL_DRAFT
        )

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
                first_location_image_view.visibility = View.VISIBLE
                first_location_image_view.setImageDrawable(ContextCompat.getDrawable(context, icon))
            }
        }

        messages_number_text_view.isVisible = mailboxUiItem.messagesCount != null
        messages_number_text_view.text = mailboxUiItem.messagesCount.toString()
        sending_uploading_progress_bar.isVisible = isBeingSent || areAttachmentsBeingUploaded
        attachment_image_view.isVisible = mailboxUiItem.hasAttachments
        star_image_view.isVisible = mailboxUiItem.isStarred

        empty_space_view.isVisible = attachment_image_view.isVisible || star_image_view.isVisible

        expiration_image_view.isVisible = mailboxUiItem.expirationTime > 0

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
