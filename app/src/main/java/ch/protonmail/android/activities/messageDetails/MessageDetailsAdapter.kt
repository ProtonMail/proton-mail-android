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
package ch.protonmail.android.activities.messageDetails

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.os.Build
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.Gravity
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.attachments.MessageDetailsAttachmentListAdapter
import ch.protonmail.android.activities.messageDetails.body.MessageBodyScaleListener
import ch.protonmail.android.activities.messageDetails.body.MessageBodyTouchListener
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.model.SignatureVerification
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.details.presentation.MessageDetailsListItem
import ch.protonmail.android.details.presentation.mapper.MessageEncryptionUiModelMapper
import ch.protonmail.android.details.presentation.mapper.MessageToMessageDetailsListItemMapper
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.details.presentation.view.MessageDetailsActionsView
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.ui.model.LabelChipUiModel
import ch.protonmail.android.utils.redirectToChrome
import ch.protonmail.android.utils.ui.ExpandableRecyclerAdapter
import ch.protonmail.android.utils.ui.TYPE_HEADER
import ch.protonmail.android.utils.ui.TYPE_ITEM
import ch.protonmail.android.utils.webview.SetUpWebViewDarkModeHandlingIfSupported
import ch.protonmail.android.views.PMWebViewClient
import ch.protonmail.android.views.messageDetails.MessageDetailsAttachmentsView
import ch.protonmail.android.views.messageDetails.MessageDetailsHeaderView
import ch.protonmail.android.views.messageDetails.ReplyActionsView
import kotlinx.android.synthetic.main.layout_message_details.view.*
import kotlinx.android.synthetic.main.layout_message_details_web_view.view.*
import org.apache.http.protocol.HTTP
import timber.log.Timber

/**
 * The delay after which we check if we can switch from a fixed message content height to WRAP_CONTENT. This behaviour
 * is needed in order to keep the message header attached to the top of the screen while the content is being loaded.
 */
private const val WRAP_MESSAGE_CONTENT_DELAY_MS = 100L

private const val ITEM_NOT_FOUND_INDEX = -1

