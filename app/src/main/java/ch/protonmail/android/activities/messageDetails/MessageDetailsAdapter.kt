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
import android.util.TypedValue
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
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.domain.MessageBodyParser
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.details.presentation.MessageDetailsListItem
import ch.protonmail.android.details.presentation.view.MessageDetailsActionsView
import ch.protonmail.android.ui.view.LabelChipUiModel
import ch.protonmail.android.utils.redirectToChrome
import ch.protonmail.android.utils.ui.ExpandableRecyclerAdapter
import ch.protonmail.android.utils.ui.TYPE_HEADER
import ch.protonmail.android.utils.ui.TYPE_ITEM
import ch.protonmail.android.views.PMWebViewClient
import ch.protonmail.android.views.messageDetails.MessageDetailsAttachmentsView
import ch.protonmail.android.views.messageDetails.MessageDetailsHeaderView
import kotlinx.android.synthetic.main.layout_message_details.view.*
import kotlinx.android.synthetic.main.layout_message_details_web_view.view.*
import org.apache.http.protocol.HTTP
import timber.log.Timber
import java.util.ArrayList

/**
 * Used to force the "message content" webview to have a fixed size while animating (expanding  / collapsing)
 * a message in a conversation. This is needed to ensure a smooth animation
 */
private const val MESSAGE_CONTENT_FIXED_SIZE_DP = 200F
private const val EXPAND_MESSAGE_ANIMATION_DELAY_MS = 200L

