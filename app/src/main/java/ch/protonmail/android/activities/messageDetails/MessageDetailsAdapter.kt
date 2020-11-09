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
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Typeface
import android.os.Build
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
import android.widget.Toast
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.body.MessageBodyScaleListener
import ch.protonmail.android.activities.messageDetails.body.MessageBodyTouchListener
import ch.protonmail.android.activities.messageDetails.details.OnDetailsCheckChangeListener
import ch.protonmail.android.activities.messageDetails.details.OnStarToggleListener
import ch.protonmail.android.activities.messageDetails.details.RecipientContextMenuFactory
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.DateUtil
import ch.protonmail.android.utils.ServerTime
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.redirectToChrome
import ch.protonmail.android.utils.ui.ExpandableRecyclerAdapter
import ch.protonmail.android.views.PMWebView
import ch.protonmail.android.views.PMWebViewClient
import ch.protonmail.android.views.messageDetails.AttachmentsView
import ch.protonmail.android.views.messageDetails.LoadContentButton
import ch.protonmail.android.views.messageDetails.MessageDetailsRecipientsLayout
import com.birbit.android.jobqueue.JobManager
import com.google.android.material.chip.ChipGroup
import kotlinx.android.synthetic.main.message_details_header_item.view.*
import kotlinx.android.synthetic.main.message_details_item.view.*
import kotlinx.android.synthetic.main.view_attachments_message_details.view.*
import me.proton.core.util.kotlin.takeIfNotEmpty
import org.apache.http.protocol.HTTP.UTF_8
import java.util.ArrayList
import java.util.concurrent.TimeUnit

// region constants
private const val TYPE_ITEM = 1001
private const val TYPE_HEADER = 1000
// endregion

