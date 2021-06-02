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

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.text.format.Formatter
import android.text.method.LinkMovementMethod
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
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
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.databinding.LayoutMessageDetailsHeaderBinding
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.ui.view.LabelChipUiModel
import ch.protonmail.android.ui.view.MultiLineLabelChipGroupView
import ch.protonmail.android.ui.view.SingleLineLabelChipGroupView
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.ui.locks.SenderLockIcon
import ch.protonmail.android.views.messagesList.SenderInitialView

private const val HYPHEN = "-"

/**
 * A view for the collapsible header in message details
 */
class MessageDetailsHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val collapsedHeaderGroup: Group
    private val expandedHeaderGroup: Group
    private val expandCollapseChevronImageView: ImageView

    private val draftInitialView: ImageView

    private val senderInitialView: SenderInitialView
    private val senderNameTextView: TextView
    private val senderEmailTextView: TextView

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
    private val learnMoreTextView: TextView

    private val repliedImageView: ImageView
    private val repliedAllImageView: ImageView
    private val forwardedImageView: ImageView

    init {
        val binding = LayoutMessageDetailsHeaderBinding.inflate(
            LayoutInflater.from(context),
            this
        )
        // region init views' references
        collapsedHeaderGroup = binding.collapsedHeaderGroup
        expandedHeaderGroup = binding.expandedHeaderGroup
        expandCollapseChevronImageView = binding.expandCollapseChevronImageView

        draftInitialView = binding.draftInitialView
        senderInitialView = binding.senderInitialView
        senderNameTextView = binding.senderNameTextView
        senderEmailTextView = binding.senderEmailTextView

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
        learnMoreTextView = binding.learnMoreTextView

        repliedImageView = binding.repliedImageView
        repliedAllImageView = binding.repliedAllImageView
        forwardedImageView = binding.forwardedImageView
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

        val typefacePgp = Typeface.createFromAsset(context.assets, "pgp-icons-android.ttf")
        lockIconTextView.typeface = typefacePgp
        lockIconExtendedTextView.typeface = typefacePgp
    }

    private var isExpanded = false

    private val onChevronClickListener = OnClickListener {
        if (isExpanded) {
            isExpanded = false
            expandedHeaderGroup.visibility = View.GONE
            collapsedHeaderGroup.visibility = View.VISIBLE
            expandCollapseChevronImageView.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_chevron_down)
            )
        } else {
            isExpanded = true
            collapsedHeaderGroup.visibility = View.GONE
            expandedHeaderGroup.visibility = View.VISIBLE
            expandCollapseChevronImageView.setImageDrawable(
                ContextCompat.getDrawable(context, R.drawable.ic_chevron_up)
            )
        }
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

    private fun getMessageFolder(labelsList: List<Label>): Label? = labelsList.find { it.exclusive }

    // TODO: Showing the location is buggy, review together with MAILAND-1422
    private fun getIconForMessageLocation(messageLocation: Constants.MessageLocationType) = when (messageLocation) {
        Constants.MessageLocationType.INBOX -> R.drawable.ic_inbox
        Constants.MessageLocationType.SENT -> R.drawable.ic_paper_plane
        Constants.MessageLocationType.DRAFT -> R.drawable.ic_pencil
        Constants.MessageLocationType.ALL_DRAFT -> R.drawable.ic_pencil
        Constants.MessageLocationType.ALL_SENT -> R.drawable.ic_paper_plane
        Constants.MessageLocationType.ARCHIVE -> R.drawable.ic_archive
        Constants.MessageLocationType.TRASH -> R.drawable.ic_trash
        Constants.MessageLocationType.SPAM -> R.drawable.ic_fire
        Constants.MessageLocationType.LABEL -> R.drawable.ic_folder_filled
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
        Constants.MessageLocationType.LABEL -> getMessageFolder(labelsList)?.name
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

    fun loadRecipients(message: Message) {
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
            toExpandedTextView.visibility = View.GONE
            toRecipientsExpandedView.visibility = View.GONE
        } else {
            toRecipientsExpandedView.bind(toRecipients)
            expandedHeaderGroup.addView(toExpandedTextView)
            expandedHeaderGroup.addView(toRecipientsExpandedView)
        }
        if (ccRecipients.isEmpty()) {
            ccExpandedTextView.visibility = View.GONE
            ccRecipientsExpandedView.visibility = View.GONE
        } else {
            ccRecipientsExpandedView.bind(ccRecipients)
            expandedHeaderGroup.addView(ccExpandedTextView)
            expandedHeaderGroup.addView(ccRecipientsExpandedView)
        }
        if (bccRecipients.isEmpty()) {
            bccExpandedTextView.visibility = View.GONE
            bccRecipientsExpandedView.visibility = View.GONE
        } else {
            bccRecipientsExpandedView.bind(bccRecipients)
            expandedHeaderGroup.addView(bccExpandedTextView)
            expandedHeaderGroup.addView(bccRecipientsExpandedView)
        }
    }

    fun bind(message: Message, allLabels: List<Label>, nonInclusiveLabels: List<LabelChipUiModel>) {
        val senderText = getSenderText(message)
        val initials = if (senderText.isEmpty()) HYPHEN else senderText.substring(0, 1)
        senderInitialView.bind(initials)

        val isDraft = message.location == Constants.MessageLocationType.DRAFT.messageLocationTypeValue
        senderInitialView.visibility = if (isDraft) View.INVISIBLE else View.VISIBLE
        draftInitialView.isVisible = isDraft


        senderNameTextView.text = senderText
        senderEmailTextView.text = context.getString(R.string.recipient_email_format, message.senderEmail)
        senderEmailTextView.setOnClickListener(getOnSenderClickListener(message.senderEmail))

        if (nonInclusiveLabels.isEmpty().not()) {
            labelsCollapsedGroupView.setLabels(nonInclusiveLabels)
            labelsExpandedGroupView.setLabels(nonInclusiveLabels)
            collapsedHeaderGroup.addView(labelsCollapsedGroupView)
            expandedHeaderGroup.addView(labelsExpandedGroupView)
            expandedHeaderGroup.addView(labelsImageView)
        } else {
            labelsCollapsedGroupView.visibility = View.GONE
            labelsExpandedGroupView.visibility = View.GONE
            labelsImageView.visibility = View.GONE
        }

        val senderLockIcon = SenderLockIcon(message, message.hasValidSignature, message.hasInvalidSignature)
        lockIconTextView.text = context.getText(senderLockIcon.icon)
        lockIconTextView.setTextColor(senderLockIcon.color)
        lockIconExtendedTextView.text = context.getText(senderLockIcon.icon)
        lockIconExtendedTextView.setTextColor(senderLockIcon.color)
        encryptionInfoTextView.text = context.getText(senderLockIcon.tooltip)
        learnMoreTextView.movementMethod = LinkMovementMethod.getInstance()

        getIconForMessageLocation(Constants.MessageLocationType.fromInt(message.location))?.let { icon ->
            locationImageView.setImageDrawable(ContextCompat.getDrawable(context, icon))
            locationExtendedImageView.setImageDrawable(ContextCompat.getDrawable(context, icon))
            if (Constants.MessageLocationType.fromInt(message.location) == Constants.MessageLocationType.LABEL) {
                getMessageFolder(allLabels)?.let { label ->
                    val folderColor = Color.parseColor(UiUtil.normalizeColor(label.color))
                    locationImageView.setColorFilter(folderColor)
                    locationExtendedImageView.setColorFilter(folderColor)
                }
            }
        }
        getTextForMessageLocation(Constants.MessageLocationType.fromInt(message.location), allLabels)?.let {
            locationTextView.text = it
        }

        timeDateTextView.text = DateUtil.formatDateTime(context, message.timeMs)
        timeDateExtendedTextView.text = DateUtil.formatDetailedDateTime(context, message.timeMs)

        loadRecipients(message)

        storageTextView.text = Formatter.formatShortFileSize(context, message.totalSize)

        expandCollapseChevronImageView.setOnClickListener(onChevronClickListener)
        expandedHeaderGroup.visibility = View.GONE
        collapsedHeaderGroup.visibility = View.VISIBLE

        repliedImageView.isVisible = message.isReplied == true && message.isRepliedAll == false
        repliedAllImageView.isVisible = message.isRepliedAll ?: false
        forwardedImageView.isVisible = message.isForwarded ?: false
    }
}
