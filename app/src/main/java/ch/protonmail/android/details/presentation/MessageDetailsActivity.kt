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
package ch.protonmail.android.details.presentation

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.MenuItem
import android.view.View
import android.view.animation.AlphaAnimation
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseStoragePermissionActivity
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
import ch.protonmail.android.activities.messageDetails.MessageDetailsAdapter
import ch.protonmail.android.activities.messageDetails.details.OnStarToggleListener
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.DownloadedAttachmentEvent
import ch.protonmail.android.events.PostPhishingReportEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.jobs.PostSpamJob
import ch.protonmail.android.jobs.ReportPhishingJob
import ch.protonmail.android.ui.actionsheet.MessageActionSheet
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.CustomLocale
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.MODE_ACCORDION
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import ch.protonmail.android.views.messageDetails.BottomActionsView
import com.google.android.material.appbar.AppBarLayout
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_message_details.*
import kotlinx.android.synthetic.main.layout_message_details_activity_toolbar.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs

private const val TITLE_ANIMATION_THRESHOLD = 0.9
private const val TITLE_ANIMATION_DURATION = 200L
private const val ONE_HUNDRED_PERCENT = 1.0

@AndroidEntryPoint
internal class MessageDetailsActivity : BaseStoragePermissionActivity() {

    private lateinit var messageOrConversationId: String
    private lateinit var messageExpandableAdapter: MessageDetailsAdapter
    private lateinit var primaryBaseActivity: Context

    private var messageRecipientUserId: Id? = null
    private var messageRecipientUsername: String? = null
    private val attachmentToDownloadId = AtomicReference<String?>(null)
    private var showPhishingReportButton = true
    private var shouldTitleFadeOut = false
    private var shouldTitleFadeIn = true
    private val viewModel: MessageDetailsViewModel by viewModels()

    /** Lazy instance of [ClipboardManager] that will be used for copy content into the Clipboard */
    private val clipboardManager by lazy { getSystemService<ClipboardManager>() }