internal class MessageDetailsAdapter(
    private val context: Context,
    private var messages: List<Message>,
    private val messageDetailsRecyclerView: RecyclerView,
    private val messageToMessageDetailsListItemMapper: MessageToMessageDetailsListItemMapper,
    private val userManager: UserManager,
    private val messageEncryptionUiModelMapper: MessageEncryptionUiModelMapper,
    private val setUpWebViewDarkModeHandlingIfSupported: SetUpWebViewDarkModeHandlingIfSupported,
    private val onLoadEmbeddedImagesClicked: (Message, List<String>) -> Unit,
    private val onDisplayRemoteContentClicked: (Message) -> Unit,
    private val onLoadMessageBody: (Message) -> Unit,
    private val onAttachmentDownloadCallback: (Attachment) -> Unit,
    private val onEditDraftClicked: (Message) -> Unit,
    private val onReplyMessageClicked: (Constants.MessageActionType, Message) -> Unit,
    private val onMoreMessageActionsClicked: (Message) -> Unit
) : ExpandableRecyclerAdapter<MessageDetailsListItem>(context) {

    private var exclusiveLabelsPerMessage: HashMap<String, List<Label>> = hashMapOf()
    private var nonExclusiveLabelsPerMessage: HashMap<String, List<LabelChipUiModel>> = hashMapOf()

    private val messageLoadingSpinnerTopMargin by lazy {
        context.resources.getDimension(R.dimen.padding_m).toInt()
    }

    private val messageContentFixedLoadingHeight by lazy {
        context.resources.getDimension(R.dimen.constrained_message_content_view_size).toInt()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_HEADER) {
            (holder as HeaderViewHolder).bind(
                visibleItems[position].message
            )
        } else {
            val isLastNonDraftItemInTheList = position == visibleItems.indexOfLast { !it.message.isDraft() }
            (holder as ItemViewHolder).bind(
                position,
                visibleItems[position],
                isLastNonDraftItemInTheList
            )
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderViewHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.layout_message_details,
                    parent,
                    false
                )
            )
        } else {
            val itemView = LayoutInflater.from(context).inflate(
                R.layout.layout_message_details_web_view,
                parent,
                false
            )

            val webView = setupMessageBodyWebView(itemView)
            itemView.messageWebViewContainer.removeAllViews()
            itemView.messageWebViewContainer.addView(webView)

            val messageBodyProgress = createMessageBodyProgressBar()
            itemView.messageWebViewContainer.addView(messageBodyProgress)

            if (showingMoreThanOneMessage()) {
                val detailsMessageActions = createInMessageActionsView()
                itemView.messageWebViewContainer.addView(detailsMessageActions)
            }

            val replyActionsView = createReplyActionsView()
            itemView.messageWebViewContainer.addView(replyActionsView)

            setInitialMessageBodyHeight(itemView.messageWebViewContainer)

            ItemViewHolder(itemView)
        }
    }

    private fun setInitialMessageBodyHeight(messageWebViewContainer: LinearLayout) {
        val isMultiMessageConversation = visibleItems.size > 2
        // For single message conversations, we don't need to artificially expand the height while loading, as the
        // top of the only message will always be attached to the top of the screen. For multi-message conversations,
        // we need to expand it to make sure it occupies enough space on the screen to stay attached to the top
        // until the content is loaded.
        if (isMultiMessageConversation) {
            setMessageContentExpandedLoadingHeight(messageWebViewContainer)
        } else {
            setMessageContentFixedLoadingHeight(messageWebViewContainer)
        }
    }

    private fun createMessageBodyProgressBar(): ProgressBar {
        return ProgressBar(context).apply {
            id = R.id.item_message_body_progress_view_id
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                setMargins(0, messageLoadingSpinnerTopMargin, 0, 0)
            }
        }
    }

    private fun showingMoreThanOneMessage() = messages.size > 1

    private fun createInMessageActionsView(): MessageDetailsActionsView {
        val detailsMessageActions = MessageDetailsActionsView(context)
        detailsMessageActions.id = R.id.item_message_body_actions_layout_id
        detailsMessageActions.isVisible = false
        return detailsMessageActions
    }

    private fun createReplyActionsView(): ReplyActionsView {
        val replyActionsView = ReplyActionsView(context)
        replyActionsView.id = R.id.item_message_body_reply_actions_layout_id
        replyActionsView.isVisible = false
        return replyActionsView
    }

    private fun setupMessageBodyWebView(itemView: View): WebView? {
        val context = context as MessageDetailsActivity
        // Looks like some devices are not able to create a WebView in some conditions.
        // Show Toast and redirect to the proper page.
        val webView = try {
            WebView(context)
        } catch (ignored: Throwable) {
            (context as FragmentActivity).redirectToChrome()
            return null
        }
        webView.id = R.id.item_message_body_web_view_id

        val webViewClient = MessageDetailsPmWebViewClient(userManager, context, itemView, shouldShowRemoteImages())
        configureWebView(webView, webViewClient)
        setUpScrollListener(webView, itemView.messageWebViewContainer)

        webView.invalidate()
        context.registerForContextMenu(webView)

        return webView
    }

    inner class HeaderViewHolder(
        view: View
    ) : ExpandableRecyclerAdapter<MessageDetailsListItem>.HeaderViewHolder(view) {

        fun bind(message: Message) {
            val messageDetailsHeaderView = itemView.headerView
            val signatureVerification = when {
                message.hasValidSignature -> SignatureVerification.SUCCESSFUL
                message.hasInvalidSignature -> SignatureVerification.FAILED
                else -> SignatureVerification.UNKNOWN
            }
            val messageEncryptionStatus = messageEncryptionUiModelMapper.messageEncryptionToUiModel(
                message.messageEncryption,
                signatureVerification,
                message.isSent
            )
            messageDetailsHeaderView.bind(
                message,
                messageEncryptionStatus,
                exclusiveLabelsPerMessage[message.messageId] ?: listOf(),
                nonExclusiveLabelsPerMessage[message.messageId] ?: listOf(),
                ::onHeaderCollapsed
            )

            messageDetailsHeaderView.setOnClickListener { view ->
                val headerView = view as MessageDetailsHeaderView

                if (isMessageBodyExpanded()) {
                    // Message Body is expended - will collapse
                    headerView.collapseHeader()
                }
                toggleExpandedItems(layoutPosition, false)
                notifyItemChanged(layoutPosition)
            }

            if (isMessageBodyExpanded()) {
                messageDetailsHeaderView.allowExpandingHeaderView()
                messageDetailsHeaderView.showRecipientsCollapsedView()
                messageDetailsHeaderView.hideCollapsedMessageViews()
            } else {
                messageDetailsHeaderView.hideRecipientsCollapsedView()
                messageDetailsHeaderView.showCollapsedMessageViews()
            }

            if (message.isRead) {
                messageDetailsHeaderView.showMessageAsRead()
            } else {
                messageDetailsHeaderView.showMessageAsUnread()
            }

            if (isLastItemHeader()) {
                itemView.lastConversationMessageCollapsedDivider.isVisible = !isMessageBodyExpanded()
            }
        }

        private fun onHeaderCollapsed() {
            notifyItemChanged(layoutPosition)
        }

        private fun isMessageBodyExpanded() = isExpanded(layoutPosition)

        private fun isLastItemHeader(): Boolean {
            val lastHeaderItem = visibleItems.last { it.itemType == TYPE_HEADER }
            return layoutPosition == visibleItems.indexOf(lastHeaderItem)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.messageWebViewContainer?.let {
            resetWebViewContent(it)
        }

        holder.itemView.lastConversationMessageCollapsedDivider?.let { it.isVisible = false }
        holder.itemView.headerView?.forbidExpandingHeaderView()
        holder.itemView.headerView?.hideRecipientsCollapsedView()
    }

    inner class ItemViewHolder(view: View) : ExpandableRecyclerAdapter<MessageDetailsListItem>.ViewHolder(view) {

        fun bind(position: Int, listItem: MessageDetailsListItem, isLastNonDraftItemInTheList: Boolean) {
            val message = listItem.message
            Timber.v("Bind item: ${message.messageId}, isDownloaded: ${message.isDownloaded}")
            val attachmentsView = itemView.attachmentsView
            attachmentsView.visibility = View.GONE

            val expirationInfoView = itemView.expirationInfoView
            val decryptionErrorView = itemView.decryptionErrorView
            val displayRemoteContentButton = itemView.displayRemoteContentButton
            val loadEmbeddedImagesButton = itemView.loadEmbeddedImagesButton
            val editDraftButton = itemView.editDraftButton
            val webView = itemView.messageWebViewContainer
                .findViewById<WebView>(R.id.item_message_body_web_view_id) ?: return
            val messageBodyProgress = itemView.messageWebViewContainer
                .findViewById<ProgressBar>(R.id.item_message_body_progress_view_id) ?: return

            messageBodyProgress.isVisible = listItem.messageFormattedHtml.isNullOrEmpty()
            displayRemoteContentButton.isVisible = false
            loadEmbeddedImagesButton.isVisible = listItem.showLoadEmbeddedImagesButton
            editDraftButton.isVisible = message.isDraft()
            decryptionErrorView.bind(listItem.showDecryptionError)
            expirationInfoView.bind(message.expirationTime)
            setUpSpamScoreView(message.spamScore, itemView.spamScoreView)

            if (listItem.messageFormattedHtml == null) {
                Timber.v("Load body for message: ${message.messageId} at position $position, loc: ${message.location}")
                onLoadMessageBody(message)
            }

            val htmlContent = if (showingMoreThanOneMessage() || messageHasNoQuotedPart(listItem)) {
                listItem.messageFormattedHtml
            } else {
                listItem.messageFormattedHtmlWithQuotedHistory
            }
            htmlContent?.let {
                loadHtmlDataIntoWebView(webView, it)
            }

            displayAttachmentInfo(listItem.message.attachments, attachmentsView)
            setUpViewDividers()

            val shouldHideAllActions = htmlContent.isNullOrEmpty() || message.isDraft()
            setupMessageActionsView(
                message, listItem.messageFormattedHtmlWithQuotedHistory, webView, shouldHideAllActions
            )
            // TODO: To be decided whether we will need these actions moving forward or they can be removed completely
            setupReplyActionsView(message, true)
            setupMessageContentActions(position, loadEmbeddedImagesButton, displayRemoteContentButton, editDraftButton)

            setMessageContentHeight(listItem, isLastNonDraftItemInTheList)
        }

        private fun messageHasNoQuotedPart(listItem: MessageDetailsListItem) =
            listItem.messageFormattedHtmlWithQuotedHistory == null

        private fun setupMessageActionsView(
            message: Message,
            messageHtmlWithQuotedHistory: String?,
            webView: WebView,
            shouldHideAllActions: Boolean
        ) {
            val messageActionsView: MessageDetailsActionsView =
                itemView.messageWebViewContainer.findViewById(R.id.item_message_body_actions_layout_id) ?: return

            val uiModel = MessageDetailsActionsView.UiModel(
                messageHtmlWithQuotedHistory.isNullOrEmpty(),
                shouldHideAllActions
            )
            messageActionsView.bind(uiModel)

            messageActionsView.onShowHistoryClicked { showHistoryButton ->
                loadHtmlDataIntoWebView(webView, messageHtmlWithQuotedHistory.orEmpty())
                showHistoryButton.isVisible = false
            }
            messageActionsView.onMoreActionsClicked { onMoreMessageActionsClicked(message) }
        }

        private fun setupReplyActionsView(
            message: Message,
            shouldHideAllActions: Boolean
        ) {
            val replyActionsView: ReplyActionsView =
                itemView.messageWebViewContainer.findViewById(R.id.item_message_body_reply_actions_layout_id) ?: return

            replyActionsView.bind(
                shouldShowReplyAllAction = message.toList.size + message.ccList.size > 1,
                shouldHideAllActions = shouldHideAllActions
            )

            replyActionsView.onReplyActionClicked {
                onReplyMessageClicked(Constants.MessageActionType.REPLY, message)
            }
            replyActionsView.onReplyAllActionClicked {
                onReplyMessageClicked(Constants.MessageActionType.REPLY_ALL, message)
            }
            replyActionsView.onForwardActionClicked {
                onReplyMessageClicked(Constants.MessageActionType.FORWARD, message)
            }
        }

        private fun loadHtmlDataIntoWebView(webView: WebView, htmlContent: String) {
            webView.loadDataWithBaseURL(
                Constants.DUMMY_URL_PREFIX,
                htmlContent,
                "text/html",
                HTTP.UTF_8,
                ""
            )
        }

        private fun setupMessageContentActions(
            position: Int,
            loadEmbeddedImagesContainer: Button,
            displayRemoteContentButton: Button,
            editDraftButton: Button
        ) {
            loadEmbeddedImagesContainer.setOnClickListener { view ->
                view.visibility = View.GONE
                // Once images were loaded for one message, we automatically load them for all the others, so:
                // the 'load embedded images' button will be hidden for all messages
                // the 'formatted html' gets reset so that messages which were already rendered without images
                // go through the rendering again (through `onLoadMessageBody` callback) and load them
                allItems.map {
                    it.showLoadEmbeddedImagesButton = false
                    it.showDecryptionError = false
                    it.messageFormattedHtml = null
                }
                val item = visibleItems[position]
                onLoadEmbeddedImagesClicked(item.message, item.embeddedImageIds)
            }

            displayRemoteContentButton.setOnClickListener {
                val item = visibleItems[position]
                val webView = itemView.messageWebViewContainer.findViewById<WebView>(R.id.item_message_body_web_view_id)

                if (webView != null && webView.contentHeight > 0) {
                    itemView.displayRemoteContentButton.isVisible = false
                    (webView.webViewClient as MessageDetailsPmWebViewClient).allowLoadingRemoteResources()
                    webView.reload()
                    webView.invalidate()
                    onDisplayRemoteContentClicked(item.message)
                }
            }

            editDraftButton.setOnClickListener {
                val item = visibleItems[position]
                onEditDraftClicked(item.message)
            }
        }

        private fun setUpViewDividers() {
            val hideHeaderDivider = itemView.attachmentsView.visibility == View.GONE &&
                itemView.expirationInfoView.visibility == View.VISIBLE
            itemView.headerDividerView.isVisible = !hideHeaderDivider

            val showAttachmentsDivider = itemView.attachmentsView.visibility == View.VISIBLE &&
                itemView.expirationInfoView.visibility != View.VISIBLE
            itemView.attachmentsDividerView.isVisible = showAttachmentsDivider
        }

        private fun setMessageContentHeight(
            listItem: MessageDetailsListItem,
            isLastNonDraftItemInTheList: Boolean
        ) {
            when {
                !listItem.messageFormattedHtml.isNullOrEmpty() -> {
                    // We want to wrap the message content of the last non-draft item in the list only when the content
                    // has been loaded, to make the message header stay attached to the top of the list
                    // (if we wrap too quickly, the list will scroll down)
                    if (isLastNonDraftItemInTheList) {
                        wrapMessageContentHeightWhenContentLoaded(itemView.messageWebViewContainer)
                    } else {
                        wrapMessageContentHeight(itemView.messageWebViewContainer)
                    }
                }
                // For messages in the middle of a conversation, that are not initially expanded when opening
                // a conversation, we use a fixed height while loading
                !isLastNonDraftItemInTheList -> {
                    setMessageContentFixedLoadingHeight(itemView.messageWebViewContainer)
                }
            }
        }
    }

    fun showMessageDetails(
        parsedBody: String?,
        messageId: String,
        showLoadEmbeddedImagesButton: Boolean,
        showDecryptionError: Boolean,
        attachments: List<Attachment>,
        embeddedImageIds: List<String>
    ) {
        val item: MessageDetailsListItem? = visibleItems.firstOrNull {
            it.itemType == TYPE_ITEM && it.message.messageId == messageId
        }
        if (item == null) {
            Timber.d("Trying to show $messageId details but message is not in visibleItems list")
            return
        }

        Timber.d("Show message details: $messageId")
        val validParsedBody = parsedBody ?: return
        val newItem = messageToMessageDetailsListItemMapper.toMessageDetailsListItem(
            item.message,
            validParsedBody,
            showDecryptionError,
            showLoadEmbeddedImagesButton
        ).apply {
            this.message.setAttachmentList(attachments)
            this.embeddedImageIds = embeddedImageIds
            // Mark the message as read optimistically to reflect the change on the UI right away.
            // Note that this message is being referenced to from both the header and the item.
            this.message.Unread = false
        }

        visibleItems.indexOf(item).let { changedItemIndex ->
            visibleItems[changedItemIndex] = newItem
            // Update both the message and its header to ensure the "read" status is shown
            val messageHeaderIndex = changedItemIndex - 1
            notifyItemRangeChanged(messageHeaderIndex, 2)
        }
    }

    fun setMessageData(conversation: ConversationUiModel) {
        messages = conversation.messages
        exclusiveLabelsPerMessage = conversation.exclusiveLabels
        nonExclusiveLabelsPerMessage = conversation.nonExclusiveLabels
        val items = ArrayList<MessageDetailsListItem>()
        messages.forEach { message ->
            items.add(MessageDetailsListItem(message))
            items.add(MessageDetailsListItem(message, message.decryptedHTML, message.decryptedHTML))
        }

        setItems(items)
        expandLastNonDraftItem()
    }

    private fun expandLastNonDraftItem() {
        val lastNonDraftHeaderIndex = visibleItems.indexOfLast {
            !it.message.isDraft() && it.itemType == TYPE_HEADER
        }

        if (lastNonDraftHeaderIndex == ITEM_NOT_FOUND_INDEX) {
            return
        }

        if (!isExpanded(lastNonDraftHeaderIndex)) {
            toggleExpandedItems(lastNonDraftHeaderIndex, true)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpScrollListener(webView: WebView, directParent: LinearLayout) {
        val mScaleDetector = ScaleGestureDetector(
            context,
            MessageBodyScaleListener(
                messageDetailsRecyclerView,
                webView,
                directParent
            )
        )

        val scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val touchListener = MessageBodyTouchListener(messageDetailsRecyclerView, mScaleDetector, scaledTouchSlop)
        messageDetailsRecyclerView.setOnTouchListener(touchListener)
        webView.setOnTouchListener(touchListener)
    }

    private fun displayAttachmentInfo(
        attachments: List<Attachment>?,
        attachmentsView: MessageDetailsAttachmentsView
    ) {
        if (attachments == null) {
            attachmentsView.visibility = View.GONE
            return
        }
        val attachmentsCount = attachments.size
        val totalAttachmentSize = attachments.map { it.fileSize }.sum()

        val attachmentsListAdapter = MessageDetailsAttachmentListAdapter(
            context,
            onAttachmentDownloadCallback
        )
        attachmentsListAdapter.setList(attachments)
        attachmentsView.bind(attachmentsCount, totalAttachmentSize, attachmentsListAdapter)
        attachmentsView.isVisible = attachmentsCount > 0
    }

    private fun configureWebView(webView: WebView, pmWebViewClient: PMWebViewClient) {
        setUpWebViewDarkModeHandlingIfSupported(context, webView)

        webView.isScrollbarFadingEnabled = false
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        val webViewParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        webViewParams.setMargins(0, 0, 0, 0)
        webView.layoutParams = webViewParams
        webView.webViewClient = pmWebViewClient
        webView.tag = "messageWebView"
        val webSettings = webView.settings
        webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        webSettings.useWideViewPort = true
        webSettings.loadWithOverviewMode = true
        webSettings.allowFileAccess = false
        webSettings.displayZoomControls = false
        webSettings.setGeolocationEnabled(false)
        webSettings.savePassword = false
        webSettings.javaScriptEnabled = false
        webSettings.setSupportZoom(true)
        webSettings.builtInZoomControls = true
        webSettings.pluginState = WebSettings.PluginState.OFF
        webSettings.setNeedInitialFocus(false)
        webSettings.setRenderPriority(WebSettings.RenderPriority.HIGH)
        webSettings.cacheMode = WebSettings.LOAD_NO_CACHE
        webSettings.setAppCacheEnabled(false)
        webSettings.saveFormData = false
        webView.setOnLongClickListener {
            val messageBodyWebView = it as WebView
            val result = messageBodyWebView.hitTestResult
            if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                (context as Activity).openContextMenu(messageBodyWebView)
                true
            } else {
                false
            }
        }
    }

    private fun resetWebViewContent(messageWebViewContainer: LinearLayout) {
        val webView = messageWebViewContainer.findViewById<WebView>(R.id.item_message_body_web_view_id)
        webView?.loadUrl("about:blank")
    }

    private fun setMessageContentFixedLoadingHeight(messageWebViewContainer: LinearLayout) {
        val params = messageWebViewContainer.layoutParams
        params.height = messageContentFixedLoadingHeight
        messageWebViewContainer.layoutParams = params
    }

    private fun setMessageContentExpandedLoadingHeight(messageWebViewContainer: LinearLayout) {
        messageWebViewContainer.layoutParams.height = (context.resources.displayMetrics.heightPixels * 0.7).toInt()
    }

    private fun wrapMessageContentHeight(messageWebViewContainer: LinearLayout) {
        val params = messageWebViewContainer.layoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        messageWebViewContainer.layoutParams = params
    }

    private fun wrapMessageContentHeightWhenContentLoaded(messageWebViewContainer: LinearLayout) {
        messageWebViewContainer.postDelayed(WRAP_MESSAGE_CONTENT_DELAY_MS) {
            val webView = messageWebViewContainer.findViewById<WebView>(R.id.item_message_body_web_view_id)
            // We want to keep waiting until the content is actually loaded before wrapping the height to
            // avoid wrapping to quickly (this could result in the message being scrolled behind the screen instead
            // of staying in the user's view)
            if (webView.contentHeight != 0) {
                wrapMessageContentHeight(messageWebViewContainer)
            } else {
                wrapMessageContentHeightWhenContentLoaded(messageWebViewContainer)
            }
        }
    }

    private fun setUpSpamScoreView(spamScore: Int, spamScoreView: TextView) {
        val spamScoreVisibility: Int
        if (listOf(100, 101, 102).contains(spamScore)) {
            val spamScoreText = getSpamScoreText(spamScore)
            spamScoreView.setText(spamScoreText)
            Linkify.addLinks(spamScoreView, Linkify.ALL)
            spamScoreView.movementMethod = LinkMovementMethod.getInstance()
            spamScoreVisibility = View.VISIBLE
        } else {
            spamScoreVisibility = View.GONE
        }
        spamScoreView.visibility = spamScoreVisibility
    }

    private fun getSpamScoreText(spamScore: Int): Int {
        return when (spamScore) {
            100 -> R.string.spam_score_100
            101 -> R.string.spam_score_101
            102 -> R.string.spam_score_102
            else -> throw IllegalArgumentException("Unknown spam score.")
        }
    }

    private class MessageDetailsPmWebViewClient(
        userManager: UserManager,
        activity: Activity,
        private val itemView: View,
        private val isAutoShowRemoteImages: Boolean
    ) : PMWebViewClient(userManager, activity, isAutoShowRemoteImages) {

        override fun onPageFinished(view: WebView, url: String) {
            if (amountOfRemoteResourcesBlocked() > 0) {
                itemView.displayRemoteContentButton.isVisible = true
            }

            this.blockRemoteResources(!isAutoShowRemoteImages)

            super.onPageFinished(view, url)
        }
    }

    private fun shouldShowRemoteImages(): Boolean {
        val mailSettings = userManager.getCurrentUserMailSettingsBlocking()
        val isAutoShowRemoteImages = mailSettings?.showImagesFrom?.includesRemote() ?: false

        // When android API < 26 we automatically show remote images because the `getWebViewClient` method
        // that we use to access the webView and load them later on was only introduced with API 26
        return isAutoShowRemoteImages || isAndroidAPILevelLowerThan26()
    }

    private fun isAndroidAPILevelLowerThan26() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O

}
