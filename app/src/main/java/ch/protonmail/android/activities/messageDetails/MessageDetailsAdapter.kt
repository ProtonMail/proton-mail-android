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
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.attachments.MessageDetailsAttachmentListAdapter
import ch.protonmail.android.activities.messageDetails.attachments.OnAttachmentDownloadCallback
import ch.protonmail.android.activities.messageDetails.body.MessageBodyScaleListener
import ch.protonmail.android.activities.messageDetails.body.MessageBodyTouchListener
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.permissions.PermissionHelper
import ch.protonmail.android.ui.view.LabelChipUiModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.redirectToChrome
import ch.protonmail.android.utils.ui.ExpandableRecyclerAdapter
import ch.protonmail.android.views.PMWebViewClient
import ch.protonmail.android.views.messageDetails.LoadContentButton
import ch.protonmail.android.views.messageDetails.MessageDetailsAttachmentsView
import kotlinx.android.synthetic.main.layout_message_details.view.*
import kotlinx.android.synthetic.main.layout_message_details_web_view.view.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import org.apache.http.protocol.HTTP.UTF_8
import timber.log.Timber
import java.util.ArrayList
import java.util.concurrent.atomic.AtomicReference

private const val TYPE_ITEM = 1001
private const val TYPE_HEADER = 1000