internal class MessageDetailsAdapter(
    private val context: Context,
    private var messages: List<Message>,
    private val messageDetailsRecyclerView: RecyclerView,
    private val messageBodyParser: MessageBodyParser,
    private val userManager: UserManager,
    private val onLoadEmbeddedImagesClicked: (Message) -> Unit,
    private val onDisplayRemoteContentClicked: (Message) -> Unit,
    private val onLoadMessageBody: (Message) -> Unit,
    private val onAttachmentDownloadCallback: (Attachment) -> Unit,
    private val onEditDraftClicked: (Message) -> Unit,
    private val onReplyMessageClicked: (Message) -> Unit,
    private val onMoreMessageActionsClicked: (Message) -> Unit
) : ExpandableRecyclerAdapter<MessageDetailsListItem>(context) {

    private var exclusiveLabelsPerMessage: HashMap<String, List<Label>> = hashMapOf()
    private var nonExclusiveLabelsPerMessage: HashMap<String, List<LabelChipUiModel>> = hashMapOf()

    private val constrainedMessageHeightPx by lazy {
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            MESSAGE_CONTENT_FIXED_SIZE_DP,
            context.resources.displayMetrics
        ).toInt()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_HEADER) {
            (holder as HeaderViewHolder).bind(
                visibleItems!![position].message
            )
        } else {
            (holder as ItemViewHolder).bind(
                position,
                visibleItems!![position]
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

            ItemViewHolder(itemView)
        }
    }

    private fun createMessageBodyProgressBar(): ProgressBar {
        val messageBodyProgress = ProgressBar(context)
        messageBodyProgress.id = R.id.item_message_body_progress_view_id
        return messageBodyProgress
    }

    private fun showingMoreThanOneMessage() = messages.size > 1

    private fun createInMessageActionsView(): MessageDetailsActionsView {
        val detailsMessageActions = MessageDetailsActionsView(context)
        detailsMessageActions.id = R.id.item_message_body_actions_layout_id
        return detailsMessageActions
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

        val webViewClient = MessageDetailsPmWebViewClient(userManager, context, itemView)
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
            messageDetailsHeaderView.bind(
                message,
                exclusiveLabelsPerMessage[message.messageId] ?: listOf(),
                nonExclusiveLabelsPerMessage[message.messageId] ?: listOf()
            )

            messageDetailsHeaderView.setOnClickListener { view ->
                val headerView = view as MessageDetailsHeaderView

                if (isMessageBodyExpanded()) {
                    // Message Body is expended - will collapse
                    headerView.collapseHeader()
                    headerView.forbidExpandingHeaderView()
                } else {
                    // Message Body is collapsed - will expand
                    headerView.allowExpandingHeaderView()
                }
                toggleExpandedItems(layoutPosition, false)
            }

            if (isLastItemHeader()) {
                messageDetailsHeaderView.allowExpandingHeaderView()
            }
        }

        private fun isMessageBodyExpanded() = isExpanded(layoutPosition)

        private fun isLastItemHeader(): Boolean {
            val lastHeaderItem = visibleItems?.last { it.itemType == TYPE_HEADER }
            return layoutPosition == visibleItems?.indexOf(lastHeaderItem)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        holder.itemView.messageWebViewContainer?.let {
            constrainMessageContentHeight(it)
            resetWebViewContent(it)
        }
    }

    inner class ItemViewHolder(view: View) : ExpandableRecyclerAdapter<MessageDetailsListItem>.ViewHolder(view) {

        fun bind(position: Int, listItem: MessageDetailsListItem) {
            val message = listItem.message
            Timber.v("Bind item: ${message.messageId}")
            val attachmentsView = itemView.attachmentsView
            attachmentsView.visibility = View.GONE

            val expirationInfoView = itemView.expirationInfoView
            val displayRemoteContentButton = itemView.displayRemoteContentButton
            val loadEmbeddedImagesButton = itemView.loadEmbeddedImagesButton
            val editDraftButton = itemView.editDraftButton
            val webView = itemView.messageWebViewContainer
                .findViewById<WebView>(R.id.item_message_body_web_view_id) ?: return
            val messageBodyProgress = itemView.messageWebViewContainer
                .findViewById<ProgressBar>(R.id.item_message_body_progress_view_id) ?: return

            messageBodyProgress.isVisible = listItem.messageFormattedHtml.isNullOrEmpty()
            editDraftButton.isVisible = message.isDraft()
            expirationInfoView.bind(message.expirationTime)
            setUpSpamScoreView(message.spamScore, itemView.spamScoreView)

            Timber.v("Load data for message: ${message.messageId} at position $position")
            if (listItem.messageFormattedHtml == null) {
                onLoadMessageBody(message)
            }

            val htmlContent = if (showingMoreThanOneMessage() || messageHasNoQuotedPart(listItem)) {
                listItem.messageFormattedHtml
            } else {
                listItem.messageFormattedHtmlWithQuotedHistory
            }
            loadHtmlDataIntoWebView(webView, htmlContent)

            displayAttachmentInfo(listItem.message.attachments, attachmentsView)
            loadEmbeddedImagesButton.isVisible = listItem.showLoadEmbeddedImagesButton
            setUpViewDividers()

            setupMessageActionsView(message, listItem.messageFormattedHtmlWithQuotedHistory, webView)
            setupMessageContentActions(position, loadEmbeddedImagesButton, displayRemoteContentButton, editDraftButton)

            itemView.messageWebViewContainer.postDelayed(EXPAND_MESSAGE_ANIMATION_DELAY_MS) {
                expandMessageContentHeight(itemView.messageWebViewContainer)
            }
        }

        private fun messageHasNoQuotedPart(listItem: MessageDetailsListItem) =
            listItem.messageFormattedHtmlWithQuotedHistory == null

        private fun setupMessageActionsView(
            message: Message,
            messageHtmlWithQuotedHistory: String?,
            webView: WebView
        ) {
            val messageActionsView: MessageDetailsActionsView =
                itemView.messageWebViewContainer.findViewById(R.id.item_message_body_actions_layout_id) ?: return

            val replyMode = if (message.toList.size + message.ccList.size > 1) {
                MessageDetailsActionsView.ReplyMode.REPLY_ALL
            } else {
                MessageDetailsActionsView.ReplyMode.REPLY
            }
            val uiModel = MessageDetailsActionsView.UiModel(
                replyMode,
                messageHtmlWithQuotedHistory.isNullOrEmpty(),
                message.isDraft()
            )
            messageActionsView.bind(uiModel)

            messageActionsView.onShowHistoryClicked { showHistoryButton ->
                loadHtmlDataIntoWebView(webView, messageHtmlWithQuotedHistory)
                showHistoryButton.isVisible = false
            }
            messageActionsView.onReplyClicked { onReplyMessageClicked(message) }
            messageActionsView.onMoreActionsClicked { onMoreMessageActionsClicked(message) }
        }

        private fun loadHtmlDataIntoWebView(webView: WebView, htmlContent: String?) {
            webView.loadDataWithBaseURL(
                Constants.DUMMY_URL_PREFIX,
                htmlContent ?: "",
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
                    it.messageFormattedHtml = null
                }
                val item = visibleItems!![position]
                onLoadEmbeddedImagesClicked(item.message)
            }

            displayRemoteContentButton.setOnClickListener {
                val item = visibleItems!![position]
                val webView = itemView.messageWebViewContainer.getChildAt(0) as? WebView
                // isInit will prevent clicking the button before the WebView is ready.
                // WebView init can take a bit longer.
                if (webView != null && webView.contentHeight > 0) {
                    itemView.displayRemoteContentButton.visibility = View.GONE
                    (webView.webViewClient as MessageDetailsPmWebViewClient).allowLoadingRemoteResources()
                    webView.reload()
                    onDisplayRemoteContentClicked(item.message)
                }
            }

            editDraftButton.setOnClickListener {
                val item = visibleItems!![position]
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
    }

    fun showMessageDetails(
        parsedBody: String?,
        messageId: String,
        showLoadEmbeddedImagesButton: Boolean,
        attachments: List<Attachment>
    ) {
        val item: MessageDetailsListItem? = visibleItems?.firstOrNull {
            it.itemType == TYPE_ITEM && it.message.messageId == messageId
        }
        if (item == null) {
            Timber.d("Trying to show $messageId details but message is not in visibleItems list")
            return
        }

        val validParsedBody = parsedBody ?: return
        val messageBodyParts = messageBodyParser.splitBody(validParsedBody)
        item.messageFormattedHtml = messageBodyParts.messageBody
        item.messageFormattedHtmlWithQuotedHistory = messageBodyParts.messageBodyWithQuote
        item.showLoadEmbeddedImagesButton = showLoadEmbeddedImagesButton
        item.message.setAttachmentList(attachments)

        visibleItems?.indexOf(item)?.let { changedItemIndex ->
            notifyItemChanged(changedItemIndex, item)
        }
    }

    fun setMessageData(messageData: List<Message>) {
        Timber.v("setMessageData size: ${messageData.size} ")
        messages = messageData
        val items = ArrayList<MessageDetailsListItem>()
        messages.forEach { message ->
            items.add(MessageDetailsListItem(message))
            items.add(MessageDetailsListItem(message, message.decryptedHTML, message.decryptedHTML))
        }
        setItems(items)
    }

    fun setExclusiveLabelsPerMessage(labels: HashMap<String, List<Label>>) {
        exclusiveLabelsPerMessage = labels
    }

    fun setNonExclusiveLabelsPerMessage(labels: HashMap<String, List<LabelChipUiModel>>) {
        nonExclusiveLabelsPerMessage = labels
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

    private fun constrainMessageContentHeight(messageWebViewContainer: LinearLayout) {
        val params = messageWebViewContainer.layoutParams
        params.height = constrainedMessageHeightPx
        messageWebViewContainer.layoutParams = params
    }

    private fun expandMessageContentHeight(messageWebViewContainer: LinearLayout) {
        val params = messageWebViewContainer.layoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        messageWebViewContainer.layoutParams = params
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
        private val userManager: UserManager,
        activity: Activity,
        private val itemView: View
    ) : PMWebViewClient(userManager, activity, false) {

        override fun onPageFinished(view: WebView, url: String) {
            // Do not display the 'displayRemoteContent' button when API is lower than 26 as in that case remote
            // images will be loaded automatically for the reason mentioned below
            if (amountOfRemoteResourcesBlocked() > 0 && !isAndroidAPILevelLowerThan26()) {
                itemView.displayRemoteContentButton.isVisible = true
            }

            // When android API < 26 we automatically show remote images because the `getWebViewClient` method
            // that we use to access the webView and load them later on was only introduced with API 26
            val showRemoteImages = isAutoShowRemoteImages() || isAndroidAPILevelLowerThan26()
            this.blockRemoteResources(!showRemoteImages)

            super.onPageFinished(view, url)
        }

        private fun isAutoShowRemoteImages(): Boolean {
            val mailSettings = userManager.getCurrentUserMailSettingsBlocking()
            return mailSettings?.showImagesFrom?.includesRemote() ?: false
        }

        private fun isAndroidAPILevelLowerThan26() = Build.VERSION.SDK_INT < Build.VERSION_CODES.O
    }
}
