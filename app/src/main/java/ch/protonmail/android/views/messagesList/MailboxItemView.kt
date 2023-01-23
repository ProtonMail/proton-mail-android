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
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.databinding.ListItemMailboxBinding
import ch.protonmail.android.mailbox.presentation.model.MailboxItemUiModel
import ch.protonmail.android.ui.view.SingleLineLabelChipGroupView
import ch.protonmail.android.utils.DateUtil
import kotlinx.android.synthetic.main.list_item_mailbox.view.*
import timber.log.Timber

private const val HYPHEN = "-"

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
            correspondents_text_view.setTextAppearance(R.style.Proton_Text_Default_Weak)
            subject_text_view.setTextAppearance(R.style.Proton_Text_DefaultSmall_Weak)
            time_date_text_view.setTextAppearance(R.style.Proton_Text_Caption_Weak)
        } else {
            correspondents_text_view.setTextAppearance(R.style.Proton_Text_Default_Bold)
            subject_text_view.setTextAppearance(R.style.Proton_Text_DefaultSmall_Medium)
            time_date_text_view.setTextAppearance(R.style.Proton_Text_Caption_Strong)
        }
    }

    private fun getIconForMessageLocation(messageLocation: MessageLocationType) = when (messageLocation) {
        MessageLocationType.INBOX -> R.drawable.ic_proton_inbox
        MessageLocationType.ALL_SCHEDULED -> R.drawable.ic_proton_clock
        MessageLocationType.SENT -> R.drawable.ic_proton_paper_plane
        MessageLocationType.DRAFT -> R.drawable.ic_proton_pencil
        MessageLocationType.ALL_DRAFT -> R.drawable.ic_proton_pencil
        MessageLocationType.ALL_MAIL -> R.drawable.ic_proton_folder
        MessageLocationType.ALL_SENT -> R.drawable.ic_proton_paper_plane
        MessageLocationType.ARCHIVE -> R.drawable.ic_proton_archive_box
        MessageLocationType.TRASH -> R.drawable.ic_proton_trash
        else -> null
    }

    fun bind(
        mailboxItem: MailboxItemUiModel,
        isMultiSelectionMode: Boolean,
        mailboxLocation: MessageLocationType,
        isBeingSent: Boolean,
        areAttachmentsBeingUploaded: Boolean
    ) {
        val readStatus = mailboxItem.isRead
        val messageLocation = MessageLocationType.fromInt(
            mailboxItem.messageData?.location
                ?: MessageLocationType.INVALID.messageLocationTypeValue
        )

        setTextViewStyles(readStatus)
        setIconsTint(readStatus)

        val showBigDraftIcon = mailboxItem.isDraft && !isDraftsLocation(mailboxLocation)
        // Sender text can only be empty in drafts where we show recipients instead of senders
        if (mailboxItem.correspondentsNames.isEmpty()) {
            correspondents_text_view.text = context.getString(R.string.empty_recipients)
            sender_initial_view.bind(HYPHEN, showBigDraftIcon, isMultiSelectionMode)
        } else {
            correspondents_text_view.text = mailboxItem.correspondentsNames
            sender_initial_view.bind(
                senderText = mailboxItem.correspondentsNames.substring(0, 1),
                showDraftIcon = showBigDraftIcon,
                isMultiSelectionMode = isMultiSelectionMode
            )
        }

        subject_text_view.text = mailboxItem.subject

        time_date_text_view.text = DateUtil.formatDateTime(context, mailboxItem.lastMessageTimeMs)
        if (areAttachmentsBeingUploaded) {
            time_date_text_view.text = context.getString(R.string.draft_label_attachments_uploading)
        }
        if (isBeingSent) { // overwrite attachment text so there's no flickering between them
            time_date_text_view.text = context.getString(R.string.draft_label_message_uploading)
        }

        replyImageView.isVisible = mailboxItem.messageData?.isReplied == true &&
            !mailboxItem.messageData.isRepliedAll
        replyAllImageView.isVisible = mailboxItem.messageData?.isRepliedAll == true
        forward_image_view.isVisible = mailboxItem.messageData?.isForwarded == true

        draft_image_view.isVisible = isDraftsLocation(mailboxLocation)

        // TODO: Currently there's a bug with showing the location on certain messages.
        //  Revisit the logic with MAILAND-1422
        if (mailboxLocation in arrayOf(
                MessageLocationType.ALL_MAIL,
                MessageLocationType.STARRED,
                MessageLocationType.LABEL,
                MessageLocationType.SEARCH
            )
        ) {
            val icon = getIconForMessageLocation(messageLocation)
            if (icon != null) {
                first_location_image_view.setImageDrawable(ContextCompat.getDrawable(context, icon))
            }
            Timber.v("Message location: $messageLocation, icon: $icon, subject: ${mailboxItem.subject}")
            first_location_image_view.isVisible = icon != null
        } else {
            first_location_image_view.visibility = View.GONE
        }

        messages_number_text_view.isVisible = mailboxItem.messagesCount != null
        messages_number_text_view.text = mailboxItem.messagesCount.toString()
        sending_uploading_progress_bar.isVisible = isBeingSent || areAttachmentsBeingUploaded
        attachment_image_view.isVisible = mailboxItem.hasAttachments
        star_image_view.isVisible = mailboxItem.isStarred
        authenticity_badge_chip.isVisible = mailboxItem.isProton

        empty_space_view.isVisible = attachment_image_view.isVisible || star_image_view.isVisible

        expiration_image_view.isVisible = mailboxItem.expirationTime > 0

        labelsLayout.setLabels(mailboxItem.messageLabels)
    }

    private fun isDraftsLocation(
        mailboxLocation: MessageLocationType
    ) = mailboxLocation in arrayOf(
        MessageLocationType.DRAFT,
        MessageLocationType.ALL_DRAFT
    )
}
