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
package ch.protonmail.android.details.presentation.ui

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
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseStoragePermissionActivity
import ch.protonmail.android.activities.StartCompose
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.messageDetails.IntentExtrasData
import ch.protonmail.android.activities.messageDetails.MessageDetailsAdapter
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.mapper.MessageEncryptionUiModelMapper
import ch.protonmail.android.details.presentation.mapper.MessageToMessageDetailsListItemMapper
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.details.presentation.model.MessageBodyState
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.DownloadedAttachmentEvent
import ch.protonmail.android.events.PostPhishingReportEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.feature.rating.ShowReviewAppInMemoryRepository
import ch.protonmail.android.jobs.PostSpamJob
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import ch.protonmail.android.settings.data.AccountSettingsRepository
import ch.protonmail.android.ui.actionsheet.MessageActionSheet
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import ch.protonmail.android.util.ProtonCalendarUtil
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showDeleteConfirmationDialog
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import ch.protonmail.android.utils.ui.screen.RenderDimensionsProvider
import ch.protonmail.android.utils.webview.SetUpWebViewDarkModeHandlingIfSupported
import ch.protonmail.android.views.messageDetails.BottomActionsView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_message_details.*
import kotlinx.android.synthetic.main.layout_message_details_activity_toolbar.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import kotlin.math.abs

private const val TITLE_ANIMATION_THRESHOLD = 0.9
private const val TITLE_ANIMATION_DURATION = 200L
private const val ONE_HUNDRED_PERCENT = 1.0

@AndroidEntryPoint
internal class MessageDetailsActivity : BaseStoragePermissionActivity() {

    @Inject
    lateinit var messageToMessageDetailsListItemMapper: MessageToMessageDetailsListItemMapper

    @Inject
    lateinit var messageEncryptionUiModelMapper: MessageEncryptionUiModelMapper

    @Inject
    lateinit var renderDimensionsProvider: RenderDimensionsProvider

    @Inject
    lateinit var setUpWebViewDarkModeHandlingIfSupported: SetUpWebViewDarkModeHandlingIfSupported

    @Inject
    lateinit var protonCalendarUtil: ProtonCalendarUtil

    @Inject
    lateinit var accountSettingsRepository: AccountSettingsRepository

    @Inject
    lateinit var showReviewAppRepository: ShowReviewAppInMemoryRepository

    private lateinit var messageOrConversationId: String
    private lateinit var messageExpandableAdapter: MessageDetailsAdapter
    private lateinit var primaryBaseActivity: Context

    private var messageRecipientUserId: UserId? = null
    private var messageRecipientUsername: String? = null
    private var openedFolderLocationId: Int = Constants.MessageLocationType.INVALID.messageLocationTypeValue
    private var openedFolderLabelId: String? = null
    private var onOffsetChangedListener: AppBarLayout.OnOffsetChangedListener? = null
    private var showPhishingReportButton = true
    private var shouldScrollToPosition = true

    private val attachmentToDownload = AtomicReference<Attachment?>(null)
    private val viewModel: MessageDetailsViewModel by viewModels()

    /** Lazy instance of [ClipboardManager] that will be used for copy content into the Clipboard */
    private val clipboardManager by lazy { getSystemService<ClipboardManager>() }

    private val recyclerViewLinearLayoutManager = LinearLayoutManager(this)

    private val startComposeLauncher = registerForActivityResult(StartCompose()) { messageId ->
        messageId?.let {
            val snack = Snackbar.make(
                findViewById(R.id.messageDetailsView),
                R.string.snackbar_message_draft_saved,
                Snackbar.LENGTH_LONG
            )
            snack.setAction(R.string.move_to_trash) {
                viewModel.moveDraftToTrash(messageId)
                Snackbar.make(
                    findViewById(R.id.messageDetailsView),
                    R.string.snackbar_message_draft_moved_to_trash,
                    Snackbar.LENGTH_LONG
                ).show()
            }
            snack.show()
        }
    }