    private val onOffsetChangedListener = AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
        val scrolledPercentage = abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange.toFloat()
        // Animate collapsed title
        if (scrolledPercentage >= TITLE_ANIMATION_THRESHOLD && shouldTitleFadeIn) {
            startTitleAlphaAnimation(collapsedToolbarTitleTextView, View.VISIBLE)
            collapsedToolbarTitleTextView.visibility = View.VISIBLE
            shouldTitleFadeIn = false
            shouldTitleFadeOut = true
        } else if (scrolledPercentage < TITLE_ANIMATION_THRESHOLD && shouldTitleFadeOut) {
            startTitleAlphaAnimation(collapsedToolbarTitleTextView, View.INVISIBLE)
            collapsedToolbarTitleTextView.visibility = View.INVISIBLE
            shouldTitleFadeIn = true
            shouldTitleFadeOut = false
        }
        // Animate expanded title
        if (scrolledPercentage < ONE_HUNDRED_PERCENT) {
            expandedToolbarTitleTextView.visibility = View.VISIBLE
            expandedToolbarTitleTextView.alpha = 1 - scrolledPercentage
        } else {
            expandedToolbarTitleTextView.visibility = View.INVISIBLE
        }
    }

    private fun startTitleAlphaAnimation(view: View, visibility: Int) {
        val alphaAnimation = if (visibility == View.VISIBLE) AlphaAnimation(0f, 1f) else AlphaAnimation(1f, 0f)
        alphaAnimation.duration = TITLE_ANIMATION_DURATION
        alphaAnimation.fillAfter = true
        view.startAnimation(alphaAnimation)
    }

    override fun getLayoutId(): Int = R.layout.activity_message_details

    override fun storagePermissionGranted() {
        val attachmentToDownloadIdAux = attachmentToDownloadId.getAndSet(null)
        if (!attachmentToDownloadIdAux.isNullOrEmpty()) {
            viewModel.viewOrDownloadAttachment(this, attachmentToDownloadIdAux)
        }
    }

    override fun checkForPermissionOnStartup(): Boolean = false

    override fun attachBaseContext(base: Context) {
        primaryBaseActivity = base
        super.attachBaseContext(CustomLocale.apply(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageOrConversationId = requireNotNull(intent.getStringExtra(EXTRA_MESSAGE_OR_CONVERSATION_ID))
        messageRecipientUserId = intent.getStringExtra(EXTRA_MESSAGE_RECIPIENT_USER_ID)?.let(::Id)
        messageRecipientUsername = intent.getStringExtra(EXTRA_MESSAGE_RECIPIENT_USERNAME)
        val currentUser = mUserManager.requireCurrentUser()
        AppUtil.clearNotifications(this, currentUser.id)
        supportActionBar?.title = null
        initAdapters()
        val recipientUsername = messageRecipientUsername
        if (recipientUsername != null && currentUser.name.s != recipientUsername) {
            val userId = checkNotNull(messageRecipientUserId) { "Username found in extras, but user id" }
            accountStateManager.switch(userId).invokeOnCompletion {
                continueSetup()
                invalidateOptionsMenu()
            }
        } else {
            continueSetup()
        }

        // Copy Subject to Clipboard at long press
        expandedToolbarTitleTextView.setOnLongClickListener {
            clipboardManager?.let {
                it.setPrimaryClip(
                    ClipData.newPlainText(getString(R.string.email_subject), expandedToolbarTitleTextView.text)
                )
                showToast(R.string.subject_copied, Toast.LENGTH_SHORT)
                true
            } ?: false
        }

        appBarLayout.addOnOffsetChangedListener(onOffsetChangedListener)

        starToggleButton.setOnCheckedChangeListener(OnStarToggleListener(mJobManager, messageOrConversationId))
    }

    private fun continueSetup() {
        viewModel.conversationUiModel.observe(this) { viewModel.loadMailboxItemDetails() }
        viewModel.decryptedConversationUiModel.observe(this, ConversationUiModelObserver())

        viewModel.labels
            .onEach(messageExpandableAdapter::setAllLabels)
            .launchIn(lifecycleScope)
        viewModel.nonExclusiveLabelsUiModels
            .onEach(messageExpandableAdapter::setNonInclusiveLabels)
            .launchIn(lifecycleScope)

        viewModel.messageDetailsError.observe(this, MessageDetailsErrorObserver())
        listenForConnectivityEvent()
        observeEditMessageEvents()
    }

    private fun initAdapters() {
        messageExpandableAdapter = MessageDetailsAdapter(
            this,
            listOf(),
            messageDetailsRecyclerView,
            { onLoadEmbeddedImagesClicked() },
            onDisplayRemoteContentClicked(),
            storagePermissionHelper,
            attachmentToDownloadId,
            mUserManager,
            onLoadMessageBody()
        )
    }

    private fun onLoadMessageBody() = { message: Message ->
        if (message.messageId != null) {
            viewModel.loadMessageBody(message).mapLatest { loadedMessage ->

                val parsedBody = viewModel.getParsedMessage(
                    requireNotNull(loadedMessage.decryptedHTML),
                    UiUtil.getRenderWidth(this.windowManager),
                    AppUtil.readTxt(this, R.raw.editor),
                    this.getString(R.string.request_timeout)
                )

                val messageId = loadedMessage.messageId ?: return@mapLatest
                messageExpandableAdapter.showMessageBody(
                    parsedBody,
                    messageId,
                    shouldShowLoadEmbeddedImagesButton(message)
                )
            }.launchIn(lifecycleScope)
        }
    }

    private fun shouldShowLoadEmbeddedImagesButton(message: Message): Boolean {
        val hasEmbeddedImages = viewModel.prepareEmbeddedImages(message)
        if (!hasEmbeddedImages) {
            return false
        }

        val displayEmbeddedImages = viewModel.isAutoShowEmbeddedImages() || viewModel.isEmbeddedImagesDisplayed()
        if (displayEmbeddedImages) {
            viewModel.displayEmbeddedImages()
        }
        return !displayEmbeddedImages
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val webView = v as WebView
        val result = webView.hitTestResult ?: return
        val type = result.type
        if (listOf(HitTestResult.UNKNOWN_TYPE, HitTestResult.EDIT_TEXT_TYPE).contains(type)) {
            return
        }
        if (listOf(HitTestResult.EMAIL_TYPE, HitTestResult.SRC_ANCHOR_TYPE).contains(type)) {
            menu?.add(getString(R.string.copy_link))?.setOnMenuItemClickListener(Copy(result.extra))
            menu?.add(getString(R.string.share_link))?.setOnMenuItemClickListener(Share(result.extra))
        }
    }

    override fun onStart() {
        super.onStart()
        checkDelinquency()
        mApp.bus.register(this)
        viewModel.checkConnectivity()
        mApp.bus.register(viewModel)
    }

    override fun onStop() {
        super.onStop()
        mApp.bus.unregister(viewModel)
        mApp.bus.unregister(this)
    }

    override fun secureContent(): Boolean = true

    override fun enableScreenshotProtector() {
        screenShotPreventerView.visibility = View.VISIBLE
    }

    override fun disableScreenshotProtector() {
        screenShotPreventerView.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                return true
            }
        }
        return true
    }

    fun showReportPhishingDialog(message: Message? = viewModel.decryptedConversationUiModel.value?.messages?.last()) {
        AlertDialog.Builder(this)
            .setTitle(R.string.phishing_dialog_title)
            .setMessage(R.string.phishing_dialog_message)
            .setPositiveButton(R.string.send) { _: DialogInterface?, _: Int ->
                showPhishingReportButton = false
                mJobManager.addJobInBackground(ReportPhishingJob(message))
            }
            .setNegativeButton(R.string.cancel, null).show()
    }

    private fun showNoConnSnackExtended(connectivity: Constants.ConnectionState) {
        Timber.v("Show no connection")
        networkSnackBarUtil.hideAllSnackBars()
        networkSnackBarUtil.getNoConnectionSnackBar(
            mSnackLayout,
            mUserManager.requireCurrentLegacyUser(),
            this,
            { onConnectivityCheckRetry() },
            anchorViewId = R.id.messageDetailsActionsView,
            isOffline = connectivity == Constants.ConnectionState.NO_INTERNET
        ).show()
    }

    private fun onConnectivityCheckRetry() {
        viewModel.loadMailboxItemDetails()
        networkSnackBarUtil.getCheckingConnectionSnackBar(
            mSnackLayout,
            R.id.messageDetailsActionsView
        ).show()

        viewModel.checkConnectivityDelayed()
    }

    private fun hideNoConnSnackExtended() {
        networkSnackBarUtil.hideNoConnectionSnackBar()
    }

    private fun listenForConnectivityEvent() {
        viewModel.hasConnectivity.observe(
            this,
            { isConnectionActive ->
                Timber.v("isConnectionActive:${isConnectionActive.name}")
                if (isConnectionActive == Constants.ConnectionState.CONNECTED) {
                    hideNoConnSnackExtended()
                } else {
                    showNoConnSnackExtended(isConnectionActive)
                }
            }
        )
    }

    @Subscribe
    @Suppress("unused")
    fun onPostPhishingReportEvent(event: PostPhishingReportEvent) {
        val status = event.status
        val toastMessageId: Int
        when (status) {
            Status.SUCCESS -> {
                mJobManager.addJobInBackground(PostSpamJob(listOf(messageOrConversationId)))
                toastMessageId = R.string.phishing_report_send_message_moved_to_spam
                finish()
            }
            Status.STARTED,
            Status.FAILED,
            Status.NO_NETWORK,
            Status.UNAUTHORIZED -> {
                showPhishingReportButton = true
                toastMessageId = R.string.cannot_send_report_send
            }
            else -> throw IllegalStateException("Unknown message status: $status")
        }
        showToast(toastMessageId, Toast.LENGTH_SHORT)
    }

    private fun getDecryptedBody(decryptedHtml: String?): String {
        var decryptedBody = decryptedHtml
        if (decryptedBody.isNullOrEmpty()) {
            decryptedBody = getString(R.string.empty_message)
        }
        val regex2 = "<body[^>]*>"
        // break the backgrounds and other urls
        decryptedBody = decryptedBody!!.replace(regex2.toRegex(), "<body>")
        return decryptedBody
    }

    override fun onBackPressed() {
        saveLastInteraction()
        finish()
    }

    @Subscribe
    @Suppress("unused")
    fun onDownloadEmbeddedImagesEvent(event: DownloadEmbeddedImagesEvent) {
        when (event.status) {
            Status.SUCCESS -> {
                viewModel.onEmbeddedImagesDownloaded(event)
            }
            Status.NO_NETWORK -> {
                showToast(R.string.load_embedded_images_failed_no_network)
            }
            Status.FAILED -> {
                showToast(R.string.load_embedded_images_failed)
            }
            Status.STARTED -> {
                viewModel.hasEmbeddedImages = false
            }
            Status.UNAUTHORIZED -> {
                // NOOP, when on enums should be exhaustive
            }
            else -> {
                // NOOP, when on enums should be exhaustive
            }
        }
    }

    @Subscribe
    @Suppress("unused")
    fun onDownloadAttachmentEvent(event: DownloadedAttachmentEvent) {
        when (val status = event.status) {
            Status.STARTED, Status.SUCCESS -> {
                val isDownloaded = Status.SUCCESS == status
                if (isDownloaded) {
                    viewModel.viewAttachment(this, event.filename, event.attachmentUri)
                } else {
                    showToast(R.string.downloading)
                }
            }
            Status.FAILED -> showToast(R.string.cant_download_attachment)
            Status.NO_NETWORK,
            Status.UNAUTHORIZED -> {
                // NOOP, when on enums should be exhaustive
            }
        }
    }


    private inner class Copy(private val text: CharSequence?) : MenuItem.OnMenuItemClickListener {

        override fun onMenuItemClick(item: MenuItem): Boolean {
            UiUtil.copy(this@MessageDetailsActivity, text)
            return true
        }
    }

    private inner class Share(private val uri: String?) : MenuItem.OnMenuItemClickListener {

        override fun onMenuItemClick(item: MenuItem): Boolean {
            val send = Intent(Intent.ACTION_SEND)
            send.type = "text/plain"
            send.putExtra(Intent.EXTRA_TEXT, uri)
            try {
                startActivity(Intent.createChooser(send, getText(R.string.share_link)))
            } catch (ex: ActivityNotFoundException) {
                // if no app handles it, do nothing
                Timber.e(ex)
            }
            return true
        }
    }

    private inner class ConversationUiModelObserver : Observer<ConversationUiModel> {

        override fun onChanged(conversation: ConversationUiModel) {
            val message = lastMessage(conversation)

            displayToolbarData(conversation)
            setupLastMessageActionsListener(message)

            Timber.v("New decrypted message ${message.messageId}")
            viewModel.renderedFromCache = AtomicBoolean(true)
            val decryptedBody = getDecryptedBody(message.decryptedHTML)
            if (decryptedBody.isEmpty() || message.messageBody.isNullOrEmpty()) {
                UiUtil.showInfoSnack(mSnackLayout, this@MessageDetailsActivity, R.string.decryption_error_desc).show()
                return
            }

            Timber.v("setMessage conversations size: ${conversation.messages.size}")
            messageExpandableAdapter.setMessageData(conversation.messages)
            if (viewModel.refreshedKeys) {
                if (isAutoShowRemoteImages) {
                    viewModel.remoteContentDisplayed()
                }
                messageExpandableAdapter.mode = MODE_ACCORDION
                messageDetailsRecyclerView.layoutManager = LinearLayoutManager(this@MessageDetailsActivity)
                messageDetailsRecyclerView.adapter = messageExpandableAdapter
            }
            viewModel.triggerVerificationKeyLoading()

            progress.visibility = View.GONE
            invalidateOptionsMenu()
            viewModel.renderingPassed = true
        }
    }

    private fun setupLastMessageActionsListener(message: Message) {
        messageDetailsActionsView.setOnMoreActionClickListener {
            MessageActionSheet.newInstance(
                MessageActionSheet.ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID,
                listOf(message.messageId ?: messageOrConversationId),
                message.location,
                getCurrentSubject(),
                getMessagesFrom(message.sender?.name),
                message.isStarred ?: false
            )
                .show(supportFragmentManager, MessageActionSheet::class.qualifiedName)
        }

        val actionsUiModel = BottomActionsView.UiModel(
            if (message.toList.size + message.ccList.size > 1) R.drawable.ic_reply_all else R.drawable.ic_reply,
            R.drawable.ic_envelope_dot,
            R.drawable.ic_trash
        )
        messageDetailsActionsView.bind(actionsUiModel)
        messageDetailsActionsView.setOnThirdActionClickListener {
            viewModel.moveToTrash()
            onBackPressed()
        }
        messageDetailsActionsView.setOnSecondActionClickListener {
            viewModel.markUnread()
            onBackPressed()
        }
        messageDetailsActionsView.setOnFirstActionClickListener {
            val messageAction = if (message.toList.size + message.ccList.size > 1) {
                Constants.MessageActionType.REPLY_ALL
            } else {
                Constants.MessageActionType.REPLY
            }
            executeMessageAction(messageAction, message)
        }

    }

    private fun displayToolbarData(conversation: ConversationUiModel) {
        starToggleButton.isChecked = conversation.isStarred
        val isInvalidSubject = conversation.subject.isNullOrEmpty()
        val subject = if (isInvalidSubject) getString(R.string.empty_subject) else conversation.subject
        val messagesInConversation = conversation.messages.count()
        toolbarMessagesCountTextView.text = resources.getQuantityString(
            R.plurals.x_messages_count,
            messagesInConversation,
            messagesInConversation
        )
        toolbarMessagesCountTextView.isVisible = messagesInConversation > 1
        collapsedToolbarTitleTextView.text = subject
        collapsedToolbarTitleTextView.visibility = View.INVISIBLE
        expandedToolbarTitleTextView.text = subject
    }

    private fun lastMessage(conversation: ConversationUiModel): Message = conversation.messages.last()

    fun executeMessageAction(
        messageAction: Constants.MessageActionType,
        message: Message = requireNotNull(viewModel.decryptedConversationUiModel.value?.messages?.last())
    ) {
        try {
            val user = mUserManager.requireCurrentLegacyUser()
            val userUsedSpace = user.usedSpace
            val userMaxSpace = if (user.maxSpace == 0L) {
                Long.MAX_VALUE
            } else {
                user.maxSpace
            }
            val percentageUsed = userUsedSpace * 100 / userMaxSpace
            if (percentageUsed >= 100) {
                this@MessageDetailsActivity.showTwoButtonInfoDialog(
                    title = getString(R.string.storage_limit_warning_title),
                    message = getString(R.string.storage_limit_reached_text),
                    rightStringId = R.string.okay,
                    leftStringId = R.string.learn_more,
                    onNegativeButtonClicked = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.limit_reached_learn_more))
                        )
                        startActivity(browserIntent)
                    }
                )
            } else {
                val newMessageTitle = MessageUtils.buildNewMessageTitle(
                    this@MessageDetailsActivity,
                    messageAction,
                    message.subject
                )
                viewModel.prepareEditMessageIntent(
                    messageAction,
                    message,
                    newMessageTitle,
                    getDecryptedBody(message.decryptedHTML),
                    mBigContentHolder
                )
            }
        } catch (exc: Exception) {
            Timber.e(exc, "Exception in reply/forward actions")
        }
    }

    private fun observeEditMessageEvents() {
        viewModel.prepareEditMessageIntent.observe(
            this@MessageDetailsActivity,
            Observer { editIntentExtrasEvent: Event<IntentExtrasData?> ->
                val editIntentExtras = editIntentExtrasEvent.getContentIfNotHandled()
                    ?: return@Observer
                val intent = AppUtil.decorInAppIntent(
                    Intent(
                        this@MessageDetailsActivity,
                        ComposeMessageActivity::class.java
                    )
                )
                MessageUtils.addRecipientsToIntent(
                    intent,
                    ComposeMessageActivity.EXTRA_TO_RECIPIENTS,
                    editIntentExtras.toRecipientListString,
                    editIntentExtras.messageAction,
                    editIntentExtras.userAddresses
                )
                if (editIntentExtras.includeCCList) {
                    MessageUtils.addRecipientsToIntent(
                        intent,
                        ComposeMessageActivity.EXTRA_CC_RECIPIENTS,
                        editIntentExtras.messageCcList,
                        editIntentExtras.messageAction,
                        editIntentExtras.userAddresses
                    )
                }
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_LOAD_IMAGES,
                    editIntentExtras.imagesDisplayed
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_LOAD_REMOTE_CONTENT,
                    editIntentExtras.remoteContentDisplayed
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_SENDER_NAME,
                    editIntentExtras.messageSenderName
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_SENDER_ADDRESS,
                    editIntentExtras.senderEmailAddress
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_PGP_MIME,
                    editIntentExtras.isPGPMime
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_MESSAGE_TITLE,
                    editIntentExtras.newMessageTitle
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_MESSAGE_BODY_LARGE,
                    editIntentExtras.largeMessageBody
                )
                mBigContentHolder.content = editIntentExtras.mBigContentHolder.content
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_MESSAGE_BODY,
                    editIntentExtras.body
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_MESSAGE_TIMESTAMP,
                    editIntentExtras.timeMs
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_MESSAGE_ENCRYPTED,
                    editIntentExtras.messageIsEncrypted
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_PARENT_ID,
                    editIntentExtras.messageId
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_ACTION_ID,
                    editIntentExtras.messageAction
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID,
                    editIntentExtras.addressID
                )
                intent.putExtra(
                    ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_EMAIL_ALIAS,
                    editIntentExtras.addressEmailAlias
                )
                if (editIntentExtras.embeddedImagesAttachmentsExist) {
                    intent.putParcelableArrayListExtra(
                        ComposeMessageActivity.EXTRA_MESSAGE_EMBEDDED_ATTACHMENTS,
                        editIntentExtras.attachments
                    )
                }
                val attachments = editIntentExtras.attachments
                if (attachments.size > 0) {
                    intent.putParcelableArrayListExtra(
                        ComposeMessageActivity.EXTRA_MESSAGE_ATTACHMENTS,
                        attachments
                    )
                }
                startActivityForResult(intent, 0)
            }
        )
    }

    private fun getCurrentSubject() = expandedToolbarTitleTextView.text ?: getString(R.string.empty_subject)

    private fun getMessagesFrom(messageOriginator: String?): String =
        messageOriginator?.let { resources.getString(R.string.message_from, messageOriginator) } ?: EMPTY_STRING

    private inner class MessageDetailsErrorObserver : Observer<Event<String>> {

        override fun onChanged(status: Event<String>?) {
            if (status != null) {
                val content = status.getContentIfNotHandled()
                if (content.isNullOrEmpty()) {
                    showToast(R.string.default_error_message)
                } else {
                    showToast(content)
                    progress.visibility = View.GONE
                }
            }
        }
    }

    private fun onLoadEmbeddedImagesClicked() {
        // this will ensure that the message has been loaded
        // and will protect from premature clicking on download attachments button
        if (viewModel.renderingPassed) {
            viewModel.startDownloadEmbeddedImagesJob()
        }
        return
    }

    private fun onDisplayRemoteContentClicked() = { message: Message ->
        viewModel.displayRemoteContent(message)
        viewModel.checkStoragePermission.observe(this, { storagePermissionHelper.checkPermission() })
    }

    fun printMessage() {
        viewModel.printMessage(primaryBaseActivity)
    }

    companion object {

        const val EXTRA_MESSAGE_OR_CONVERSATION_ID = "messageOrConversationId"
        const val EXTRA_MESSAGE_LOCATION_ID = "messageOrConversationLocation"

        const val EXTRA_MESSAGE_RECIPIENT_USER_ID = "message_recipient_user_id"
        const val EXTRA_MESSAGE_RECIPIENT_USERNAME = "message_recipient_username"
    }
}