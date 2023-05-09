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

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.format.Formatter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.Group
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.details.RecipientContextMenuFactory
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.RecipientType
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.databinding.LayoutMessageDetailsHeaderBinding
import ch.protonmail.android.details.presentation.model.MessageEncryptionUiModel
import ch.protonmail.android.details.presentation.ui.MessageDetailsActivity
import ch.protonmail.android.details.presentation.view.CollapsedMessageViews
import ch.protonmail.android.details.presentation.view.MessageDetailsHeaderIcons
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.ui.view.MultiLineLabelChipGroupView
import ch.protonmail.android.ui.view.SingleLineLabelChipGroupView
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.views.messagesList.SenderInitialView
import com.google.android.material.chip.Chip

private const val HYPHEN = "-"

/**
 * A view for the collapsible header in message details
 */
class MessageDetailsHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    // region Views
    private val collapsedHeaderGroup: Group
    private val expandedHeaderGroup: Group
    private val expandCollapseChevronImageView: ImageView

    private val senderInitialView: SenderInitialView
    private val senderNameTextView: TextView
    private val senderEmailTextView: TextView
    private val authenticityBadge: Chip

    private val recipientsCollapsedTextView: TextView
    private val toExpandedTextView: TextView
    private val toRecipientsExpandedView: MessageDetailsRecipientsContainerView

    private val ccExpandedTextView: TextView
    private val ccRecipientsExpandedView: MessageDetailsRecipientsContainerView

    private val bccExpandedTextView: TextView
    private val bccRecipientsExpandedView: MessageDetailsRecipientsContainerView

    private val labelsImageView: ImageView
    private val labelsCollapsedGroupView: SingleLineLabelChipGroupView
    private val labelsExpandedGroupView: MultiLineLabelChipGroupView

    private val timeDateTextView: TextView
    private val timeDateExtendedTextView: TextView

    private val locationImageView: ImageView
    private val locationExtendedImageView: ImageView
    private val locationTextView: TextView

    private val storageTextView: TextView

    private val lockIconTextView: TextView
    private val lockIconExtendedTextView: TextView
    private val encryptionInfoTextView: TextView

    private val repliedImageView: ImageView
    private val repliedAllImageView: ImageView
    private val forwardedImageView: ImageView

    private val messageDetailsIcons: MessageDetailsHeaderIcons
    private val collapsedMessageViews: CollapsedMessageViews
    // endregion

    init {
        val binding = LayoutMessageDetailsHeaderBinding.inflate(
            LayoutInflater.from(context),
            this
        )
        // region Init views
        collapsedHeaderGroup = binding.collapsedHeaderGroup
        expandedHeaderGroup = binding.expandedHeaderGroup
        expandCollapseChevronImageView = binding.expandCollapseChevronImageView

        senderInitialView = binding.senderInitialView
        senderNameTextView = binding.senderNameTextView
        senderEmailTextView = binding.senderEmailTextView
        authenticityBadge = binding.authenticityBadgeChip

        recipientsCollapsedTextView = binding.recipientsCollapsedTextView
        toExpandedTextView = binding.toExpandedTextView
        toRecipientsExpandedView = binding.toRecipientsExpandedView

        ccExpandedTextView = binding.ccExpandedTextView
        ccRecipientsExpandedView = binding.ccRecipientsExpandedView
        bccExpandedTextView = binding.bccExpandedTextView
        bccRecipientsExpandedView = binding.bccRecipientsExpandedView

        labelsImageView = binding.labelsImageView
        labelsCollapsedGroupView = binding.labelsCollapsedGroupView
        labelsExpandedGroupView = binding.labelsExpandedGroupView

        timeDateTextView = binding.timeDateTextView
        timeDateExtendedTextView = binding.timeDateExtendedTextView

        locationImageView = binding.locationImageView
        locationExtendedImageView = binding.locationExtendedImageView
        locationTextView = binding.locationTextView

        storageTextView = binding.storageTextView

        lockIconTextView = binding.lockIconTextView
        lockIconExtendedTextView = binding.lockIconExtendedTextView
        encryptionInfoTextView = binding.encryptionInfoTextView

        repliedImageView = binding.repliedImageView
        repliedAllImageView = binding.repliedAllImageView
        forwardedImageView = binding.forwardedImageView

        messageDetailsIcons = binding.messageDetailsIcons
        collapsedMessageViews = binding.collapsedMessageViews
        // endregion

        // animated layout changes looks buggy on Android 27, so we enable only on 28 +
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            layoutTransition = LayoutTransition()
        }
        val horizontalPadding = resources.getDimensionPixelSize(R.dimen.message_details_header_padding_horizontal)
        val verticalPadding = resources.getDimensionPixelSize(R.dimen.message_details_header_padding_vertical)
        updatePadding(
            left = horizontalPadding,
            right = horizontalPadding,
            top = verticalPadding,
            bottom = verticalPadding
        )
        background = context.getDrawable(R.color.background_norm)

        val typefacePgp = Typeface.createFromAsset(context.assets, "pgp-icons-android.ttf")
        lockIconTextView.typeface = typefacePgp
        lockIconExtendedTextView.typeface = typefacePgp
    }

    private var isExpanded = false

    fun bind(
        message: Message,
        messageEncryptionUiModel: MessageEncryptionUiModel,
        exclusiveLabels: List<Label>,
        nonExclusiveLabels: List<LabelChipUiModel>,
        onHeaderCollapsed: () -> Unit
    ) {
        val senderText = getSenderText(message)
        val initials = if (senderText.isEmpty()) HYPHEN else senderText.substring(0, 1)
        senderInitialView.bind(initials, message.isDraft())

        senderNameTextView.text = senderText
        senderEmailTextView.text = context.getString(R.string.recipient_email_format, message.senderEmail)
        senderEmailTextView.setOnClickListener(getOnSenderClickListener(message.senderEmail))

        authenticityBadge.isVisible = message.sender?.isProton ?: false

        labelsCollapsedGroupView.setLabels(nonExclusiveLabels)
        labelsExpandedGroupView.setLabels(nonExclusiveLabels)
        collapsedHeaderGroup.addView(labelsCollapsedGroupView)
        expandedHeaderGroup.addView(labelsExpandedGroupView)

        // Can't control the visibility of individual views within the group as the group visibility trumps the
        // visibility of the individual views within the group; thus we want to add the icon to the group only
        // if there are labels to show, otherwise we won't be able to hide the icon.
        // See https://issuetracker.google.com/issues/73186245
        expandedHeaderGroup.removeView(labelsImageView)
        if (nonExclusiveLabels.isEmpty()) {
            labelsImageView.isVisible = false
        } else {
            expandedHeaderGroup.addView(labelsImageView)
        }

        lockIconTextView.text = context.getText(messageEncryptionUiModel.lockIcon)
        lockIconTextView.setTextColor(context.getColor(messageEncryptionUiModel.lockIconColor))
        lockIconExtendedTextView.text = context.getText(messageEncryptionUiModel.lockIcon)
        lockIconExtendedTextView.setTextColor(context.getColor(messageEncryptionUiModel.lockIconColor))
        encryptionInfoTextView.text = context.getText(messageEncryptionUiModel.tooltip)

        val messageLocation = message.location
        getIconForMessageLocation(Constants.MessageLocationType.fromInt(messageLocation))?.let { icon ->
            locationImageView.setImageDrawable(ContextCompat.getDrawable(context, icon))
            locationExtendedImageView.setImageDrawable(ContextCompat.getDrawable(context, icon))
            // If the location is a custom folder, then make sure to color the icon in the correct folder color
            if (Constants.MessageLocationType.fromInt(messageLocation) in arrayOf(
                    Constants.MessageLocationType.LABEL_FOLDER,
                    Constants.MessageLocationType.LABEL
                ) && exclusiveLabels.isNotEmpty()
            ) {
                // There should be only one exclusive label for a message
                val folderColor = Color.parseColor(UiUtil.normalizeColor(exclusiveLabels[0].color))
                locationImageView.setColorFilter(folderColor)
                locationExtendedImageView.setColorFilter(folderColor)
            } else {
                locationImageView.clearColorFilter()
                locationExtendedImageView.clearColorFilter()
            }
        }
        getTextForMessageLocation(Constants.MessageLocationType.fromInt(messageLocation), exclusiveLabels)?.let {
            locationTextView.text = it
        }

        timeDateTextView.text = DateUtil.formatDateTime(context, message.timeMs)
        timeDateExtendedTextView.text = DateUtil.formatDetailedDateTime(context, message.timeMs)

        loadRecipients(message)

        storageTextView.text = Formatter.formatShortFileSize(context, message.totalSize)

        expandCollapseChevronImageView.setOnClickListener {
            if (isExpanded) {
                collapseHeader()
                onHeaderCollapsed()
            } else {
                expandHeader()
            }
            it.contentDescription = context?.getString(
                if (isExpanded) R.string.expand_message_details else R.string.collapse_message_details
            ) ?: ""
            isExpanded = isExpanded.not()
        }
        expandedHeaderGroup.isVisible = false
        collapsedHeaderGroup.isVisible = true

        repliedImageView.isVisible = message.isReplied == true && message.isRepliedAll == false
        repliedAllImageView.isVisible = message.isRepliedAll ?: false
        forwardedImageView.isVisible = message.isForwarded ?: false

        messageDetailsIcons.bind(message)
        collapsedMessageViews.bind(message, nonExclusiveLabels)
    }

    fun allowExpandingHeaderView() {
        expandCollapseChevronImageView.isVisible = true
    }

    fun forbidExpandingHeaderView() {
        expandCollapseChevronImageView.isVisible = false
    }

    fun hideRecipientsCollapsedView() {
        collapsedHeaderGroup.isVisible = false
    }

    fun showRecipientsCollapsedView() {
        collapsedHeaderGroup.isVisible = true
    }

    fun showCollapsedMessageViews() {
        collapsedMessageViews.isVisible = true
    }

    fun hideCollapsedMessageViews() {
        collapsedMessageViews.isVisible = false
    }

    fun collapseHeader() {
        expandedHeaderGroup.isVisible = false
        collapsedHeaderGroup.isVisible = true
        locationImageView.isVisible = true
        expandCollapseChevronImageView.setImageDrawable(
            ContextCompat.getDrawable(context, R.drawable.ic_proton_chevron_down)
        )
    }

    fun showMessageAsUnread() {
        senderNameTextView.setTypeface(null, Typeface.BOLD)
        timeDateTextView.setTypeface(null, Typeface.BOLD)
        timeDateTextView.setTextColor(context.getColor(R.color.text_norm))
    }

    fun showMessageAsRead() {
        senderNameTextView.setTypeface(null, Typeface.NORMAL)
        timeDateTextView.setTypeface(null, Typeface.NORMAL)
        timeDateTextView.setTextColor(context.getColor(R.color.text_weak))
    }

    private fun expandHeader() {
        collapsedHeaderGroup.isVisible = false
        expandedHeaderGroup.isVisible = true
        locationImageView.isVisible = false
        expandCollapseChevronImageView.setImageDrawable(
            ContextCompat.getDrawable(context, R.drawable.ic_proton_chevron_up)
        )
    }

    private fun getSenderText(message: Message): String {
        return when {
            message.senderDisplayName.isNullOrEmpty().not() -> {
                message.senderDisplayName!!
            }
            message.senderName.isNullOrEmpty().not() -> {
                message.senderName!!
            }
            else -> {
                message.senderEmail
            }
        }
    }

    private fun getOnSenderClickListener(senderEmail: String): OnClickListener {
        val recipientContextMenuFactory = RecipientContextMenuFactory(context as MessageDetailsActivity)
        return recipientContextMenuFactory.invoke(senderEmail)
    }

    private fun getIconForMessageLocation(messageLocation: Constants.MessageLocationType) = when (messageLocation) {
        Constants.MessageLocationType.INBOX -> R.drawable.ic_proton_inbox
        Constants.MessageLocationType.SENT -> R.drawable.ic_proton_paper_plane
        Constants.MessageLocationType.DRAFT -> R.drawable.ic_proton_pencil
        Constants.MessageLocationType.ALL_DRAFT -> R.drawable.ic_proton_pencil
        Constants.MessageLocationType.ALL_SENT -> R.drawable.ic_proton_paper_plane
        Constants.MessageLocationType.ARCHIVE -> R.drawable.ic_proton_archive_box
        Constants.MessageLocationType.TRASH -> R.drawable.ic_proton_trash
        Constants.MessageLocationType.SPAM -> R.drawable.ic_proton_fire
        Constants.MessageLocationType.LABEL_FOLDER -> R.drawable.ic_proton_folder_filled
        Constants.MessageLocationType.LABEL -> R.drawable.ic_proton_folder_filled
        else -> null
    }

    private fun getTextForMessageLocation(
        messageLocation: Constants.MessageLocationType,
        labelsList: List<Label>
    ) = when (messageLocation) {
        Constants.MessageLocationType.INBOX -> context.getString(R.string.inbox)
        Constants.MessageLocationType.SENT -> context.getString(R.string.sent)
        Constants.MessageLocationType.DRAFT -> context.getString(R.string.drafts)
        Constants.MessageLocationType.ALL_DRAFT -> context.getString(R.string.drafts)
        Constants.MessageLocationType.ALL_SENT -> context.getString(R.string.sent)
        Constants.MessageLocationType.ARCHIVE -> context.getString(R.string.archive)
        Constants.MessageLocationType.TRASH -> context.getString(R.string.trash)
        Constants.MessageLocationType.SPAM -> context.getString(R.string.spam)
        Constants.MessageLocationType.LABEL_FOLDER -> { if (labelsList.isNotEmpty()) labelsList[0].name else null }
        Constants.MessageLocationType.LABEL -> { if (labelsList.isNotEmpty()) labelsList[0].name else null }
        else -> null
    }

    private fun getRecipientText(messageRecipient: MessageRecipient) =
        if (messageRecipient.name.isNullOrEmpty()) messageRecipient.emailAddress else messageRecipient.name

    private fun getCollapsedRecipients(
        toRecipients: List<MessageRecipient>,
        ccRecipients: List<MessageRecipient>,
        bccRecipients: List<MessageRecipient>
    ): String {
        if (toRecipients.isEmpty() && ccRecipients.isEmpty() && bccRecipients.isEmpty()) {
            return context.getString(R.string.undisclosed_recipients)
        }
        val stringBuilder = StringBuilder()
        toRecipients.forEachIndexed { index, messageRecipient ->
            stringBuilder.append(getRecipientText(messageRecipient))
            if (index != toRecipients.size - 1 || ccRecipients.isEmpty().not()) {
                stringBuilder.append(", ")
            }
        }
        ccRecipients.forEachIndexed { index, messageRecipient ->
            stringBuilder.append(getRecipientText(messageRecipient))
            if (index != ccRecipients.size - 1 || bccRecipients.isEmpty().not()) {
                stringBuilder.append(", ")
            }
        }
        bccRecipients.forEachIndexed { index, messageRecipient ->
            stringBuilder.append(getRecipientText(messageRecipient))
            if (index != bccRecipients.size - 1) {
                stringBuilder.append(", ")
            }
        }
        return stringBuilder.toString()
    }

    private fun loadRecipients(message: Message) {
        val toRecipients = message.getList(RecipientType.TO)
        val ccRecipients = message.getList(RecipientType.CC)
        val bccRecipients = message.getList(RecipientType.BCC)
        recipientsCollapsedTextView.text = getCollapsedRecipients(toRecipients, ccRecipients, bccRecipients)
        if (toRecipients.isEmpty() && ccRecipients.isEmpty() && bccRecipients.isEmpty()) {
            toRecipientsExpandedView.bind(listOf())
            expandedHeaderGroup.addView(toExpandedTextView)
            expandedHeaderGroup.addView(toRecipientsExpandedView)
        }
        if (toRecipients.isEmpty()) {
            toExpandedTextView.isVisible = false
            toRecipientsExpandedView.isVisible = false
        } else {
            toRecipientsExpandedView.bind(toRecipients)
            expandedHeaderGroup.addView(toExpandedTextView)
            expandedHeaderGroup.addView(toRecipientsExpandedView)
        }
        if (ccRecipients.isEmpty()) {
            ccExpandedTextView.isVisible = false
            ccRecipientsExpandedView.isVisible = false
        } else {
            ccRecipientsExpandedView.bind(ccRecipients)
            expandedHeaderGroup.addView(ccExpandedTextView)
            expandedHeaderGroup.addView(ccRecipientsExpandedView)
        }
        if (bccRecipients.isEmpty()) {
            bccExpandedTextView.isVisible = false
            bccRecipientsExpandedView.isVisible = false
        } else {
            bccRecipientsExpandedView.bind(bccRecipients)
            expandedHeaderGroup.addView(bccExpandedTextView)
            expandedHeaderGroup.addView(bccRecipientsExpandedView)
        }
    }

}