class MessageDetailsAdapter(
        private val context: Context,
        private var mJobManager: JobManager,
        private var message: Message,
        private var content: String,
        private val wvScrollView: RecyclerView,
        private var pmWebViewClient: PMWebViewClient,
        private val onLoadEmbeddedImagesCLick: (() -> Unit)?,
        private val onDisplayImagesCLick: (() -> Unit)?
): ExpandableRecyclerAdapter<MessageDetailsAdapter.MessageDetailsListItem>(context) {

    /** Lazy instance of [ClipboardManager] that will be used for copy content into the Clipboard */
    private val clipboardManager by lazy { context.getSystemService<ClipboardManager>() }

    var containerDisplayImages = LoadContentButton(context)
    var loadEmbeddedImagesContainer = LoadContentButton(context)
    var embeddedImagesDownloadProgress = ProgressBar(context)
    var attachmentsContainer = AttachmentsView(context)
    var attachmentIcon = TextView(context)
    var attachmentsViewDivider = View(context)
    var labels = ChipGroup(context)
    var messageInfoView = View(context)
    var recipientsLayout = MessageDetailsRecipientsLayout(context)

    init {
        val items = ArrayList<MessageDetailsListItem>()
        items.add(MessageDetailsListItem(message))
        items.add(MessageDetailsListItem(content))
        setItems(items)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_HEADER -> {
                (holder as HeaderViewHolder).bind(context, position, visibleItems!![position].message, onLoadEmbeddedImagesCLick, onDisplayImagesCLick)
            }
            else -> {
                (holder as ItemViewHolder).bind(context, position, visibleItems!![position - 1].message)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                HeaderViewHolder(
                        LayoutInflater.from(context).inflate(
                                R.layout.message_details_header_item,
                                parent,
                                false
                        ))
            }
            else -> {
                ItemViewHolder(
                        LayoutInflater.from(context).inflate(
                                R.layout.message_details_item,
                                parent,
                                false))
            }
        }
    }

    class MessageDetailsListItem : ListItem {
        var message = Message()
        var content = ""
        lateinit var messageWebView: WebView

        constructor(messageData: Message) : super(TYPE_HEADER) {
            message = messageData
        }

        constructor(contentData: String) : super(TYPE_ITEM) {
            content = contentData
        }

        fun isInit(): Boolean {
            return ::messageWebView.isInitialized
        }
    }

    inner class HeaderViewHolder(view: View) : ExpandableRecyclerAdapter<MessageDetailsListItem>.HeaderViewHolder(view) {

        fun bind(
                context: Context,
                position: Int,
                message: Message,
                onLoadEmbeddedImagesCLick: (() -> Unit)?,
                onDisplayImagesCLick: (() -> Unit)?) {
            val typeface = Typeface.createFromAsset(context.assets, "protonmail-mobile-icons.ttf")

            messageInfoView = itemView.messageInfoView

            itemView.attachmentIcon.typeface = typeface
            itemView.messageStarView.typeface = typeface
            itemView.messageReplyTextView.typeface = typeface
            itemView.messageReplyAllTextView.typeface = typeface
            itemView.messageForwardTextView.typeface = typeface

            containerDisplayImages = itemView.containerDisplayImages
            loadEmbeddedImagesContainer = itemView.containerLoadEmbeddedImagesContainer
            embeddedImagesDownloadProgress = itemView.attachmentsContainer.embeddedImagesDownloadProgress
            attachmentsContainer = itemView.attachmentsContainer
            attachmentIcon = itemView.attachmentIcon
            attachmentsViewDivider = itemView.attachmentsViewDivider
            recipientsLayout = itemView.recipientsLayout

            itemView.detailsToggleButton.setOnCheckedChangeListener(OnDetailsCheckChangeListener(itemView.recipientsLayout, itemView.messageTitle))
            itemView.messageStarView.setOnCheckedChangeListener(OnStarToggleListener(mJobManager, message.messageId!!))

            val subject = message.subject
            itemView.messageTitle.text = subject

            val isMessageReplied = message.isReplied
            val isMessageRepliedAll = message.isRepliedAll
            val isMessageForwarded = message.isForwarded
            if (isMessageReplied != null && isMessageRepliedAll != null) {
                itemView.messageReplyTextView.visibility = if (isMessageReplied && !isMessageRepliedAll) View.VISIBLE else View.GONE
            }
            if (isMessageRepliedAll != null) {
                itemView.messageReplyAllTextView.visibility = if (isMessageRepliedAll) View.VISIBLE else View.GONE
            }
            if (isMessageForwarded != null) {
                itemView.messageForwardTextView.visibility = if (isMessageForwarded) View.VISIBLE else View.GONE
            }

            recipientsLayout.bind(message, RecipientContextMenuFactory(context as MessageDetailsActivity))

            val simpleDateText = DateUtil.formatDateTime(context, message.timeMs)
            itemView.shortDate.text = simpleDateText


            val starred = message.isStarred != null && message.isStarred!!
            itemView.messageStarView.isChecked = starred

            itemView.messageInfoView.visibility = View.VISIBLE
            itemView.labels.visibility = View.VISIBLE

            val expirationTime = message.expirationTime
            val expirationTimerVisibility: Int
            val dividerVisibility: Int
            if (expirationTime > 0) {
                val remainingSeconds = expirationTime - TimeUnit.MILLISECONDS.toSeconds(ServerTime.currentTimeMillis())
                itemView.expirationTimeView.remainingSeconds = remainingSeconds
                expirationTimerVisibility = View.VISIBLE
                dividerVisibility = View.GONE
            } else {
                expirationTimerVisibility = View.GONE
                dividerVisibility = View.VISIBLE
                itemView.expirationTimeView.visibility = View.GONE
            }
            itemView.divider.visibility = dividerVisibility
            itemView.expirationTimeView.visibility = expirationTimerVisibility

            val spamScore = message.spamScore
            setUpSpamScoreView(spamScore, itemView.spamScoreView)

            labels = itemView.labels

            // Copy Subject to Clipboard at long press
            itemView.messageTitle.setOnLongClickListener {
                clipboardManager?.let {
                    it.setPrimaryClip(
                        ClipData.newPlainText(context.getString(R.string.email_subject), itemView.messageTitle.text)
                    )
                    context.showToast(R.string.subject_copied, Toast.LENGTH_SHORT)
                    true
                } ?: false
            }

            itemView.containerLoadEmbeddedImagesContainer.setOnClickListener {
                itemView.containerLoadEmbeddedImagesContainer.visibility = View.GONE
                itemView.embeddedImagesDownloadProgress.visibility = View.VISIBLE
                onLoadEmbeddedImagesCLick?.invoke()
            }

            itemView.containerDisplayImages.setOnClickListener {

                val item = visibleItems!![position + 1]
                if (item.isInit() && item.messageWebView.contentHeight > 0) { // isInit will prevent clicking the button before the webview is ready. Webview init can take a bit longer.
                    itemView.containerDisplayImages.visibility = View.GONE
                    pmWebViewClient.loadRemoteResources()
                    onDisplayImagesCLick?.invoke()
                }
            }
        }
    }

    open inner class ItemViewHolder(view: View) : ExpandableRecyclerAdapter<MessageDetailsListItem>.ViewHolder(view) {

        fun bind(context: Context, position: Int, message: Message) {

            // Looks like some devices are not able to create a WebView in some conditions.
            // Show Toast and redirect to the proper page.
            val webView = try {
                PMWebView(context)
            } catch (ignored: Throwable) {
                (context as FragmentActivity).redirectToChrome()
                return
            }
            configureWebView(webView, pmWebViewClient)
            setUpScrollListener(webView, messageInfoView, itemView.messageWebViewContainer)

            webView.invalidate()
            (context as MessageDetailsActivity).registerForContextMenu(webView)
            itemView.messageWebViewContainer.removeAllViews()
            itemView.messageWebViewContainer.addView(webView)

            visibleItems!![position].messageWebView = webView

            webView.loadDataWithBaseURL(
                Constants.DUMMY_URL_PREFIX,
                content.takeIfNotEmpty() ?: message.decryptedHTML!!,
                "text/html",
                UTF_8,
                ""
            )
        }
    }

    fun setMessageData(messageData: Message) {
        message = messageData
        val items = ArrayList<MessageDetailsListItem>()
        items.add(MessageDetailsListItem(messageData))
        items.add(MessageDetailsListItem(content))
        setItems(items)
    }

    /**
     * Update the [WebView] content
     * @param contentData [String] representation of the HTML page to be displayed in the [WebView]
     */
    fun loadDataFromUrlToMessageView(contentData: String) {
        content = contentData
        val contentItem = visibleItems?.find {
            it.ItemType == TYPE_ITEM
        } ?: MessageDetailsListItem(contentData)
        val messageItem = visibleItems?.find {
            it.ItemType == TYPE_HEADER
        } ?: MessageDetailsListItem(message)

        contentItem.content = contentData
        if (contentItem.isInit()) {
            contentItem.messageWebView.loadDataWithBaseURL(
                Constants.DUMMY_URL_PREFIX,
                if (content.isEmpty()) message.decryptedHTML!! else content,
                "text/html",
                UTF_8,
                ""
            )
        } else {
            setItems(arrayListOf(messageItem, contentItem))
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpScrollListener(webView: WebView, messageInfoView: View, directParent: LinearLayout) {
        val mScaleDetector = ScaleGestureDetector(context,
                MessageBodyScaleListener(wvScrollView, messageInfoView, webView, directParent))

        val scaledTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val touchListener = MessageBodyTouchListener(wvScrollView, mScaleDetector, scaledTouchSlop)
        wvScrollView.setOnTouchListener(touchListener)
        webView.setOnTouchListener(touchListener)
    }

    fun displayContainerDisplayImages(visibility: Int) {
        containerDisplayImages.visibility = visibility
    }

    fun displayEmbeddedImagesDownloadProgress(visibility: Int) {
        embeddedImagesDownloadProgress.visibility = visibility
    }

    fun displayLoadEmbeddedImagesContainer(visibility: Int) {
        loadEmbeddedImagesContainer.visibility = visibility
    }

    fun displayAttachmentsViews(visibility: Int) {
        attachmentsContainer.visibility = visibility
        attachmentIcon.visibility = visibility
        attachmentsViewDivider.visibility = visibility
    }

    fun refreshRecipientsLayout() {
        recipientsLayout.bind(message, RecipientContextMenuFactory(context as MessageDetailsActivity))
    }

    private fun configureWebView(webView: WebView, pmWebViewClient: PMWebViewClient) {
        webView.isScrollbarFadingEnabled = false
        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false
        val webViewParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        webViewParams.height = LinearLayout.LayoutParams.WRAP_CONTENT
        webViewParams.setMargins(0, 0, 0, 0)
        webView.layoutParams = webViewParams
        webView.webViewClient = pmWebViewClient
        val webSettings = webView.settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING
        } else {
            webSettings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN
        }
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
        webView.setOnLongClickListener(MessageBodyOnLongClickListener())
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
            else -> throw RuntimeException("Unknown spam score.")
        }
    }

    private inner class MessageBodyOnLongClickListener : View.OnLongClickListener {

        override fun onLongClick(v: View): Boolean {
            val messageBodyWebView = v as WebView
            val result = messageBodyWebView.hitTestResult
            if (result.type == WebView.HitTestResult.SRC_ANCHOR_TYPE) {
                (context as Activity).openContextMenu(messageBodyWebView)
                return true
            }
            return false
        }
    }
}