internal class MessageDetailsAdapter(
    private val context: Context,
    private var messages: List<Message>,
    private val messageDetailsRecyclerView: RecyclerView,
    private val onLoadEmbeddedImagesClicked: (() -> Unit)?,
    private val onDisplayRemoteContentClicked: ((Message) -> Unit)?,
    private val viewModel: MessageDetailsViewModel,
    private val storagePermissionHelper: PermissionHelper,
    private val attachmentToDownloadId: AtomicReference<String?>,
    private val userManager: UserManager
) : ExpandableRecyclerAdapter<MessageDetailsAdapter.MessageDetailsListItem>(context) {

    private var allLabelsList: List<Label>? = listOf()
    private var nonInclusiveLabelsList: List<LabelChipUiModel> = emptyList()

    init {
        val items = ArrayList<MessageDetailsListItem>()
        messages.forEach { message ->
            items.add(MessageDetailsListItem(message))
            items.add(MessageDetailsListItem())
        }
        setItems(items)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_HEADER) {
            (holder as HeaderViewHolder).bind(
                position,
                visibleItems!![position].message
            )
        } else {
            (holder as ItemViewHolder).bind(
                position,
                visibleItems!![position - 1].message
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
            ItemViewHolder(
                LayoutInflater.from(context).inflate(
                    R.layout.layout_message_details_web_view,
                    parent,
                    false
                )
            )
        }
    }

    class MessageDetailsListItem : ListItem {
        var message = Message()
        lateinit var messageWebView: WebView

        constructor(messageData: Message) : super(TYPE_HEADER) {
            message = messageData
        }

        constructor() : super(TYPE_ITEM)

        fun isInit(): Boolean = ::messageWebView.isInitialized
    }

    inner class HeaderViewHolder(
        view: View
    ) : ExpandableRecyclerAdapter<MessageDetailsListItem>.HeaderViewHolder(view) {

        fun bind(position: Int, message: Message) {
            val messageDetailsHeaderView = itemView.headerView
            messageDetailsHeaderView.bind(message, allLabelsList ?: listOf(), nonInclusiveLabelsList)
        }
    }

    open inner class ItemViewHolder(view: View) : ExpandableRecyclerAdapter<MessageDetailsListItem>.ViewHolder(view) {

        fun bind(position: Int, message: Message) {
            val context = context as MessageDetailsActivity
            val messageBodyProgress = ProgressBar(context)
            val attachmentsView = itemView.attachmentsView
            attachmentsView.visibility = View.GONE

            val expirationInfoView = itemView.expirationInfoView
            val displayRemoteContentButton = itemView.displayRemoteContentButton
            val loadEmbeddedImagesContainer = itemView.containerLoadEmbeddedImagesContainer

            val webView = setupMessageWebView(messageBodyProgress, position) ?: return
            expirationInfoView.bind(message.expirationTime)
            setUpSpamScoreView(message.spamScore, itemView.spamScoreView)

            viewModel.loadMessageBody(message).mapLatest { loadedMessage ->
                Timber.v("Load data for message: ${loadedMessage.messageId} at position $position")

                val parsedBody = viewModel.getParsedMessage(
                    loadedMessage.decryptedHTML,
                    UiUtil.getRenderWidth(context.windowManager),
                    AppUtil.readTxt(context, R.raw.editor),
                    context.getString(R.string.request_timeout)
                )

                webView.loadDataWithBaseURL(
                    Constants.DUMMY_URL_PREFIX,
                    parsedBody!!,
                    "text/html",
                    UTF_8,
                    ""
                )
                messageBodyProgress.visibility = View.INVISIBLE

                displayAttachmentInfo(loadedMessage.attachments, attachmentsView)
                showLoadEmbeddedImagesButton(loadEmbeddedImagesContainer, message)
                setUpViewDividers()
            }.launchIn(context.lifecycleScope)

            setupMessageContentActions(position, loadEmbeddedImagesContainer, displayRemoteContentButton)
        }

        private fun setupMessageContentActions(
            position: Int,
            loadEmbeddedImagesContainer: LoadContentButton,
            displayRemoteContentButton: LoadContentButton
        ) {
            loadEmbeddedImagesContainer.setOnClickListener {
                it.visibility = View.GONE
                onLoadEmbeddedImagesClicked?.invoke()
            }

            displayRemoteContentButton.setOnClickListener {
                val item = visibleItems!![position]
                // isInit will prevent clicking the button before the WebView is ready.
                // WebView init can take a bit longer.
                if (item.isInit() && item.messageWebView.contentHeight > 0) {
                    itemView.displayRemoteContentButton.visibility = View.GONE
                    (item.messageWebView.webViewClient as MessageDetailsPmWebViewClient).allowLoadingRemoteResources()
                    item.messageWebView.reload()
                    onDisplayRemoteContentClicked?.invoke(item.message)
                }
            }
        }

        private fun setupMessageWebView(
            messageBodyProgress: ProgressBar,
            position: Int
        ): WebView? {
            val context = context as MessageDetailsActivity
            // Looks like some devices are not able to create a WebView in some conditions.
            // Show Toast and redirect to the proper page.
            val webView = try {
                WebView(context)
            } catch (ignored: Throwable) {
                (context as FragmentActivity).redirectToChrome()
                return null
            }

            val webViewClient = MessageDetailsPmWebViewClient(userManager, context, itemView)
            configureWebView(webView, webViewClient)
            setUpScrollListener(webView, itemView.messageWebViewContainer)

            webView.invalidate()
            context.registerForContextMenu(webView)
            itemView.messageWebViewContainer.removeAllViews()
            itemView.messageWebViewContainer.addView(webView)
            itemView.messageWebViewContainer.addView(messageBodyProgress)

            visibleItems!![position].messageWebView = webView
            return webView
        }

        private fun setUpViewDividers() {
            val hideHeaderDivider = itemView.attachmentsView.visibility == View.GONE
                && itemView.expirationInfoView.visibility == View.VISIBLE
            itemView.headerDividerView.isVisible = !hideHeaderDivider

            val showAttachmentsDivider = itemView.attachmentsView.visibility == View.VISIBLE
                && itemView.expirationInfoView.visibility != View.VISIBLE
            itemView.attachmentsDividerView.isVisible = showAttachmentsDivider
        }
    }

    fun setMessageData(messageData: List<Message>) {
        messages = messageData
        val items = ArrayList<MessageDetailsListItem>()
        messages.forEach { message ->
            items.add(MessageDetailsListItem(message))
            items.add(MessageDetailsListItem())
        }
        setItems(items)
    }

    fun setAllLabels(labels: List<Label>) {
        allLabelsList = labels
        setMessageData(messages)
    }

    fun setNonInclusiveLabels(labels: List<LabelChipUiModel>) {
        nonInclusiveLabelsList = labels
        setMessageData(messages)
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

    private fun showLoadEmbeddedImagesButton(loadEmbeddedImagesContainer: LoadContentButton, message: Message) {
        val hasEmbeddedImages = viewModel.prepareEmbeddedImages(message)
        if (!hasEmbeddedImages) {
            loadEmbeddedImagesContainer.isVisible = false
            return
        }

    private fun showLoadEmbeddedImagesButton() {
        val hasEmbeddedImages = viewModel.prepareEmbeddedImages()
        if (hasEmbeddedImages) {
            if (viewModel.isAutoShowEmbeddedImages() || viewModel.isEmbeddedImagesDisplayed()) {
                viewModel.displayEmbeddedImages()
                displayLoadEmbeddedImagesContainer(View.GONE)
            } else {
                displayLoadEmbeddedImagesContainer(View.VISIBLE)
            }
        } else {
            displayLoadEmbeddedImagesContainer(View.GONE)
        }
    }

    private fun displayAttachmentInfo(attachments: List<Attachment>?, attachmentsView: MessageDetailsAttachmentsView) {
        if (attachments == null) {
            attachmentsView.visibility = View.GONE
            return
        }
        val attachmentsCount = attachments.size
        val totalAttachmentSize = attachments.map { it.fileSize }.sum()

        val attachmentsListAdapter = MessageDetailsAttachmentListAdapter(
            context,
            OnAttachmentDownloadCallback(storagePermissionHelper, attachmentToDownloadId)
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
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        webViewParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
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

    private inner class MessageDetailsPmWebViewClient(
        userManager: UserManager,
        activity: Activity,
        private val itemView: View
    ) : PMWebViewClient(userManager, activity, false) {

        override fun onPageFinished(view: WebView, url: String) {
            if (amountOfRemoteResourcesBlocked() > 0) {
                itemView.displayRemoteContentButton.isVisible = true
            }

            this.blockRemoteResources(!isAutoShowRemoteImages())

            super.onPageFinished(view, url)
        }

        private fun isAutoShowRemoteImages(): Boolean {
            val mailSettings = userManager.getCurrentUserMailSettingsBlocking()
            return mailSettings?.showImagesFrom?.includesRemote() ?: false
        }
    }
}
