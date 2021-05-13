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
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.databinding.ListItemMailboxBinding
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.ui.view.SingleLineLabelChipGroupView
import ch.protonmail.android.utils.DateUtil
import kotlinx.android.synthetic.main.list_item_mailbox.view.*

class MailboxItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val replyImageView: ImageView
    private val replyAllImageView: ImageView
    private val labelsLayout: SingleLineLabelChipGroupView

    init {
        val binding = ListItemMailboxBinding.inflate(
            LayoutInflater.from(context),
            this,
            true
        )
        replyImageView = binding.replyImageView
        replyAllImageView = binding.replyAllImageView
        labelsLayout = binding.mailboxLabelChipGroup

        layoutParams = RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun setIconsTint(isRead: Boolean) {
        val iconTint = if (isRead) context.getColor(R.color.icon_weak) else context.getColor(R.color.icon_norm)

        replyImageView.setColorFilter(iconTint)
        replyAllImageView.setColorFilter(iconTint)
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

        replyImageView.isVisible = mailboxUiItem.messageData?.isReplied == true &&
            !mailboxUiItem.messageData.isRepliedAll
        replyAllImageView.isVisible = mailboxUiItem.messageData?.isRepliedAll == true
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

        labelsLayout.setLabels(mailboxUiItem.labels)
    }

}
