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

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.os.bundleOf
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.WorkInfo
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseStoragePermissionActivity
import ch.protonmail.android.activities.EXTRA_SWITCHED_USER
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment.ILabelCreationListener
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment.ILabelsChangeListener
import ch.protonmail.android.activities.dialogs.MoveToFolderDialogFragment
import ch.protonmail.android.activities.dialogs.MoveToFolderDialogFragment.IMoveMessagesListener
import ch.protonmail.android.activities.guest.LoginActivity
import ch.protonmail.android.activities.labelsManager.EXTRA_CREATE_ONLY
import ch.protonmail.android.activities.labelsManager.EXTRA_MANAGE_FOLDERS
import ch.protonmail.android.activities.labelsManager.EXTRA_POPUP_STYLE
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity
import ch.protonmail.android.activities.mailbox.MailboxActivity
import ch.protonmail.android.activities.messageDetails.attachments.MessageDetailsAttachmentListAdapter
import ch.protonmail.android.activities.messageDetails.attachments.OnAttachmentDownloadCallback
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.api.models.SimpleMessage
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.MessageActionType
import ch.protonmail.android.core.Constants.MessageLocationType.Companion.fromInt
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.DownloadEmbeddedImagesEvent
import ch.protonmail.android.events.DownloadedAttachmentEvent
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.MessageSentEvent
import ch.protonmail.android.events.PostPhishingReportEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.events.user.MailSettingsEvent
import ch.protonmail.android.jobs.PostArchiveJob
import ch.protonmail.android.jobs.PostInboxJob
import ch.protonmail.android.jobs.PostSpamJob
import ch.protonmail.android.jobs.PostTrashJobV2
import ch.protonmail.android.jobs.PostUnreadJob
import ch.protonmail.android.jobs.ReportPhishingJob
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.CustomLocale
import ch.protonmail.android.utils.DownloadUtils
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.UserUtils
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.MODE_ACCORDION
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showDeleteConfirmationDialog
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showInfoDialogWithTwoButtons
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showSignedInSnack
import ch.protonmail.android.views.MessageActionButton
import ch.protonmail.android.views.PMWebViewClient
import ch.protonmail.android.worker.KEY_POST_LABEL_WORKER_RESULT_ERROR
import ch.protonmail.android.worker.PostLabelWorker
import com.birbit.android.jobqueue.Job
import com.google.android.material.snackbar.Snackbar
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_message_details.*
import me.proton.core.util.android.workmanager.activity.getWorkManager
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@AndroidEntryPoint
internal class MessageDetailsActivity :
    BaseStoragePermissionActivity(),
    ILabelCreationListener,
    ILabelsChangeListener,
    IMoveMessagesListener {

    private lateinit var messageId: String
    private lateinit var pmWebViewClient: PMWebViewClient
    private lateinit var messageExpandableAdapter: MessageDetailsAdapter
    private lateinit var attachmentsListAdapter: MessageDetailsAttachmentListAdapter
    private lateinit var primaryBaseActivity: Context

    /**
     * Whether the current message needs to be store in database. If transient if won't be stored
     */
    private var isTransientMessage = false
    private var messageRecipientUsername: String? = null
    private val buttonsVisibilityHandler = Handler(Looper.getMainLooper())
    private val attachmentToDownloadId = AtomicReference<String?>(null)
    private var markAsRead = false
    private var showPhishingReportButton = true
    private val viewModel: MessageDetailsViewModel by viewModels()

    private var buttonsVisibilityRunnable = Runnable { action_buttons.visibility = View.VISIBLE }

    override fun getLayoutId(): Int = R.layout.activity_message_details

    override fun storagePermissionGranted() {
        val attachmentToDownloadIdAux = attachmentToDownloadId.getAndSet(null)
        if (!attachmentToDownloadIdAux.isNullOrEmpty()) {
            viewModel.tryDownloadingAttachment(this, attachmentToDownloadIdAux, messageId)
        }
    }

    override fun checkForPermissionOnStartup(): Boolean = false

    override fun attachBaseContext(base: Context) {
        primaryBaseActivity = base
        super.attachBaseContext(CustomLocale.apply(base))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (mUserManager.isFirstMessageDetails) {
            mUserManager.firstMessageDetailsDone()
        }

        markAsRead = true
        messageId = requireNotNull(intent.getStringExtra(EXTRA_MESSAGE_ID))
        messageRecipientUsername = intent.getStringExtra(EXTRA_MESSAGE_RECIPIENT_USERNAME)
        isTransientMessage = intent.getBooleanExtra(EXTRA_TRANSIENT_MESSAGE, false)
        val currentAccountUsername = mUserManager.username
        AppUtil.clearNotifications(this)
        if (!mUserManager.isLoggedIn) {
            startActivity(AppUtil.decorInAppIntent(Intent(this, LoginActivity::class.java)))
        }
        loadMailSettings()
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = null
        }
        initAdapters()
        if (messageRecipientUsername != null && currentAccountUsername != messageRecipientUsername) {
            showInfoDialogWithTwoButtons(
                this,
                getString(R.string.switch_accounts_question),
                String.format(getString(R.string.switch_to_account), messageRecipientUsername),
                getString(R.string.cancel),
                getString(R.string.okay),
                { finish() },
                {
                    mUserManager.switchToAccount(messageRecipientUsername!!)
                    continueSetup()
                    invalidateOptionsMenu()
                    showSignedInSnack(
                        coordinatorLayout,
                        String.format(getString(R.string.signed_in_with), messageRecipientUsername)
                    )
                },
                false
            )
        } else {
            continueSetup()
        }

        findViewById<MessageActionButton>(R.id.reply).isEnabled = false
        findViewById<MessageActionButton>(R.id.reply_all).isEnabled = false
        findViewById<MessageActionButton>(R.id.forward).isEnabled = false
    }

    private fun continueSetup() {
        viewModel.tryFindMessage()
        viewModel.message.observe(this, MessageObserver())
        viewModel.decryptedMessageData.observe(this, DecryptedMessageObserver())

        viewModel.labels.observe(
            this,
            LabelsObserver(messageExpandableAdapter, viewModel.folderIds)
        )

        viewModel.webViewContent.observe(this, WebViewContentObserver())
        viewModel.messageDetailsError.observe(this, MessageDetailsErrorObserver())
        viewModel.pendingSend.observe(this, PendingSendObserver())
        listenForConnectivityEvent()
    }

    private fun initAdapters() {
        attachmentsListAdapter = MessageDetailsAttachmentListAdapter(
            this,
            OnAttachmentDownloadCallback(storagePermissionHelper, attachmentToDownloadId)
        )
        pmWebViewClient = MessageDetailsPmWebViewClient(mUserManager, this)
        messageExpandableAdapter = MessageDetailsAdapter(
            this,
            mJobManager,
            Message(),
            "",
            wv_scroll,
            pmWebViewClient,
            { onLoadEmbeddedImagesCLick() }
        ) { onDisplayImagesCLick() }
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

    override fun move(folderId: String) {
        viewModel.removeMessageLabels()
        viewModel.messageSavedInDBResult.observe(
            this,
            object : Observer<Boolean?> {
                override fun onChanged(aBoolean: Boolean?) {
                    viewModel.messageSavedInDBResult.removeObserver(this)
                    val message = viewModel.decryptedMessageData.value
                    MessageUtils.moveMessage(
                        this@MessageDetailsActivity,
                        mJobManager,
                        folderId,
                        viewModel.folderIds,
                        listOf(SimpleMessage(message))
                    )
                    viewModel.markRead(true)
                    onBackPressed()
                }
            }
        )
        viewModel.saveMessage()
    }

    override fun showFoldersManager() {
        val foldersManagerIntent = Intent(this, LabelsManagerActivity::class.java)
        foldersManagerIntent.putExtras(
            bundleOf(EXTRA_MANAGE_FOLDERS to true, EXTRA_POPUP_STYLE to true, EXTRA_CREATE_ONLY to true)
        )
        startActivity(AppUtil.decorInAppIntent(foldersManagerIntent))
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
        if (markAsRead) {
            viewModel.markRead(true)
        }
        mApp.bus.unregister(viewModel)
        stopEmbeddedImagesTask()
        mApp.bus.unregister(this)
    }

    override fun secureContent(): Boolean = true

    override fun enableScreenshotProtector() {
        screenShotPreventer.visibility = View.VISIBLE
    }

    override fun disableScreenshotProtector() {
        screenShotPreventer.visibility = View.GONE
    }

    private fun stopEmbeddedImagesTask() {
        messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.GONE)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.message_details_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {

        val message = viewModel.decryptedMessageData.value
        if (message != null) {
            val trash = menu.findItem(R.id.move_to_trash)
            val viewHeaders = menu.findItem(R.id.view_headers)
            val delete = menu.findItem(R.id.delete_message)
            val spam = menu.findItem(R.id.move_to_spam)
            val inbox = menu.findItem(R.id.move_to_inbox)
            val archive = menu.findItem(R.id.move_to_archive)
            val reportPhishing = menu.findItem(R.id.action_report_phishing)
            val printItem = menu.findItem(R.id.action_print)
            trash.isVisible = fromInt(message.location) != Constants.MessageLocationType.TRASH
            viewHeaders.isVisible = true
            delete.isVisible = fromInt(message.location) == Constants.MessageLocationType.TRASH
            spam.isVisible = fromInt(message.location) != Constants.MessageLocationType.SPAM
            inbox.isVisible = fromInt(message.location) != Constants.MessageLocationType.INBOX
            archive.isVisible = fromInt(message.location) != Constants.MessageLocationType.ARCHIVE
            reportPhishing.isVisible = showPhishingReportButton
            printItem.isVisible = true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var job: Job? = null
        val message = viewModel.decryptedMessageData.value
        when (item.itemId) {
            android.R.id.home -> {
                viewModel.markRead(true)
                onBackPressed()
                return true
            }
            R.id.move_to_trash ->
                if (message != null) {
                    job = PostTrashJobV2(listOf(message.messageId), null)
                } else {
                    showToast(R.string.message_not_loaded, Toast.LENGTH_SHORT)
                }
            R.id.view_headers -> if (message != null) {
                startActivity(
                    AppUtil.decorInAppIntent(
                        Intent(
                            this,
                            MessageViewHeadersActivity::class.java
                        ).putExtra(EXTRA_VIEW_HEADERS, message.header)
                    )
                )
            }
            R.id.delete_message ->
                showDeleteConfirmationDialog(
                    this,
                    getString(R.string.delete_message),
                    getString(R.string.confirm_destructive_action)
                ) {
                    viewModel.deleteMessage(messageId)
                    onBackPressed()
                }
            R.id.move_to_spam -> job = PostSpamJob(listOf(messageId))
            R.id.mark_unread -> {
                if (message != null) {
                    markAsRead = false
                    viewModel.markRead(false)
                }
                job = PostUnreadJob(listOf(messageId))
            }
            R.id.move_to_inbox -> job = PostInboxJob(listOf(messageId))
            R.id.move_to_archive -> job = PostArchiveJob(listOf(messageId))
            R.id.add_label -> {
                if (message == null) {
                    showToast(R.string.message_not_loaded, Toast.LENGTH_SHORT)
                } else {
                    showLabelsManagerDialog(supportFragmentManager, message)
                }
            }
            R.id.add_folder -> {
                if (message == null) {
                    showToast(R.string.message_not_loaded, Toast.LENGTH_SHORT)
                } else {
                    showFoldersManagerDialog(supportFragmentManager, message)
                }
            }
            R.id.action_report_phishing -> showReportPhishingDialog(message)
            R.id.action_print -> viewModel.printMessage(primaryBaseActivity) // do not change context type
        }
        if (job != null) {
            mJobManager.addJobInBackground(job)
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showReportPhishingDialog(message: Message?) {
        AlertDialog.Builder(this)
            .setTitle(R.string.phishing_dialog_title)
            .setMessage(R.string.phishing_dialog_message)
            .setPositiveButton(R.string.send) { _: DialogInterface?, _: Int ->
                showPhishingReportButton = false
                invalidateOptionsMenu()
                mJobManager.addJobInBackground(ReportPhishingJob(message))
            }
            .setNegativeButton(R.string.cancel, null).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        buttonsVisibilityHandler.removeCallbacks(buttonsVisibilityRunnable)
    }

    private fun showNoConnSnackExtended() {
        Timber.v("Show no connection")
        networkSnackBarUtil.hideAllSnackBars()
        networkSnackBarUtil.getNoConnectionSnackBar(
            mSnackLayout,
            mUserManager.user,
            this,
            { onConnectivityCheckRetry() },
            anchorViewId = R.id.action_buttons
        ).show()
        invalidateOptionsMenu()
    }

    private fun onConnectivityCheckRetry() {
        viewModel.fetchMessageDetails(false)
        networkSnackBarUtil.getCheckingConnectionSnackBar(
            mSnackLayout,
            R.id.action_buttons
        ).show()

        viewModel.checkConnectivityDelayed()
    }

    private fun hideNoConnSnackExtended() {
        networkSnackBarUtil.hideNoConnectionSnackBar()
        invalidateOptionsMenu()
    }

    private fun listenForConnectivityEvent() {
        viewModel.hasConnectivity.observe(
            this,
            { isConnectionActive ->
                Timber.v("isConnectionActive:$isConnectionActive")
                if (isConnectionActive) {
                    hideNoConnSnackExtended()
                    viewModel.fetchMessageDetails(false)
                } else {
                    showNoConnSnackExtended()
                }
            }
        )
    }

    fun listenForRecipientsKeys() {
        viewModel.reloadRecipientsEvent.observe(
            this,
            Observer { event: Event<Boolean?>? ->
                if (event == null) {
                    return@Observer
                }
                val content = event.getContentIfNotHandled() ?: return@Observer
                if (content) {
                    messageExpandableAdapter.refreshRecipientsLayout()
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
                mJobManager.addJobInBackground(PostSpamJob(listOf(messageId)))
                toastMessageId = R.string.phishing_report_send_message_moved_to_spam
                finish()
            }
            Status.STARTED,
            Status.FAILED,
            Status.NO_NETWORK,
            Status.UNAUTHORIZED -> {
                showPhishingReportButton = true
                invalidateOptionsMenu()
                toastMessageId = R.string.cannot_send_report_send
            }
            else -> throw IllegalStateException("Unknown message status: $status")
        }
        showToast(toastMessageId, Toast.LENGTH_SHORT)
    }

    @Subscribe
    @Suppress("UNUSED_PARAMETER")
    fun onMailSettingsEvent(event: MailSettingsEvent?) {
        loadMailSettings()
    }

    @Subscribe
    @Suppress("unused", "UNUSED_PARAMETER")
    fun onLogoutEvent(event: LogoutEvent?) {
        startActivity(AppUtil.decorInAppIntent(Intent(this, LoginActivity::class.java)))
        finish()
    }

    @Subscribe
    override fun onMessageSentEvent(event: MessageSentEvent) {
        super.onMessageSentEvent(event)
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

    private fun filterAndLoad(decryptedMessage: String) {

        val parsedMessage = viewModel.getParsedMessage(
            decryptedMessage,
            UiUtil.getRenderWidth(windowManager),
            AppUtil.readTxt(this, R.raw.editor),
            resources.getString(R.string.request_timeout)
        )

        if (isAutoShowRemoteImages) {
            viewModel.remoteContentDisplayed()
        }
        messageExpandableAdapter.displayContainerDisplayImages(View.GONE)
        pmWebViewClient.blockRemoteResources(!isAutoShowRemoteImages)
        viewModel.webViewContentWithoutImages.value = parsedMessage
    }

    override fun onBackPressed() {
        stopEmbeddedImagesTask()
        saveLastInteraction()
        if (messageRecipientUsername != null) {
            val mailboxIntent = AppUtil.decorInAppIntent(Intent(this, MailboxActivity::class.java))
            mailboxIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            mailboxIntent.putExtra(EXTRA_SWITCHED_USER, true)
            startActivity(mailboxIntent)
        }
        finish()
    }

    // TODO should be moved inside view model
    @Subscribe
    @Suppress("unused")
    fun onDownloadEmbeddedImagesEvent(event: DownloadEmbeddedImagesEvent) {
        when (event.status) {
            Status.SUCCESS -> {
                messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.GONE)
                messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.VISIBLE)
                viewModel.downloadEmbeddedImagesResult.observe(
                    this,
                    Observer { content ->
                        Timber.v("downloadEmbeddedImagesResult pair: $content")
                        messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.GONE)
                        if (content.isNullOrEmpty()) {
                            return@Observer
                        }
                        viewModel.webViewContentWithImages.setValue(content)
                    }
                )
                viewModel.onEmbeddedImagesDownloaded(event)
            }
            Status.NO_NETWORK -> {
                messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.VISIBLE)
                messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.GONE)
                showToast(R.string.load_embedded_images_failed_no_network)
            }
            Status.FAILED -> {
                messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.VISIBLE)
                messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.GONE)
                showToast(R.string.load_embedded_images_failed)
            }
            Status.STARTED -> {
                messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.GONE)
                messageExpandableAdapter.displayEmbeddedImagesDownloadProgress(View.VISIBLE)
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
                val eventAttachmentId = event.attachmentId
                val isDownloaded = Status.SUCCESS == status
                attachmentsListAdapter.setIsPgpEncrypted(viewModel.isPgpEncrypted())
                attachmentsListAdapter.setDownloaded(eventAttachmentId, isDownloaded)
                if (isDownloaded) {
                    DownloadUtils.viewAttachment(this, event.filename, event.attachmentUri)
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

    override fun onLabelCreated(labelName: String, color: String) {
        val postLabelResult = PostLabelWorker.Enqueuer(getWorkManager()).enqueue(labelName, color)
        postLabelResult.observe(
            this,
            {
                val state: WorkInfo.State = it.state

                if (state == WorkInfo.State.SUCCEEDED) {
                    showToast(getString(R.string.label_created), Toast.LENGTH_SHORT)
                    return@observe
                }

                if (state == WorkInfo.State.FAILED) {
                    val outputData = it.outputData
                    val errorMessage = outputData.getString(KEY_POST_LABEL_WORKER_RESULT_ERROR)
                        ?: getString(R.string.label_invalid)
                    showToast(errorMessage, Toast.LENGTH_SHORT)
                }
            }
        )
    }

    override fun onLabelsDeleted(checkedLabelIds: List<String>) {
        // NOOP
    }

    private fun showFoldersManagerDialog(fragmentManager: FragmentManager, message: Message) {
        val moveToFolderDialogFragment = MoveToFolderDialogFragment.newInstance(fromInt(message.location))
        val transaction = fragmentManager.beginTransaction()
        transaction.add(moveToFolderDialogFragment, moveToFolderDialogFragment.fragmentKey)
        transaction.commitAllowingStateLoss()
    }

    private fun showLabelsManagerDialog(fragmentManager: FragmentManager, message: Message) {
        val attachedLabels = HashSet(message.labelIDsNotIncludingLocations)
        val manageLabelsDialogFragment = ManageLabelsDialogFragment.newInstance(attachedLabels, null, null)
        fragmentManager.commit(allowStateLoss = true) {
            add(manageLabelsDialogFragment, manageLabelsDialogFragment.fragmentKey)
            addToBackStack(null)
        }
    }

    override fun onLabelsChecked(
        checkedLabelIds: List<String>,
        unchangedLabelIds: List<String>?,
        messageIds: List<String>?
    ) {
        val message = viewModel.decryptedMessageData.value
        // handle the case where too many labels exist for this message
        val maxLabelsAllowed = UserUtils.getMaxAllowedLabels(mUserManager)
        if (checkedLabelIds.size > maxLabelsAllowed) {
            val messageText = String.format(
                getString(R.string.max_labels_in_message),
                message!!.subject,
                maxLabelsAllowed
            )
            showToast(messageText, Toast.LENGTH_SHORT)
        } else {
            viewModel.findAllLabelsWithIds(checkedLabelIds.toMutableList())
        }
    }

    override fun onLabelsChecked(
        checkedLabelIds: List<String>,
        unchangedLabels: List<String>?,
        messageIds: List<String>?,
        messagesToArchive: List<String>?
    ) {
        val message = viewModel.decryptedMessageData.value
        onLabelsChecked(checkedLabelIds.toMutableList(), unchangedLabels, messageIds)
        mJobManager.addJobInBackground(PostArchiveJob(listOf(message!!.messageId)))
        onBackPressed()
    }

    private var showActionButtons = false

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

    private inner class MessageObserver : Observer<Message?> {
        override fun onChanged(message: Message?) {
            if (message != null) {
                onMessageFound(message)
                viewModel.message.removeObserver(this)
            } else {
                onMessageNotFound()
            }
        }

        private fun onMessageFound(message: Message) {
            viewModel.addressId = message.addressID!!
            if (message.isDownloaded) {
                if (!viewModel.renderingPassed) {
                    viewModel.fetchMessageDetails(true)
                }
            } else {
                viewModel.fetchMessageDetails(false)
            }
        }

        private fun onMessageNotFound() {
            if ((isTransientMessage || messageRecipientUsername != null) && mNetworkUtil.isConnected()) {
                // request to fetch message if didn't find in local database
                viewModel.fetchMessageDetails(false)
            }
        }
    }

    private inner class MessageDetailsPmWebViewClient(
        userManager: UserManager,
        activity: MessageDetailsActivity
    ) : PMWebViewClient(userManager, activity, false) {
        override fun onPageFinished(view: WebView, url: String) {
            if (amountOfRemoteResourcesBlocked() > 0) {
                messageExpandableAdapter.displayContainerDisplayImages(View.VISIBLE)
            }
            if (showActionButtons) {
                // workaround on some devices, the buttons appear quicker than the webview renders
                // the data
                buttonsVisibilityHandler.postDelayed(buttonsVisibilityRunnable, 500)
            }
            super.onPageFinished(view, url)
        }
    }

    private var prevAttachments: List<Attachment>? = null

    private inner class AttachmentsObserver : Observer<List<Attachment>?> {
        override fun onChanged(newAttachments: List<Attachment>?) {
            /* Workaround for don't let it loop. TODO: Find the cause of the infinite loop after
            Attachments are downloaded and screen has been locked / unlocked */
            val attachments: List<Attachment>?
            var loadImages = false
            if (newAttachments != null && newAttachments == prevAttachments) {
                attachments = prevAttachments
            } else {
                // load images only if there is a difference in attachments from the previous load
                loadImages = true
                prevAttachments = newAttachments
                attachments = prevAttachments
            }
            if (attachments != null) {
                viewModel.setAttachmentsList(attachments)
                val hasEmbeddedImages = viewModel.prepareEmbeddedImages()
                if (hasEmbeddedImages) {
                    if (isAutoShowEmbeddedImages || viewModel.isEmbeddedImagesDisplayed()) {
                        if (loadImages) {
                            viewModel.displayEmbeddedImages()
                        }
                        messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.GONE)
                    } else {
                        messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.VISIBLE)
                    }
                } else {
                    messageExpandableAdapter.displayLoadEmbeddedImagesContainer(View.GONE)
                }
                displayAttachmentInfo(attachments)
            } else {
                messageExpandableAdapter.displayAttachmentsViews(View.GONE)
            }
        }
    }

    private fun displayAttachmentInfo(attachments: List<Attachment>) {
        val attachmentsCount = attachments.size
        val totalAttachmentSize = attachments.map { it.fileSize }.sum()
        val attachmentsVisibility: Int

        messageExpandableAdapter.attachmentsContainer.setTitle(attachmentsCount, totalAttachmentSize)
        attachmentsListAdapter.setList(attachments)
        messageExpandableAdapter.attachmentsContainer.attachmentsAdapter = attachmentsListAdapter
        attachmentsVisibility = if (attachmentsCount > 0) View.VISIBLE else View.GONE
        messageExpandableAdapter.displayAttachmentsViews(attachmentsVisibility)
    }

    private inner class DecryptedMessageObserver : Observer<Message?> {
        override fun onChanged(message: Message?) {
            if (message == null) {
                return
            }
            Timber.v("New decrypted message ${message.messageId}")
            viewModel.messageAttachments.observe(this@MessageDetailsActivity, AttachmentsObserver())
            viewModel.renderedFromCache = AtomicBoolean(true)
            val decryptedBody = getDecryptedBody(message.decryptedHTML)
            if (decryptedBody.isEmpty() || message.messageBody.isNullOrEmpty()) {
                UiUtil.showInfoSnack(mSnackLayout, this@MessageDetailsActivity, R.string.decryption_error_desc).show()
                return
            }
            findViewById<MessageActionButton>(R.id.reply).isEnabled = true
            findViewById<MessageActionButton>(R.id.reply_all).isEnabled = true
            findViewById<MessageActionButton>(R.id.forward).isEnabled = true

            messageExpandableAdapter.setMessageData(message)
            messageExpandableAdapter.refreshRecipientsLayout()
            if (viewModel.refreshedKeys) {
                filterAndLoad(decryptedBody)
                messageExpandableAdapter.mode = MODE_ACCORDION
                wv_scroll.layoutManager = LinearLayoutManager(this@MessageDetailsActivity)
                listenForRecipientsKeys()
                wv_scroll.adapter = messageExpandableAdapter
            }
            viewModel.triggerVerificationKeyLoading()
            action_buttons.setOnMessageActionListener { messageAction: MessageActionType? ->
                try {
                    val newMessageTitle = MessageUtils.buildNewMessageTitle(
                        this@MessageDetailsActivity,
                        messageAction,
                        message.subject
                    )
                    val userUsedSpace = app.getSecureSharedPreferences(mUserManager.username)
                        .getLong(Constants.Prefs.PREF_USED_SPACE, 0)
                    val userMaxSpace = if (mUserManager.user.maxSpace == 0L) {
                        Long.MAX_VALUE
                    } else {
                        mUserManager.user.maxSpace
                    }
                    val percentageUsed = userUsedSpace * 100 / userMaxSpace
                    if (percentageUsed >= 100) {
                        showInfoDialogWithTwoButtons(
                            this@MessageDetailsActivity,
                            getString(R.string.storage_limit_warning_title),
                            getString(R.string.storage_limit_reached_text),
                            getString(R.string.learn_more),
                            getString(R.string.okay),
                            {
                                val browserIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(getString(R.string.limit_reached_learn_more))
                                )
                                startActivity(browserIntent)
                            },
                            { },
                            true
                        )
                    } else {
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
                                intent.putExtra(
                                    ComposeMessageActivity.EXTRA_MESSAGE_IS_TRANSIENT,
                                    isTransientMessage
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
                        viewModel.prepareEditMessageIntent(
                            messageAction!!,
                            message,
                            newMessageTitle,
                            decryptedBody,
                            mBigContentHolder
                        )
                    }
                } catch (exc: Exception) {
                    Timber.tag("588").e(exc, "Exception on reply panel press")
                }
            }
            progress.visibility = View.GONE
            invalidateOptionsMenu()
            viewModel.renderingPassed = true
        }
    }

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

    private inner class WebViewContentObserver : Observer<String?> {
        override fun onChanged(content: String?) {
            messageExpandableAdapter.loadDataFromUrlToMessageView(content!!)
        }
    }

    private inner class PendingSendObserver : Observer<PendingSend?> {
        override fun onChanged(pendingSend: PendingSend?) {
            if (pendingSend != null) {
                val cannotEditSnack = Snackbar.make(
                    findViewById(R.id.root_layout),
                    R.string.message_can_not_edit,
                    Snackbar.LENGTH_INDEFINITE
                )
                val view = cannotEditSnack.view
                view.setBackgroundColor(getColor(R.color.red))
                val tv = view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                tv.setTextColor(Color.WHITE)
                cannotEditSnack.show()
                action_buttons.visibility = View.INVISIBLE
            } else {
                showActionButtons = true
            }
        }
    }

    private fun onLoadEmbeddedImagesCLick() {
        // this will ensure that the message has been loaded
        // and will protect from premature clicking on download attachments button
        if (viewModel.renderingPassed) {
            viewModel.startDownloadEmbeddedImagesJob()
        }
        return
    }

    private fun onDisplayImagesCLick() {
        viewModel.displayRemoteContentClicked()
        viewModel.checkStoragePermission.observe(this, { storagePermissionHelper.checkPermission() })
        return
    }

    companion object {
        const val EXTRA_MESSAGE_ID = "messageId"

        // if transient is true it will not save Message object to db
        const val EXTRA_TRANSIENT_MESSAGE = "transient_message"
        const val EXTRA_MESSAGE_RECIPIENT_USERNAME = "message_recipient_username"
    }
}