    override fun getLayoutId(): Int = R.layout.activity_message_details

    override fun storagePermissionGranted() {
        val attachmentToDownload = attachmentToDownload.getAndSet(null)
        if (attachmentToDownload?.attachmentId?.isNotEmpty() == true) {
            viewModel.viewOrDownloadAttachment(this, attachmentToDownload)
        }
    }

    override fun checkForPermissionOnStartup(): Boolean = false

    override fun attachBaseContext(base: Context) {
        primaryBaseActivity = base
        super.attachBaseContext(base)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        messageOrConversationId = requireNotNull(intent.getStringExtra(EXTRA_MESSAGE_OR_CONVERSATION_ID))
        messageRecipientUserId = intent.getStringExtra(EXTRA_MESSAGE_RECIPIENT_USER_ID)?.let(::UserId)
        messageRecipientUsername = intent.getStringExtra(EXTRA_MESSAGE_RECIPIENT_USERNAME)
        openedFolderLocationId = intent.getIntExtra(
            EXTRA_MESSAGE_LOCATION_ID,
            Constants.MessageLocationType.INVALID.messageLocationTypeValue
        )
        openedFolderLabelId = intent.getStringExtra(EXTRA_MAILBOX_LABEL_ID)
        expandedToolbarTitleTextView.text = intent.getStringExtra(EXTRA_MESSAGE_SUBJECT) ?: ""
        val currentUser = mUserManager.requireCurrentUser()
        supportActionBar?.title = null
        initAdapters()
        initRecyclerView()
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

        starToggleButton.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!buttonView.isPressed) {
                return@setOnCheckedChangeListener
            }
            viewModel.handleStarUnStar(messageOrConversationId, isChecked)
        }

        viewModel.reloadMessageFlow
            .onEach { messageId ->
                messageExpandableAdapter.reloadMessage(messageId)
            }
            .launchIn(lifecycleScope)

        lifecycle.addObserver(viewModel)
    }

    private fun continueSetup() {
        viewModel.decryptedConversationUiModel.observe(this, ConversationUiModelObserver())
        viewModel.messageRenderedWithImages.observe(this) { message ->
            val messageId = message.messageId ?: return@observe
            messageExpandableAdapter.showMessageDetails(
                message.decryptedHTML,
                messageId,
                false,
                false,
                message.attachments,
                message.embeddedImageIds,
                hasValidSignature = message.hasValidSignature,
                hasInvalidSignature = message.hasInvalidSignature,
            )
        }

        viewModel.messageDetailsError.observe(this, MessageDetailsErrorObserver())
        listenForConnectivityEvent()
        observeEditMessageEvents()
        observePermissionMissingDialogTrigger()
    }

    private fun initAdapters() {
        messageExpandableAdapter = MessageDetailsAdapter(
            context = this,
            messages = emptyList(),
            messageDetailsRecyclerView = messageDetailsRecyclerView,
            messageToMessageDetailsListItemMapper = messageToMessageDetailsListItemMapper,
            userManager = mUserManager,
            accountSettingsRepository = accountSettingsRepository,
            messageEncryptionUiModelMapper = messageEncryptionUiModelMapper,
            setUpWebViewDarkModeHandlingIfSupported = setUpWebViewDarkModeHandlingIfSupported,
            onLoadEmbeddedImagesClicked = ::onLoadEmbeddedImagesClicked,
            onDisplayRemoteContentClicked = ::onDisplayRemoteContentClicked,
            protonCalendarUtil = protonCalendarUtil,
            onLoadMessageBody = ::onLoadMessageBody,
            onAttachmentDownloadCallback = ::onDownloadAttachment,
            onOpenInProtonCalendarClicked = { viewModel.openInProtonCalendar(this, it) },
            onEditDraftClicked = ::onEditDraftClicked,
            onReplyMessageClicked = ::onReplyMessageClicked,
            onMoreMessageActionsClicked = ::onShowMessageActionSheet
        )
    }

    private fun initRecyclerView() {
        messageDetailsRecyclerView.layoutManager = recyclerViewLinearLayoutManager
        messageDetailsRecyclerView.adapter = messageExpandableAdapter
    }

    private fun onLoadMessageBody(message: Message) {
        if (message.messageId != null) {
            viewModel.loadMessageBody(message).mapLatest { messageBodyState ->

                val showDecryptionError = messageBodyState is MessageBodyState.Error.DecryptionError
                val loadedMessage = messageBodyState.message
                val messageId = loadedMessage.messageId ?: return@mapLatest
                val parsedBody = viewModel.formatMessageHtmlBody(
                    loadedMessage,
                    renderDimensionsProvider.getRenderWidth(this),
                    AppUtil.readTxt(this, R.raw.css_reset_with_custom_props),
                    if (viewModel.isAppInDarkMode(this) &&
                        viewModel.isWebViewInDarkModeBlocking(this, messageId)
                    ) {
                        AppUtil.readTxt(this, R.raw.css_reset_dark_mode_only)
                    } else {
                        EMPTY_STRING
                    },
                    this.getString(R.string.request_timeout)
                )

                val showLoadEmbeddedImagesButton = handleEmbeddedImagesLoading(loadedMessage)
                messageExpandableAdapter.showMessageDetails(
                    parsedBody,
                    messageId,
                    showLoadEmbeddedImagesButton,
                    showDecryptionError,
                    loadedMessage.attachments,
                    loadedMessage.embeddedImageIds,
                    hasValidSignature = loadedMessage.hasValidSignature,
                    hasInvalidSignature = loadedMessage.hasInvalidSignature,
                )
            }.launchIn(lifecycleScope)
        }
    }

    private fun handleEmbeddedImagesLoading(message: Message): Boolean {
        val hasEmbeddedImages = viewModel.prepareEmbeddedImages(message)
        if (!hasEmbeddedImages) {
            // Let client know the 'load images' button should not be shown
            return false
        }

        val displayEmbeddedImages = viewModel.isAutoShowEmbeddedImages() || viewModel.isEmbeddedImagesDisplayed()
        if (displayEmbeddedImages) {
            viewModel.displayEmbeddedImages(message)
        }
        // Let client know whether the 'load images' button should be shown
        return !displayEmbeddedImages
    }

    override fun onCreateContextMenu(menu: ContextMenu?, v: View?, menuInfo: ContextMenuInfo?) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val webView = v as WebView
        val result = webView.hitTestResult
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

    override fun onBackPressed() {
        showReviewAppRepository.recordMailboxScreenView()
        super.onBackPressed()
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

    override fun onPermissionDenied(type: Constants.PermissionType) {
        super.onPermissionDenied(type)
        viewModel.storagePermissionDenied()
    }

    fun showReportPhishingDialog(messageId: String) {
        val message = viewModel.decryptedConversationUiModel.value?.messages?.find { it.messageId == messageId }
        AlertDialog.Builder(this)
            .setTitle(R.string.phishing_dialog_title)
            .setMessage(R.string.phishing_dialog_message)
            .setPositiveButton(R.string.send) { _: DialogInterface?, _: Int ->
                showPhishingReportButton = false
                if (message != null) {
                    viewModel.sendPhishingReport(message, mJobManager)
                } else {
                    showToast(R.string.cannot_send_report_send, Toast.LENGTH_SHORT)
                }
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
        val constraintSet = ConstraintSet()
        constraintSet.clone(messageDetailsView)
        constraintSet.connect(
            R.id.coordinatorLayout, ConstraintSet.BOTTOM, R.id.layout_no_connectivity_info, ConstraintSet.TOP, 0
        )
        constraintSet.applyTo(messageDetailsView)
    }

    private fun onConnectivityCheckRetry() {
        networkSnackBarUtil.getCheckingConnectionSnackBar(
            mSnackLayout,
            R.id.messageDetailsActionsView
        ).show()

        viewModel.checkConnectivityDelayed()
    }

    private fun hideNoConnSnackExtended() {
        networkSnackBarUtil.hideNoConnectionSnackBar()
        val constraintSet = ConstraintSet()
        constraintSet.clone(messageDetailsView)
        constraintSet.connect(
            R.id.coordinatorLayout, ConstraintSet.BOTTOM, R.id.messageDetailsActionsView, ConstraintSet.TOP, 0
        )
        constraintSet.applyTo(messageDetailsView)
    }

    private fun listenForConnectivityEvent() {
        viewModel.hasConnectivity.observe(
            this,
            { isConnectionActive ->
                Timber.v("isConnectionActive:${isConnectionActive.name}")
                if (isConnectionActive != Constants.ConnectionState.CONNECTED) {
                    showNoConnSnackExtended(isConnectionActive)
                } else {
                    hideNoConnSnackExtended()
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
        decryptedBody = decryptedBody.replace(regex2.toRegex(), "<body>")
        return decryptedBody
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
                    viewModel.viewAttachment(event.attachmentId, event.filename, event.attachmentUri)
                } else {
                    showToast(R.string.downloading)
                }
            }
            Status.FAILED, Status.VALIDATION_FAILED -> showToast(R.string.cant_download_attachment)
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
            val lastNonDraftMessage = conversation.messages.lastOrNull { it.isDraft().not() }
            if (lastNonDraftMessage != null) {
                setupLastMessageActionsListener(lastNonDraftMessage)
            } else {
                messageDetailsActionsView.isVisible = false
            }

            setupToolbarOffsetListener(conversation.messages.count())
            displayToolbarData(conversation)

            Timber.v("setMessage conversations size: ${conversation.messages.size}")
            messageExpandableAdapter.setMessageData(conversation)
            if (isAutoShowRemoteImages) {
                viewModel.remoteContentDisplayed()
            }

            progress.visibility = View.GONE
            invalidateOptionsMenu()

            // Scroll to the last message if there is more than one message,
            // i.e. the item count is greater than 2 (header and body)
            if (shouldScrollToPosition && messageExpandableAdapter.itemCount > 2) {
                scrollToTheExpandedMessage(lastNonDraftMessage)
                shouldScrollToPosition = false
            }
            viewModel.renderingPassed = true
        }
    }

    private fun setupToolbarOffsetListener(messagesCount: Int) {
        // Ensure we do only set onOffsetChangedListener once
        if (onOffsetChangedListener != null) {
            return
        }

        var areCollapsedViewsShown = false

        onOffsetChangedListener = AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            val scrolledPercentage = abs(verticalOffset).toFloat() / appBarLayout.totalScrollRange.toFloat()

            val collapsedViewsAnimation = if (areCollapsedViewsShown) AlphaAnimation(1f, 0f) else AlphaAnimation(0f, 1f)
            collapsedViewsAnimation.duration = TITLE_ANIMATION_DURATION
            collapsedViewsAnimation.fillAfter = true

            // Animate collapsed toolbar views
            val shouldDisplayCollapsedViews = scrolledPercentage >= TITLE_ANIMATION_THRESHOLD && !areCollapsedViewsShown
            val shouldHideCollapsedViews = scrolledPercentage < TITLE_ANIMATION_THRESHOLD && areCollapsedViewsShown

            if (shouldDisplayCollapsedViews) {
                collapsedToolbarTitleTextView.startAnimation(collapsedViewsAnimation)
                collapsedToolbarMessagesCountTextView.startAnimation(collapsedViewsAnimation)

                collapsedToolbarTitleTextView.visibility = View.VISIBLE
                collapsedToolbarMessagesCountTextView.isVisible = messagesCount > 1
                areCollapsedViewsShown = true
            } else if (shouldHideCollapsedViews) {
                collapsedToolbarTitleTextView.startAnimation(collapsedViewsAnimation)
                collapsedToolbarMessagesCountTextView.startAnimation(collapsedViewsAnimation)

                collapsedToolbarTitleTextView.visibility = View.INVISIBLE
                collapsedToolbarMessagesCountTextView.isVisible = false
                areCollapsedViewsShown = false
            }

            // Animate expanded toolbar views
            if (scrolledPercentage < ONE_HUNDRED_PERCENT) {
                expandedToolbarTitleTextView.visibility = View.VISIBLE
                expandedToolbarMessagesCountTextView.isVisible = messagesCount > 1

                expandedToolbarTitleTextView.alpha = 1 - scrolledPercentage
                expandedToolbarMessagesCountTextView.alpha = 1 - scrolledPercentage
            } else {
                expandedToolbarTitleTextView.visibility = View.INVISIBLE
                expandedToolbarMessagesCountTextView.isVisible = false
            }

            ViewCompat.setElevation(appBarLayout, resources.getDimensionPixelSize(R.dimen.elevation_m).toFloat())
        }

        appBarLayout.addOnOffsetChangedListener(onOffsetChangedListener)
    }

    private fun setupLastMessageActionsListener(message: Message) {
        val actionSheetTarget =
            if (viewModel.isConversationEnabled() && viewModel.doesConversationHaveMoreThanOneMessage()) {
                ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
            } else {
                ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN
            }
        val id = if (viewModel.isConversationEnabled() && viewModel.doesConversationHaveMoreThanOneMessage()) {
            messageOrConversationId
        } else {
            message.messageId ?: messageOrConversationId
        }
        messageDetailsActionsView.setOnMoreActionClickListener {
            MessageActionSheet.newInstance(
                actionSheetTarget,
                listOf(id),
                openedFolderLocationId,
                openedFolderLabelId ?: openedFolderLocationId.toString(),
                getCurrentSubject(),
                getMessagesFrom(message.sender?.name),
                message.isStarred ?: false,
                message.isScheduled,
                viewModel.doesConversationHaveMoreThanOneMessage()
            )
                .show(supportFragmentManager, MessageActionSheet::class.qualifiedName)
        }

        val hasMultipleRecipients = message.toList.size + message.ccList.size > 1

        val actionsUiModel = BottomActionsView.UiModel(
            null,
            R.drawable.ic_proton_envelope_dot,
            if (viewModel.shouldShowDeleteActionInBottomActionBar()) R.drawable.ic_proton_trash_cross else R.drawable.ic_proton_trash,
            R.drawable.ic_proton_tag
        )
        messageDetailsActionsView.bind(actionsUiModel)
        messageDetailsActionsView.setAction(
            BottomActionsView.ActionPosition.ACTION_FIRST, !message.isScheduled,
            if (hasMultipleRecipients) R.drawable.ic_proton_arrows_up_and_left else R.drawable.ic_proton_arrow_up_and_left,
            if (hasMultipleRecipients) getString(R.string.reply_all) else getString(R.string.reply)
        )
        messageDetailsActionsView.setAction(
            BottomActionsView.ActionPosition.ACTION_THIRD, !message.isScheduled,
            if (viewModel.shouldShowDeleteActionInBottomActionBar()) R.drawable.ic_proton_trash_cross else R.drawable.ic_proton_trash,
            if (viewModel.shouldShowDeleteActionInBottomActionBar()) getString(R.string.delete) else getString(
                R.string.trash
            )
        )
        messageDetailsActionsView.setOnFourthActionClickListener {
            showLabelsActionSheet(LabelType.MESSAGE_LABEL)
        }
        messageDetailsActionsView.setOnThirdActionClickListener {
            if (viewModel.shouldShowDeleteActionInBottomActionBar()) {
                showDeleteConfirmationDialog(
                    this,
                    getString(R.string.delete_messages),
                    getString(R.string.confirm_destructive_action)
                ) {
                    // Cancel observing the message/conversation in order for it not to be fetched again
                    // after it has been deleted from DB optimistically
                    viewModel.cancelConversationFlowJob()
                    viewModel.delete()
                    onBackPressed()
                }
            } else {
                moveToTrash(message.isScheduled)
            }
        }
        messageDetailsActionsView.setOnSecondActionClickListener {
            onBackPressed()
            viewModel.markUnread()
        }
        messageDetailsActionsView.setOnFirstActionClickListener {
            executeMessageAction(
                if (hasMultipleRecipients) Constants.MessageActionType.REPLY_ALL else Constants.MessageActionType.REPLY,
                messageOrConversationId
            )
        }
    }

    private fun moveToTrash(isScheduled: Boolean) {
        if (isScheduled) {
            showTwoButtonInfoDialog(
                titleStringId = R.string.scheduled_message_moved_to_trash_title,
                messageStringId = R.string.scheduled_message_moved_to_trash_desc,
                negativeStringId = R.string.cancel,
                onPositiveButtonClicked = {
                    viewModel.moveToTrash()
                    onBackPressed()
                }
            )
        } else {
            viewModel.moveToTrash()
            onBackPressed()
        }
    }

    private fun scrollToTheExpandedMessage(messageToScrollTo: Message?) {
        val messageBodyIndexToScrollTo = if (messageToScrollTo == null) {
            messageExpandableAdapter.itemCount - 1
        } else {
            messageExpandableAdapter.visibleItems.indexOfLast {
                it.message == messageToScrollTo
            }
        }
        val messageHeaderIndexToScrollTo = messageBodyIndexToScrollTo - 1
        recyclerViewLinearLayoutManager.scrollToPositionWithOffset(messageHeaderIndexToScrollTo, 0)
    }

    private fun showLabelsActionSheet(labelActionSheetType: LabelType = LabelType.MESSAGE_LABEL) {
        LabelsActionSheet.newInstance(
            messageIds = listOf(messageOrConversationId),
            currentFolderLocation = openedFolderLocationId,
            currentLocationId = openedFolderLabelId ?: openedFolderLocationId.toString(),
            labelType = labelActionSheetType,
            actionSheetTarget =
            if (viewModel.isConversationEnabled()) ActionSheetTarget.CONVERSATION_ITEM_IN_DETAIL_SCREEN
            else ActionSheetTarget.MESSAGE_ITEM_IN_DETAIL_SCREEN
        )
            .show(supportFragmentManager, LabelsActionSheet::class.qualifiedName)
    }

    private fun displayToolbarData(conversation: ConversationUiModel) {
        starToggleButton.isChecked = conversation.isStarred
        val isInvalidSubject = conversation.subject.isNullOrEmpty()
        val subject = if (isInvalidSubject) getString(R.string.empty_subject) else conversation.subject
        val messagesInConversation = conversation.messages.count()
        val numberOfMessagesFormatted = resources.getQuantityString(
            R.plurals.x_messages_count,
            messagesInConversation,
            messagesInConversation
        )
        collapsedToolbarMessagesCountTextView.text = numberOfMessagesFormatted
        // Initially, the expanded message count view is shown instead.
        // Visibility changes are handled by `OnOffsetChangedListener`
        collapsedToolbarMessagesCountTextView.isVisible = false

        expandedToolbarMessagesCountTextView.text = numberOfMessagesFormatted
        expandedToolbarMessagesCountTextView.isVisible = messagesInConversation > 1

        collapsedToolbarTitleTextView.text = subject
        collapsedToolbarTitleTextView.visibility = View.INVISIBLE
        expandedToolbarTitleTextView.text = subject
    }

    /**
     * Legacy method to executes reply, reply_all and forward op
     * @param messageOrConversationId the message or conversation ID on which to perform the action on.
     * Passing a conversation ID will cause the action to be applied to the last message that is not a draft.
     */
    fun executeMessageAction(
        messageAction: Constants.MessageActionType,
        messageOrConversationId: String?
    ) {
        try {
            val message = requireNotNull(
                viewModel.decryptedConversationUiModel.value?.messages?.find {
                    it.messageId == messageOrConversationId
                } ?: viewModel.decryptedConversationUiModel.value?.messages?.last { it.isDraft().not() }
            )
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
                    positiveStringId = R.string.ok,
                    negativeStringId = R.string.learn_more,
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
                mBigContentHolder.content = editIntentExtras.mBigContentHolder.content
                startComposeLauncher.launch(StartCompose.Input(intentExtrasData = editIntentExtras))
            }
        )
    }

    private fun observePermissionMissingDialogTrigger() {
        viewModel.showPermissionMissingDialog.observe(this) {
            DialogUtils.showInfoDialog(
                context = this,
                title = getString(R.string.need_permissions_title),
                message = getString(R.string.need_storage_permissions_download_attachment_text),
                okListener = { }
            )
        }
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
                }
                progress.visibility = View.GONE
                Timber.w("MessageDetailsError, $content")
            }
        }
    }

    private fun onLoadEmbeddedImagesClicked(message: Message, embeddedImageIds: List<String>) {
        // this will ensure that the message has been loaded
        // and will protect from premature clicking on download attachments button
        if (viewModel.renderingPassed) {
            viewModel.startDownloadEmbeddedImagesJob(message, embeddedImageIds)
        }
    }

    private fun onDisplayRemoteContentClicked(message: Message) {
        viewModel.displayRemoteContent(message)
        viewModel.checkStoragePermission.observe(this, { storagePermissionHelper.checkPermission() })
    }

    private fun onEditDraftClicked(message: Message) {
        val intent = AppUtil.decorInAppIntent(Intent(this, ComposeMessageActivity::class.java))
        intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, message.messageId)
        intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_RESPONSE_INLINE, message.isInline)
        intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID, message.addressID)
        startActivity(intent)
    }

    private fun onDownloadAttachment(attachment: Attachment) {
        attachmentToDownload.set(attachment)
        storagePermissionHelper.checkPermission()
    }

    private fun onReplyMessageClicked(messageAction: Constants.MessageActionType, message: Message) {
        executeMessageAction(messageAction, message.messageId)
    }

    private fun onShowMessageActionSheet(message: Message) {
        MessageActionSheet.newInstance(
            ActionSheetTarget.MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN,
            listOf(message.messageId ?: messageOrConversationId),
            message.location,
            openedFolderLabelId ?: message.location.toString(),
            getCurrentSubject(),
            getMessagesFrom(message.sender?.name),
            message.isStarred ?: false,
            message.isScheduled
        ).show(supportFragmentManager, MessageActionSheet::class.qualifiedName)
    }

    fun printMessage(messageId: String) {
        viewModel.printMessage(messageId, primaryBaseActivity)
    }

    class Launcher : ActivityResultContract<Input, Unit>() {

        override fun createIntent(context: Context, input: Input): Intent =
            input.toIntent(context)

        override fun parseResult(resultCode: Int, intent: Intent?) {}
    }

    data class Input(
        val messageId: String,
        val locationType: Constants.MessageLocationType?,
        val labelId: LabelId?,
        val messageSubject: String?
    ) {

        fun toIntent(context: Context) = Intent(context, MessageDetailsActivity::class.java)
            .putExtra(EXTRA_MESSAGE_OR_CONVERSATION_ID, messageId)
            .putExtra(EXTRA_MESSAGE_LOCATION_ID, locationType?.messageLocationTypeValue)
            .putExtra(EXTRA_MAILBOX_LABEL_ID, labelId?.id)
            .putExtra(EXTRA_MESSAGE_SUBJECT, messageSubject)
    }

    companion object {

        const val EXTRA_MESSAGE_OR_CONVERSATION_ID = "messageOrConversationId"
        const val EXTRA_MESSAGE_LOCATION_ID = "messageOrConversationLocation"
        const val EXTRA_MAILBOX_LABEL_ID = "mailboxLabelId"

        const val EXTRA_MESSAGE_RECIPIENT_USER_ID = "message_recipient_user_id"
        const val EXTRA_MESSAGE_RECIPIENT_USERNAME = "message_recipient_username"

        const val EXTRA_MESSAGE_SUBJECT = "message_subject"
    }
}
