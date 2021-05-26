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
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.body.MessageBodyScaleListener
import ch.protonmail.android.activities.messageDetails.body.MessageBodyTouchListener
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.ui.view.LabelChipUiModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.redirectToChrome
import ch.protonmail.android.utils.ui.ExpandableRecyclerAdapter
import ch.protonmail.android.views.PMWebViewClient
import ch.protonmail.android.views.messageDetails.LoadContentButton
import ch.protonmail.android.views.messageDetails.MessageDetailsAttachmentsView
import ch.protonmail.android.views.messageDetails.MessageDetailsExpirationInfoView
import ch.protonmail.android.views.messageDetails.MessageDetailsHeaderView
import kotlinx.android.synthetic.main.layout_message_details.view.*
import kotlinx.android.synthetic.main.layout_message_details_web_view.view.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import org.apache.http.protocol.HTTP.UTF_8
import timber.log.Timber
import java.util.ArrayList

// region constants
private const val TYPE_ITEM = 1001
private const val TYPE_HEADER = 1000
// endregion

internal class MessageDetailsAdapter(
    private val context: Context,
    private var messages: List<Message>,
    private val messageDetailsRecyclerView: RecyclerView,
    private var pmWebViewClient: PMWebViewClient,
    private val onLoadEmbeddedImagesCLick: (() -> Unit)?,
    private val onDisplayImagesClick: (() -> Unit)?,
    private val viewModel: MessageDetailsViewModel
) : ExpandableRecyclerAdapter<MessageDetailsAdapter.MessageDetailsListItem>(context) {

    var containerDisplayImages = LoadContentButton(context)
    var loadEmbeddedImagesContainer = LoadContentButton(context)
    var attachmentsView = MessageDetailsAttachmentsView(context)
    var attachmentsViewDivider = View(context)
    var expirationInfoView = MessageDetailsExpirationInfoView(context)
    var messageDetailsView = View(context)
    var messageDetailsHeaderView = MessageDetailsHeaderView(context)

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
                visibleItems!![position].message,
                onLoadEmbeddedImagesCLick,
                onDisplayImagesClick
            )
        } else {
            (holder as ItemViewHolder).bind(
                context,
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

        fun bind(
            position: Int,
            message: Message,
            onLoadEmbeddedImagesCLick: (() -> Unit)?,
            onDisplayImagesCLick: (() -> Unit)?
        ) {
            messageDetailsView = itemView.messageDetailsView
            messageDetailsHeaderView = itemView.headerView
            attachmentsView = itemView.attachmentsView
            attachmentsViewDivider = itemView.attachmentsDividerView
            expirationInfoView = itemView.expirationInfoView
            containerDisplayImages = itemView.containerDisplayImages
            loadEmbeddedImagesContainer = itemView.containerLoadEmbeddedImagesContainer

            messageDetailsHeaderView.bind(message, allLabelsList ?: listOf(), nonInclusiveLabelsList)
            expirationInfoView.bind(message.expirationTime)

            setUpSpamScoreView(message.spamScore, itemView.spamScoreView)

            loadEmbeddedImagesContainer.setOnClickListener {
                it.visibility = View.GONE
                onLoadEmbeddedImagesCLick?.invoke()
            }

            itemView.containerDisplayImages.setOnClickListener {
                val item = visibleItems!![position + 1]
                // isInit will prevent clicking the button before the WebView is ready.
                // WebView init can take a bit longer.
                if (item.isInit() && item.messageWebView.contentHeight > 0) {
                    itemView.containerDisplayImages.visibility = View.GONE
                    pmWebViewClient.loadRemoteResources()
                    onDisplayImagesCLick?.invoke()
                }
            }

            setUpViewDividers()
        }

        // TODO: Update this method when all the banners are added to the design
        private fun setUpViewDividers() {
            itemView.headerDividerView.visibility = if (
                itemView.attachmentsView.visibility == View.GONE &&
                itemView.expirationInfoView.visibility == View.VISIBLE
            ) {
                View.GONE
            } else {
                View.VISIBLE
            }

            itemView.attachmentsDividerView.visibility = if (itemView.attachmentsView.visibility == View.VISIBLE) {
                if (itemView.expirationInfoView.visibility == View.VISIBLE) View.GONE else View.VISIBLE
            } else View.GONE
        }
    }

    open inner class ItemViewHolder(view: View) : ExpandableRecyclerAdapter<MessageDetailsListItem>.ViewHolder(view) {

        fun bind(context: Context, position: Int, message: Message) {

            // Looks like some devices are not able to create a WebView in some conditions.
            // Show Toast and redirect to the proper page.
            val webView = try {
                WebView(context)
            } catch (ignored: Throwable) {
                (context as FragmentActivity).redirectToChrome()
                return
            }
            val progress = ProgressBar(context)
            configureWebView(webView, pmWebViewClient)
            setUpScrollListener(webView, messageDetailsView, itemView.messageWebViewContainer)

            webView.invalidate()
            (context as MessageDetailsActivity).registerForContextMenu(webView)
            itemView.messageWebViewContainer.removeAllViews()
            itemView.messageWebViewContainer.addView(webView)
            itemView.messageWebViewContainer.addView(progress)

            visibleItems!![position].messageWebView = webView

            viewModel.loadMessageBody(message).mapLatest {
                Timber.v("Load data for message: ${it.messageId} at position $position")
                val parsedBody = viewModel.getParsedMessage(
                    it.decryptedHTML,
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
                progress.visibility = View.INVISIBLE
            }.launchIn(context.lifecycleScope)

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
    private fun setUpScrollListener(webView: WebView, messageInfoView: View, directParent: LinearLayout) {
        val mScaleDetector = ScaleGestureDetector(
            context,
            MessageBodyScaleListener(
                messageDetailsRecyclerView,
                messageInfoView,
                webView,
                directParent
            )
        )

        val scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val touchListener = MessageBodyTouchListener(messageDetailsRecyclerView, mScaleDetector, scaledTouchSlop)
        messageDetailsRecyclerView.setOnTouchListener(touchListener)
        webView.setOnTouchListener(touchListener)
    }

    fun displayContainerDisplayImages(visibility: Int) {
        containerDisplayImages.visibility = visibility
    }

    fun displayLoadEmbeddedImagesContainer(visibility: Int) {
        loadEmbeddedImagesContainer.visibility = visibility
    }

    fun displayAttachmentsViews(visibility: Int) {
        attachmentsView.visibility = visibility
        attachmentsViewDivider.visibility = if (expirationInfoView.visibility == View.VISIBLE) View.GONE else visibility
    }

    fun refreshRecipientsLayout() {
        messageDetailsHeaderView.loadRecipients(messages[0])
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
}
