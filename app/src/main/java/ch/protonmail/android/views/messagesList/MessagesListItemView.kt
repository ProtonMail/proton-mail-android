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

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import ch.protonmail.android.R
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.UiUtil
import kotlinx.android.synthetic.main.messages_list_item_new.view.*

// region constants
private const val CHECKBOX_WIDTH_IN_DP = 34
private const val MAX_LABELS_WITH_TEXT = 1
// endregion

/**
 * Created by Kamil Rajtar on 16.07.18.
 */

class MessagesListItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    init {
        inflate(context, R.layout.messages_list_item_new, this)
        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private val mCheckBoxWidthInPixels: Int by lazy {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                CHECKBOX_WIDTH_IN_DP.toFloat(),
                context.resources.displayMetrics).toInt()
    }

    private val mStrokeWidth by lazy {
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context
                .resources.displayMetrics).toInt()
    }

    private var allFolders = HashMap<String, Label>()
    private var mIsAnimating = false

    fun onSelectionModeChanged(isInSelectionMode: Boolean) {
        val start = if (isInSelectionMode) 0 else 1
        val end = if (isInSelectionMode) 1 else 0
        val animator = ValueAnimator.ofFloat(start.toFloat(), end.toFloat())

        val startMargin = if (isInSelectionMode) -mCheckBoxWidthInPixels else 0
        val endMargin = if (isInSelectionMode) 0 else -mCheckBoxWidthInPixels
        val animatorMargin = ValueAnimator.ofInt(startMargin, endMargin)

        animator.addUpdateListener { animation ->
            val alpha = animation.animatedValue as Float
            checkboxImageView.alpha = alpha
            if (alpha == 1.0f) {
                checkboxImageView.visibility = View.VISIBLE
            } else if (alpha == 0.0f) {
                checkboxImageView.visibility = View.GONE
            }
        }

        animatorMargin.addUpdateListener { animation ->
            val leftMargin = animation.animatedValue as Int

            val paramsTitle = messageTitleContainerLinearLayout.layoutParams as ConstraintLayout.LayoutParams
            val paramsSender = messageSenderContainerLinearLayout.layoutParams as ConstraintLayout.LayoutParams
            paramsTitle.setMargins(leftMargin, 0, 0, 0)
            paramsSender.setMargins(leftMargin, 0, 0, 0)
            messageTitleContainerLinearLayout.layoutParams = paramsTitle
            messageSenderContainerLinearLayout.layoutParams = paramsSender
            mIsAnimating = leftMargin != end
        }

        mIsAnimating = true
        animator.start()
        animatorMargin.start()
    }

    // TODO simplify as much as possible
    fun bind(message: Message,
             labels: List<Label>,
             isMultiSelectionMode: Boolean,
             mailboxLocation: Constants.MessageLocationType,
             typeface: Typeface
    ) {
        val backgroundResId: Int
        val primaryTextStyle: Int
        val secondaryTextStyle: Int

        val read = message.isRead
        if (read) {
            backgroundResId = R.drawable.read_message_bg_selector
            primaryTextStyle = R.style.MessagePrimaryText_Read
            secondaryTextStyle = R.style.MessageSecondaryText_Read
        } else {
            backgroundResId = R.drawable.unread_message_bg_selector
            primaryTextStyle = R.style.MessagePrimaryText_Unread
            secondaryTextStyle = R.style.MessageSecondaryText_Unread
        }

        dataContainerConstraintLayout.setBackgroundResource(backgroundResId)

        messageTitleTextView.text = message.subject
        TextViewCompat.setTextAppearance(messageTitleTextView, primaryTextStyle)

        val messageSenderText = when {
            Constants.MessageLocationType.fromInt(message.location) in arrayOf(Constants.MessageLocationType.DRAFT,
                    Constants.MessageLocationType.SENT) -> message.toListStringGroupsAware
            !message.senderDisplayName.isNullOrEmpty() -> message.senderDisplayName
            else -> message.senderEmail
        }
        messageSenderTextView.text = messageSenderText
        TextViewCompat.setTextAppearance(messageSenderTextView, secondaryTextStyle)

        labelsLinearLayout.removeAllViews()
        labelsLinearLayout.visibility = View.GONE
        val messageLabelsIDs = message.allLabelIDs
        val isMessageSentAndTrashed =
                message.searchForLocation(Constants.MessageLocationType.SENT.messageLocationTypeValue) &&
                        messageLabelsIDs.any { it == Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString() }
        val isMessageSentAndArchived =
                message.searchForLocation(Constants.MessageLocationType.SENT.messageLocationTypeValue) &&
                        messageLabelsIDs.any { it == Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString() }
        //region labels
        labels.forEach { allFolders[it.id] = it }
        val nonExclusiveLabels = labels.filter { !it.exclusive }

        var commonHeight = 20
        nonExclusiveLabels.forEachIndexed { i, (_, name, colorString) ->
            val labelItemView = ItemLabelMarginlessSmallView(context)
            val color = when {
                colorString.isNotEmpty() -> {
                    val normalizedColor = UiUtil.normalizeColor(colorString)
                    Color.parseColor(normalizedColor)
                }
                else -> 0
            }

            if (i < MAX_LABELS_WITH_TEXT) {
                labelItemView.bind(name, color, mStrokeWidth)
                labelsLinearLayout.visibility = View.VISIBLE
                labelsLinearLayout.addView(labelItemView)
                labelItemView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
                commonHeight = labelItemView.measuredHeight
            } else {
                val imageView = ImageView(context)
                val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT)
                lp.setMargins(0, 0, 0, 0)
                imageView.layoutParams = lp
                imageView.setImageResource(R.drawable.mail_label_collapsed)
                imageView.setColorFilter(color)
                imageView.layoutParams.height = commonHeight
                labelsLinearLayout.visibility = View.VISIBLE
                labelsLinearLayout.addView(imageView)
            }
            labelItemView.requestLayout()
        }
        //endregion

        messageLabelTrashTextView.visibility =
                if (mailboxLocation != Constants.MessageLocationType.TRASH &&
                isMessageSentAndTrashed) View.VISIBLE else View.GONE
        messageLabelArchiveTextView.visibility =
                if (mailboxLocation != Constants.MessageLocationType.ARCHIVE &&
                isMessageSentAndArchived) View.VISIBLE else View.GONE
        messageStarredTextView.typeface = typeface
        messageExpirationTextView.typeface = typeface
        messageAttachmentTextView.typeface = typeface
        messageReplyTextView.typeface = typeface
        messageLabelTrashTextView.typeface = typeface
        messageLabelArchiveTextView.typeface = typeface
        messageReplyAllTextView.typeface = typeface
        messageForwardTextView.typeface = typeface

        messageDateTextView.text = DateUtil.formatDateTime(context, message.timeMs)
        TextViewCompat.setTextAppearance(messageDateTextView, secondaryTextStyle)
        messageExpirationTextView.visibility = if (message.expirationTime > 0) View.VISIBLE else View.GONE
        val hasAttachments = message.Attachments.isNotEmpty() || message.numAttachments >= 1 || message.hasPendingUploads()
        messageAttachmentTextView.visibility = if (hasAttachments) View.VISIBLE else View.GONE
        messageReplyTextView.visibility = if (message.isReplied == true && message.isRepliedAll != true) View.VISIBLE else View.GONE
        messageReplyAllTextView.visibility = if (message.isRepliedAll == true) View.VISIBLE else View.GONE
        messageForwardTextView.visibility = if (message.isForwarded == true) View.VISIBLE else View.GONE
        messageStarredTextView.visibility = if (message.isStarred == true) View.VISIBLE else View.GONE

        if (mailboxLocation in arrayOf(Constants.MessageLocationType.STARRED,
                        Constants.MessageLocationType.LABEL,
                        Constants.MessageLocationType.SEARCH,
                        Constants.MessageLocationType.ALL_MAIL,
                        Constants.MessageLocationType.SENT)) {
            val title = if (mailboxLocation == Constants.MessageLocationType.SENT)
                message.getFolderTitle()
            else
                message.getLocationOrFolderTitle()
            if (!title.isNullOrEmpty()) {
                messageLocationTextView.text = title
                messageLocationTextView.visibility = View.VISIBLE
            } else {
                messageLocationTextView.visibility = View.GONE
            }
        } else {
            messageLocationTextView.visibility = View.GONE
        }

        // Don't update the margins if the view is currently being animated
        if (!mIsAnimating) {
            val leftMargin = if (isMultiSelectionMode) 0 else -mCheckBoxWidthInPixels
            if (leftMargin < 0) {
                checkboxImageView.alpha = 0f
                checkboxImageView.visibility = View.GONE
            } else {
                checkboxImageView.visibility = View.VISIBLE
                checkboxImageView.alpha = 1f
            }
            val paramsTitle = messageTitleContainerLinearLayout.layoutParams as ConstraintLayout.LayoutParams
            val paramsSender = messageSenderContainerLinearLayout.layoutParams as ConstraintLayout.LayoutParams
            paramsTitle.setMargins(leftMargin, 0, 0, 0)
            paramsSender.setMargins(leftMargin, 0, 0, 0)
            messageTitleContainerLinearLayout.layoutParams = paramsTitle
            messageSenderContainerLinearLayout.layoutParams = paramsSender
        }

        uploadCircularProgressBar.visibility = if (message.isBeingSent || message.isAttachmentsBeingUploaded) View.VISIBLE else View.GONE
        if (message.isAttachmentsBeingUploaded) {
            messageDateTextView.text = context.getString(R.string.draft_label_attachments_uploading)
        }
        if (message.isBeingSent) { // overwrite attachment text so there's no flickering between them
            messageDateTextView.text = context.getString(R.string.draft_label_message_uploading)
        }

    }


    private fun Message.hasPendingUploads(): Boolean {
        val messageId = messageId
        //TODO restore
        return false
//		return !messageId.isNullOrEmpty()&&PendingUpload.findByMessageId(messageId)!=null
    }

    private fun Message.getLocationOrFolderTitle(): String? {
        var messageLocation = Constants.MessageLocationType.fromInt(location)
        for (labelId in allLabelIDs) {
            val label = allFolders[labelId]
            if (label != null && label.exclusive && label.name.isNotEmpty()) {
                return label.name
            }
            if (labelId.length <= 2) {
                messageLocation = Constants.MessageLocationType.fromInt(Integer.valueOf(labelId))
                if (messageLocation !in arrayOf(Constants.MessageLocationType.STARRED,
                                Constants.MessageLocationType.ALL_MAIL,
                                Constants.MessageLocationType.INVALID,
                                Constants.MessageLocationType.ALL_DRAFT,
                                Constants.MessageLocationType.ALL_SENT
                        )) {
                    break
                }
            }
        }

        return getLocationTitleId(messageLocation)?.let {
            context.getString(it)
        } ?: ""
    }

    private fun getLocationTitleId(mailboxLocation: Constants.MessageLocationType): Int? {
        return when (mailboxLocation) {
            Constants.MessageLocationType.INBOX -> R.string.inbox_option
            Constants.MessageLocationType.STARRED -> R.string.starred_option
            Constants.MessageLocationType.DRAFT -> R.string.drafts_option
            Constants.MessageLocationType.SENT -> R.string.sent_option
            Constants.MessageLocationType.ARCHIVE -> R.string.archive_option
            Constants.MessageLocationType.TRASH -> R.string.trash_option
            Constants.MessageLocationType.SPAM -> R.string.spam_option
            Constants.MessageLocationType.ALL_MAIL -> null
            else -> R.string.app_name
        }
    }

    private fun Message.getFolderTitle(): String {
        return allLabelIDs.asReversed().asSequence().map(allFolders::get).filterNotNull().filter(Label::exclusive).map { it.name }.lastOrNull()
                ?: ""
    }
}

