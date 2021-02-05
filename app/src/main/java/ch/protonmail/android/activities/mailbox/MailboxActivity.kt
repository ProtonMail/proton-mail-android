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
package ch.protonmail.android.activities.mailbox

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ActionMode
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.AbsListView.MultiChoiceModeListener
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.Transformations
import androidx.lifecycle.switchMap
import androidx.loader.app.LoaderManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import androidx.work.WorkInfo
import ch.protonmail.android.R
import ch.protonmail.android.activities.EXTRA_FIRST_LOGIN
import ch.protonmail.android.activities.EXTRA_LOGOUT
import ch.protonmail.android.activities.EXTRA_SETTINGS_ITEM_TYPE
import ch.protonmail.android.activities.EXTRA_SWITCHED_TO_USER
import ch.protonmail.android.activities.EXTRA_SWITCHED_USER
import ch.protonmail.android.activities.EditSettingsItemActivity
import ch.protonmail.android.activities.EngagementActivity
import ch.protonmail.android.activities.FLOW_START_ACTIVITY
import ch.protonmail.android.activities.FLOW_TRY_COMPOSE
import ch.protonmail.android.activities.FLOW_USED_SPACE_CHANGED
import ch.protonmail.android.activities.MailboxViewModel
import ch.protonmail.android.activities.MailboxViewModel.MaxLabelsReached
import ch.protonmail.android.activities.NavigationActivity
import ch.protonmail.android.activities.REQUEST_CODE_SWITCHED_USER
import ch.protonmail.android.activities.SearchActivity
import ch.protonmail.android.activities.SettingsActivity
import ch.protonmail.android.activities.SettingsItem
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment.ILabelCreationListener
import ch.protonmail.android.activities.dialogs.ManageLabelsDialogFragment.ILabelsChangeListener
import ch.protonmail.android.activities.dialogs.MoveToFolderDialogFragment
import ch.protonmail.android.activities.dialogs.MoveToFolderDialogFragment.IMoveMessagesListener
import ch.protonmail.android.activities.guest.LoginActivity
import ch.protonmail.android.activities.guest.MailboxLoginActivity
import ch.protonmail.android.activities.labelsManager.EXTRA_CREATE_ONLY
import ch.protonmail.android.activities.labelsManager.EXTRA_MANAGE_FOLDERS
import ch.protonmail.android.activities.labelsManager.EXTRA_POPUP_STYLE
import ch.protonmail.android.activities.labelsManager.LabelsManagerActivity
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.activities.settings.EXTRA_CURRENT_MAILBOX_LABEL_ID
import ch.protonmail.android.activities.settings.EXTRA_CURRENT_MAILBOX_LOCATION
import ch.protonmail.android.activities.settings.SettingsEnum
import ch.protonmail.android.adapters.messages.MessagesListViewHolder.MessageViewHolder
import ch.protonmail.android.adapters.messages.MessagesRecyclerViewAdapter
import ch.protonmail.android.adapters.swipe.ArchiveSwipeHandler
import ch.protonmail.android.adapters.swipe.MarkReadSwipeHandler
import ch.protonmail.android.adapters.swipe.SpamSwipeHandler
import ch.protonmail.android.adapters.swipe.StarSwipeHandler
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.adapters.swipe.TrashSwipeHandler
import ch.protonmail.android.api.models.MessageCount
import ch.protonmail.android.api.models.SimpleMessage
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.counters.CountersDatabase
import ch.protonmail.android.api.models.room.counters.CountersDatabaseFactory
import ch.protonmail.android.api.models.room.counters.TotalLabelCounter
import ch.protonmail.android.api.models.room.counters.TotalLocationCounter
import ch.protonmail.android.api.models.room.messages.Label
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.api.services.MessagesService.Companion.getLastMessageTime
import ch.protonmail.android.api.services.MessagesService.Companion.startFetchFirstPage
import ch.protonmail.android.api.services.MessagesService.Companion.startFetchFirstPageByLabel
import ch.protonmail.android.api.services.MessagesService.Companion.startFetchMessages
import ch.protonmail.android.api.services.MessagesService.Companion.startFetchMessagesByLabel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.DrawerOptionType
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.Constants.MessageLocationType.Companion.fromInt
import ch.protonmail.android.core.Constants.Prefs.PREF_SWIPE_GESTURES_DIALOG_SHOWN
import ch.protonmail.android.core.Constants.SWIPE_GESTURES_CHANGED_VERSION
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.events.AuthStatus
import ch.protonmail.android.events.FetchLabelsEvent
import ch.protonmail.android.events.FetchUpdatesEvent
import ch.protonmail.android.events.ForceSwitchedAccountEvent
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.MailboxLoadedEvent
import ch.protonmail.android.events.MailboxLoginEvent
import ch.protonmail.android.events.MailboxNoMessagesEvent
import ch.protonmail.android.events.MessageCountsEvent
import ch.protonmail.android.events.ParentEvent
import ch.protonmail.android.events.RefreshDrawerEvent
import ch.protonmail.android.events.SettingsChangedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.events.user.MailSettingsEvent
import ch.protonmail.android.fcm.FcmUtil
import ch.protonmail.android.fcm.PMRegistrationWorker
import ch.protonmail.android.jobs.EmptyFolderJob
import ch.protonmail.android.jobs.FetchByLocationJob
import ch.protonmail.android.jobs.FetchLabelsJob
import ch.protonmail.android.jobs.PostArchiveJob
import ch.protonmail.android.jobs.PostInboxJob
import ch.protonmail.android.jobs.PostReadJob
import ch.protonmail.android.jobs.PostSpamJob
import ch.protonmail.android.jobs.PostStarJob
import ch.protonmail.android.jobs.PostTrashJobV2
import ch.protonmail.android.jobs.PostUnreadJob
import ch.protonmail.android.jobs.PostUnstarJob
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.servers.notification.EXTRA_MAILBOX_LOCATION
import ch.protonmail.android.servers.notification.EXTRA_USERNAME
import ch.protonmail.android.settings.pin.EXTRA_TOTAL_COUNT_EVENT
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.NetworkSnackBarUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showDeleteConfirmationDialog
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showInfoDialogWithTwoButtons
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showSignedInSnack
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showUndoSnackbar
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import ch.protonmail.android.worker.KEY_POST_LABEL_WORKER_RESULT_ERROR
import ch.protonmail.android.worker.PostLabelWorker
import com.birbit.android.jobqueue.Job
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_mailbox.*
import me.proton.core.util.android.workmanager.activity.getWorkManager
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.seconds

private const val TAG_MAILBOX_ACTIVITY = "MailboxActivity"
private const val ACTION_MESSAGE_DRAFTED = "ch.protonmail.MESSAGE_DRAFTED"
private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
private const val STATE_MAILBOX_LOCATION = "mailbox_location"
private const val STATE_MAILBOX_LABEL_LOCATION = "mailbox_label_location"
private const val STATE_MAILBOX_LABEL_LOCATION_NAME = "mailbox_label_location_name"
const val LOADER_ID = 0
const val LOADER_ID_LABELS_OFFLINE = 32
private const val REQUEST_CODE_TRASH_MESSAGE_DETAILS = 1
private const val REQUEST_CODE_COMPOSE_MESSAGE = 19


@AndroidEntryPoint
class MailboxActivity :
    NavigationActivity(),
    MultiChoiceModeListener,
    OnRefreshListener,
    ILabelCreationListener,
    ILabelsChangeListener,
    IMoveMessagesListener,
    DialogInterface.OnDismissListener {

    private lateinit var countersDatabase: CountersDatabase
    private lateinit var pendingActionsDatabase: PendingActionsDatabase

    @Inject
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var networkSnackBarUtil: NetworkSnackBarUtil

    @Inject
    lateinit var pmRegistrationWorkerEnqueuer: PMRegistrationWorker.Enqueuer

    private lateinit var messagesAdapter: MessagesRecyclerViewAdapter
    private val mailboxLocationMain = MutableLiveData<MessageLocationType>()
    private val isLoadingMore = AtomicBoolean(false)
    private var scrollStateChanged = false
    private var actionMode: ActionMode? = null
    private var swipeCustomizeSnack: Snackbar? = null
    private var mailboxLabelId: String? = null
    private var mailboxLabelName: String? = null
    private var refreshMailboxJobRunning = false
    private lateinit var syncUUID: String
    private var customizeSwipeSnackShown = false
    private var catchLabelEvents = false
    private val mailboxViewModel: MailboxViewModel by viewModels()
    private var storageLimitApproachingAlertDialog: AlertDialog? = null
    private var liveSharedPreferences: LiveSharedPreferences? = null
    private val handler = Handler(Looper.getMainLooper())

    override val currentLabelId get() = mailboxLabelId

    override fun getLayoutId(): Int = R.layout.activity_mailbox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        countersDatabase = CountersDatabaseFactory.getInstance(this).getDatabase()
        pendingActionsDatabase = PendingActionsDatabaseFactory.getInstance(this).getDatabase()

        // force reload of MessageDetailsRepository's internal dependencies in case we just switched user

        // TODO if we decide to use special flag for switching (and not login), change this
        if (intent.getBooleanExtra(EXTRA_FIRST_LOGIN, false)) {
            messageDetailsRepository.reloadDependenciesForUser(mUserManager.username)
            FcmUtil.setTokenSent(false) // force FCM to re-register
        }
        val extras = intent.extras
        if (!mUserManager.isEngagementShown) {
            startActivity(AppUtil.decorInAppIntent(Intent(this, EngagementActivity::class.java)))
        }
        mailboxLocationMain.value = MessageLocationType.INBOX
        // Set the padding to match the Status Bar height
        if (savedInstanceState != null) {
            val locationInt = savedInstanceState.getInt(STATE_MAILBOX_LOCATION)
            mailboxLabelId = savedInstanceState.getString(STATE_MAILBOX_LABEL_LOCATION)
            mailboxLabelName = savedInstanceState.getString(STATE_MAILBOX_LABEL_LOCATION_NAME)
            mailboxLocationMain.value = fromInt(locationInt)
        }
        if (extras != null && extras.containsKey(EXTRA_MAILBOX_LOCATION)) {
            setupNewMessageLocation(extras.getInt(EXTRA_MAILBOX_LOCATION))
        }
        startObserving()
        mailboxViewModel.toastMessageMaxLabelsReached.observe(
            this,
            { event: Event<MaxLabelsReached?> ->
                val maxLabelsReached = event.getContentIfNotHandled()
                if (maxLabelsReached != null) {
                    val message =
                        String.format(
                            getString(R.string.max_labels_exceeded),
                            maxLabelsReached.subject,
                            maxLabelsReached.maxAllowedLabels
                        )
                    showToast(message, Toast.LENGTH_SHORT)
                }
            }
        )
        mailboxViewModel.hasConnectivity.observe(
            this,
            { onConnectivityEvent(it) }
        )

        startObservingUsedSpace()

        messagesAdapter = MessagesRecyclerViewAdapter(
            this,
            object : Function1<SelectionModeEnum, Unit> {
                var actionModeAux: ActionMode? = null
                override fun invoke(selectionModeEvent: SelectionModeEnum) {
                    when (selectionModeEvent) {
                        SelectionModeEnum.STARTED -> actionModeAux = startActionMode(this@MailboxActivity)
                        SelectionModeEnum.ENDED -> {
                            val actionModeEnd = actionModeAux
                            if (actionModeEnd != null) {
                                actionModeEnd.finish()
                                this.actionModeAux = null
                            }
                        }
                    }
                }
            }
        )

        contactsRepository.findAllContactsEmailsAsync().observe(
            this,
            { contactEmails: List<ContactEmail> ->
                messagesAdapter.setContactsList(contactEmails)
            }
        )

        mailboxViewModel.pendingSendsLiveData.observe(
            this,
            { pendingSendList: List<PendingSend> ->
                messagesAdapter.setPendingForSendingList(pendingSendList)
            }
        )

        mailboxViewModel.pendingUploadsLiveData.observe(
            this,
            { pendingUploadList: List<PendingUpload> ->
                messagesAdapter.setPendingUploadsList(pendingUploadList)
            }
        )

        messageDetailsRepository.getAllLabels().observe(
            this,
            { labels: List<Label> ->
                messagesAdapter.setLabels(labels)
            }
        )

        mailboxViewModel.hasSuccessfullyDeletedMessages.observe(
            this,
            { isSuccess ->
                Timber.v("Delete message status is success $isSuccess")
                if (!isSuccess) {
                    showToast(R.string.message_deleted_error)
                }
            }
        )

        checkUserAndFetchNews()

        if (extras != null && extras.containsKey(EXTRA_SWITCHED_TO_USER)) {
            switchAccountProcedure(extras.getString(EXTRA_SWITCHED_TO_USER)!!) // should never be null here
        }

        setUpDrawer()
        setTitle()

        messages_list_view.adapter = messagesAdapter
        messages_list_view.layoutManager = LinearLayoutManager(this)
        buildSwipeProcessor()
        initializeSwipeRefreshLayout(swipe_refresh_layout)
        initializeSwipeRefreshLayout(spinner_layout)
        initializeSwipeRefreshLayout(no_messages_layout)

        if (mUserManager.isFirstMailboxLoad) {
            swipeCustomizeSnack = Snackbar.make(
                findViewById(R.id.drawer_layout),
                getString(R.string.customize_swipe_actions),
                Snackbar.LENGTH_INDEFINITE
            ).apply {
                view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.setTextColor(Color.WHITE)
                setAction(getString(R.string.settings)) {
                    val settingsIntent = AppUtil.decorInAppIntent(
                        Intent(
                            this@MailboxActivity,
                            SettingsActivity::class.java
                        )
                    )
                    settingsIntent.putExtra(
                        EXTRA_CURRENT_MAILBOX_LOCATION,
                        if (mailboxLocationMain.value != null) mailboxLocationMain.value!!.messageLocationTypeValue
                        else MessageLocationType.INBOX.messageLocationTypeValue
                    )
                    settingsIntent.putExtra(EXTRA_CURRENT_MAILBOX_LABEL_ID, mailboxLabelId)
                    startActivity(settingsIntent)
                }
                setActionTextColor(ContextCompat.getColor(this@MailboxActivity, R.color.icon_purple))
            }
            mUserManager.firstMailboxLoadDone()
        }

        messagesAdapter.setItemClick { message: Message? ->
            if (message != null && !message.messageId.isNullOrEmpty()) {
                OnMessageClickTask(
                    WeakReference(this@MailboxActivity),
                    messageDetailsRepository,
                    message
                ).execute()
            }
        }

        checkRegistration()
        closeDrawer()

        messages_list_view.addOnScrollListener(listScrollListener)

        fetchOrganizationData()

        val messagesLiveData: LiveData<List<Message>> =
            Transformations.switchMap(mailboxLocationMain) { location: MessageLocationType? ->
                getLiveDataByLocation(messageDetailsRepository, location!!)
            }

        mailboxLocationMain.observe(
            this,
            { newLocation: MessageLocationType? ->
                messagesAdapter.setNewLocation(newLocation!!)
            }
        )
        messagesLiveData.observe(this, MessagesListObserver(messagesAdapter))
        ItemTouchHelper(SwipeController()).attachToRecyclerView(messages_list_view)

        if (extras != null && extras.getBoolean(EXTRA_SWITCHED_USER, false)) {
            val newUser = extras.getString(EXTRA_SWITCHED_TO_USER)
            if (!newUser.isNullOrEmpty()) {
                switchAccountProcedure(newUser)
            } else {
                onSwitchedAccounts()
            }
        }
    }

    override fun secureContent(): Boolean = true

    private val setupUpLimitReachedObserver = Observer { limitReached: Event<Boolean> ->
        // val _limitReached = limitReached.getContentIfNotHandled()
        if (limitReached.getContentIfNotHandled() == true /* _limitReached != null */) {
            if (storageLimitApproachingAlertDialog != null) {
                storageLimitApproachingAlertDialog!!.dismiss()
                storageLimitApproachingAlertDialog = null
            }
            if (mUserManager.canShowStorageLimitReached()) {
                showInfoDialogWithTwoButtons(
                    this@MailboxActivity,
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
                        mUserManager.setShowStorageLimitReached(false)
                    },
                    {
                        mUserManager.setShowStorageLimitReached(false)
                    },
                    true
                )
            }
            mUserManager.setShowStorageLimitWarning(true)
            storageLimitAlert.visibility = View.VISIBLE
            storageLimitAlert.setIcon(getDrawable(R.drawable.inbox)!!)
            storageLimitAlert.setText(getString(R.string.storage_limit_alert))
        }
    }

    private val setupUpLimitApproachingObserver = Observer { limitApproaching: Event<Boolean> ->
        // val _limitApproaching = limitApproaching.getContentIfNotHandled()

        if (limitApproaching.getContentIfNotHandled() == true /* _limitApproaching != null */) {
            if (mUserManager.canShowStorageLimitWarning()) {
                if (storageLimitApproachingAlertDialog == null || !storageLimitApproachingAlertDialog!!.isShowing) {
                    // This is the first time the dialog is going to be showed or
                    // the dialog is not showing and had previously been dismissed by clicking the positive
                    // or negative button or the dialog is not showing and had previously been dismissed on touch
                    // outside or by clicking the back button
                    showStorageLimitApproachingAlertDialog()
                }
            }
            mUserManager.setShowStorageLimitReached(true)
            storageLimitAlert.visibility = View.GONE
        }
    }

    private val setupUpLimitBelowCriticalObserver = Observer { limitReached: Event<Boolean> ->
        // val _limitReached = limitReached.getContentIfNotHandled()
        if (limitReached.getContentIfNotHandled() == true /* _limitReached != null */) {
            mUserManager.setShowStorageLimitWarning(true)
            mUserManager.setShowStorageLimitReached(true)
            storageLimitAlert.visibility = View.GONE
        }
    }

    private val setupUpLimitReachedTryComposeObserver = Observer { limitReached: Event<Boolean> ->
        // val _limitReached = limitReached.getContentIfNotHandled()
        if (limitReached.getContentIfNotHandled() == true /* _limitReached != null && _limitReached */) {
            showInfoDialogWithTwoButtons(
                this@MailboxActivity,
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
            val intent = AppUtil.decorInAppIntent(
                Intent(
                    this@MailboxActivity,
                    ComposeMessageActivity::class.java
                )
            )
            startActivityForResult(intent, REQUEST_CODE_COMPOSE_MESSAGE)
        }
    }

    private val selectedMessages: List<SimpleMessage>
        get() = messagesAdapter.checkedMessages.map { SimpleMessage(it) }

    private var firstLogin: Boolean? = null

    override fun enableScreenshotProtector() {
        screenShotPreventer.visibility = View.VISIBLE
    }

    override fun disableScreenshotProtector() {
        screenShotPreventer.visibility = View.GONE
    }

    private fun startObserving() {
        mailboxViewModel.usedSpaceActionEvent(FLOW_START_ACTIVITY)
        mailboxViewModel.manageLimitReachedWarning.observe(this, setupUpLimitReachedObserver)
        mailboxViewModel.manageLimitApproachingWarning.observe(this, setupUpLimitApproachingObserver)
        mailboxViewModel.manageLimitBelowCritical.observe(this, setupUpLimitBelowCriticalObserver)
        mailboxViewModel.manageLimitReachedWarningOnTryCompose.observe(this, setupUpLimitReachedTryComposeObserver)
    }

    private fun startObservingUsedSpace() {
        liveSharedPreferences?.removeObservers(this)
        liveSharedPreferences = LiveSharedPreferences(
            app.getSecureSharedPreferences(mUserManager.username) as SecureSharedPreferences,
            Constants.Prefs.PREF_USED_SPACE
        )
        liveSharedPreferences!!.observe(
            this,
            { mailboxViewModel.usedSpaceActionEvent(FLOW_USED_SPACE_CHANGED) }
        )
    }

    private fun showStorageLimitApproachingAlertDialog() {
        storageLimitApproachingAlertDialog = showInfoDialogWithTwoButtons(
            this@MailboxActivity,
            getString(R.string.storage_limit_warning_title),
            getString(R.string.storage_limit_approaching_text),
            getString(R.string.dont_remind_again),
            getString(R.string.okay),
            {
                mUserManager.setShowStorageLimitWarning(false)
                storageLimitApproachingAlertDialog = null
            },
            { storageLimitApproachingAlertDialog = null },
            true
        )
    }

    private fun getLiveDataByLocation(
        messageDetailsRepository: MessageDetailsRepository,
        mMailboxLocation: MessageLocationType
    ): LiveData<List<Message>> {
        return when (mMailboxLocation) {
            MessageLocationType.STARRED -> messageDetailsRepository.getStarredMessagesAsync()
            MessageLocationType.LABEL,
            MessageLocationType.LABEL_OFFLINE,
            MessageLocationType.LABEL_FOLDER -> messageDetailsRepository.getMessagesByLabelIdAsync(mailboxLabelId!!)
            MessageLocationType.DRAFT,
            MessageLocationType.SENT,
            MessageLocationType.ARCHIVE,
            MessageLocationType.INBOX,
            MessageLocationType.SEARCH,
            MessageLocationType.SPAM,
            MessageLocationType.TRASH ->
                messageDetailsRepository.getMessagesByLocationAsync(mMailboxLocation.messageLocationTypeValue)
            MessageLocationType.ALL_MAIL -> messageDetailsRepository.getAllMessages()
            MessageLocationType.INVALID -> throw IllegalArgumentException("Invalid location.")
            else -> throw IllegalArgumentException("Unknown location: $mMailboxLocation")
        }
    }

    private fun startObservingPendingActions() {
        val owner = this
        mailboxViewModel.run {
            pendingSendsLiveData.removeObservers(owner)
            pendingUploadsLiveData.removeObservers(owner)
            reloadDependenciesForUser()
            pendingSendsLiveData.observe(owner) { messagesAdapter.setPendingForSendingList(it) }
            pendingUploadsLiveData.observe(owner) { messagesAdapter.setPendingUploadsList(it) }

        }
    }

    override fun onSwitchedAccounts() {
        val username = mUserManager.username
        mJobManager.start()
        countersDatabase = CountersDatabaseFactory.getInstance(this).getDatabase()
        pendingActionsDatabase = PendingActionsDatabaseFactory.getInstance(this).getDatabase()
        messageDetailsRepository.reloadDependenciesForUser(username)
        startObservingPendingActions()
        AppUtil.clearNotifications(this, username)
        lazyManager.reset()
        setUpDrawer()
        setupAccountsList()
        checkRegistration()
        handler.postDelayed(
            {
                mJobManager.addJobInBackground(FetchLabelsJob())
                setupNewMessageLocation(DrawerOptionType.INBOX.drawerOptionTypeValue)
            },
            500
        )

        val messagesLiveData =
            mailboxLocationMain.switchMap { getLiveDataByLocation(messageDetailsRepository, it) }
        messagesLiveData.observe(this, MessagesListObserver(messagesAdapter))
        messageDetailsRepository.getAllLabels().observe(
            this,
            { labels: List<Label> ->
                messagesAdapter.setLabels(labels)
            }
        )
        // Account has been switched, so used space changed as well
        mailboxViewModel.usedSpaceActionEvent(FLOW_USED_SPACE_CHANGED)
        // Observe used space for current account
        startObservingUsedSpace()

        // manually update the flags for preventing screenshots
        if (isPreventingScreenshots || mUserManager.user.isPreventTakingScreenshots) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    private inner class MessagesListObserver(
        private val adapter: MessagesRecyclerViewAdapter?
    ) : Observer<List<Message>> {
        override fun onChanged(messages: List<Message>) {
            adapter!!.clear()
            adapter.addAll(messages)
        }
    }

    private val listScrollListener: RecyclerView.OnScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(view: RecyclerView, scrollState: Int) {
            scrollStateChanged =
                scrollState == RecyclerView.SCROLL_STATE_DRAGGING || scrollState == RecyclerView.SCROLL_STATE_SETTLING
        }

        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (!scrollStateChanged) {
                return
            }
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager?
            val adapter = recyclerView.adapter
            val lastVisibleItem = layoutManager!!.findLastVisibleItemPosition()
            val lastPosition = adapter!!.itemCount - 1
            if (lastVisibleItem == lastPosition && dy > 0 && !setLoadingMore(true)) {
                loadMoreMessages()
            }
        }

        private fun loadMoreMessages() {
            val mailboxLocation = mailboxLocationMain.value ?: MessageLocationType.INBOX
            val earliestTime = getLastMessageTime(mailboxLocation, mailboxLabelId)
            if (mailboxLocation != MessageLocationType.LABEL && mailboxLocation != MessageLocationType.LABEL_FOLDER) {
                startFetchMessages(mailboxLocation, earliestTime)
            } else {
                startFetchMessagesByLabel(mailboxLocation, earliestTime, mailboxLabelId ?: "")
            }
        }
    }

    private fun registerFcmReceiver() {
        val filter = IntentFilter(getString(R.string.action_notification))
        filter.priority = 2
        LocalBroadcastManager.getInstance(this).registerReceiver(fcmBroadcastReceiver, filter)
    }

    private fun onConnectivityCheckRetry() {
        mConnectivitySnackLayout?.let {
            networkSnackBarUtil.getCheckingConnectionSnackBar(it).show()
        }
        syncUUID = UUID.randomUUID().toString()
        handler.postDelayed(FetchMessagesRetryRunnable(this), 3.seconds.toLongMilliseconds())
        mailboxViewModel.checkConnectivityDelayed()
    }

    private fun checkRegistration() {
        // Check device for Play Services APK.
        if (checkPlayServices()) {
            val tokenSent = FcmUtil.isTokenSent()
            if (!tokenSent) {
                try {
                    FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            task.result?.let { result ->
                                FcmUtil.setFirebaseToken(result.token)
                                pmRegistrationWorkerEnqueuer()
                            }
                        } else {
                            Timber.e(task.exception, "Could not retrieve FirebaseInstanceId")
                        }
                    }
                } catch (exc: IllegalArgumentException) {
                    Toast.makeText(this, R.string.invalid_firebase_api_key_message, Toast.LENGTH_LONG).show()
                    Timber.e(exc, getString(R.string.invalid_firebase_api_key_message))
                }
            }
        }
    }

    private fun checkUserAndFetchNews(): Boolean {
        syncUUID = UUID.randomUUID().toString()
        if (mUserManager.isBackgroundSyncEnabled) {
            setRefreshing(true)
            layout_sync.visibility = View.VISIBLE
        }
        if (firstLogin == null) {
            firstLogin = intent.getBooleanExtra(EXTRA_FIRST_LOGIN, false)
        }
        return if (!firstLogin!!) {
            val alarmReceiver = AlarmReceiver()
            alarmReceiver.setAlarm(this, true)
            false
        } else {
            firstLogin = false
            refreshMailboxJobRunning = true
            app.updateDone()
            mJobManager.addJobInBackground(
                FetchByLocationJob(
                    mailboxLocationMain.value ?: MessageLocationType.INVALID,
                    mailboxLabelId,
                    false,
                    syncUUID,
                    false
                )
            )
            true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // force reload of MessageDetailsRepository's internal dependencies in case we just switched user
        if (intent.extras != null && intent.extras!!.containsKey(EXTRA_SWITCHED_TO_USER)) {
            switchAccountProcedure(intent.getStringExtra(EXTRA_SWITCHED_TO_USER)!!)
        } else if (intent.getBooleanExtra(EXTRA_SWITCHED_USER, false)) {
            onSwitchedAccounts()
        } else if (intent.getBooleanExtra(EXTRA_LOGOUT, false)) {
            onLogout()
        } else if (intent.extras != null && intent.extras!!.containsKey(EXTRA_USERNAME)) {
            if (mUserManager.username != intent.getStringExtra(EXTRA_USERNAME)) {
                switchAccountProcedure(intent.getStringExtra(EXTRA_USERNAME)!!)
            }
        } else {
            checkRegistration()
            checkUserAndFetchNews()
            setupNewMessageLocation(DrawerOptionType.INBOX.drawerOptionTypeValue)
        }
    }

    private fun shouldShowSwipeGesturesChangedDialog(): Boolean {
        val prefs: SharedPreferences = app.defaultSharedPreferences
        val previousVersion: Int = prefs.getInt(Constants.Prefs.PREF_PREVIOUS_APP_VERSION, Int.MIN_VALUE)
        // The dialog should be shown once on the update when swiping gestures are switched
        return previousVersion in 1 until SWIPE_GESTURES_CHANGED_VERSION &&
            !prefs.getBoolean(PREF_SWIPE_GESTURES_DIALOG_SHOWN, false)
    }


    private fun showSwipeGesturesChangedDialog() {
        val prefs: SharedPreferences = (applicationContext as ProtonMailApplication).defaultSharedPreferences
        showInfoDialogWithTwoButtons(
            this,
            getString(R.string.swipe_gestures_changed),
            getString(R.string.swipe_gestures_changed_message),
            getString(R.string.go_to_settings),
            getString(R.string.okay),
            {
                val swipeGestureIntent = Intent(
                    this,
                    EditSettingsItemActivity::class.java
                )
                swipeGestureIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.SWIPE)
                startActivityForResult(
                    AppUtil.decorInAppIntent(swipeGestureIntent),
                    SettingsEnum.SWIPING_GESTURE.ordinal
                )
            },
            { },
            cancelable = true,
            dismissible = true,
            outsideClickCancellable = true
        )
        prefs.edit().putBoolean(PREF_SWIPE_GESTURES_DIALOG_SHOWN, true).apply()
    }

    override fun onResume() {
        super.onResume()
        if (!mUserManager.isLoggedIn) {
            return
        }
        reloadMessageCounts()
        registerFcmReceiver()
        checkDelinquency()
        no_messages_layout.visibility = View.GONE
        mailboxViewModel.checkConnectivity()
        checkForDraftedMessages()
        val mailboxLocation = mailboxLocationMain.value
        if (mailboxLocation == MessageLocationType.INBOX) {
            AppUtil.clearNotifications(this, mUserManager.username)
        }
        if (mailboxLocation == MessageLocationType.ALL_DRAFT || mailboxLocation == MessageLocationType.DRAFT) {
            AppUtil.clearSendingFailedNotifications(this, mUserManager.username)
        }
        setUpDrawer()
        closeDrawer(true)

        if (shouldShowSwipeGesturesChangedDialog()) {
            showSwipeGesturesChangedDialog()
        }
    }

    override fun onPause() {
        runCatching {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(fcmBroadcastReceiver)
            unregisterReceiver(showDraftedSnackBroadcastReceiver)
        }
        networkSnackBarUtil.hideAllSnackBars()
        super.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(
            STATE_MAILBOX_LOCATION,
            if (mailboxLocationMain.value != null) {
                mailboxLocationMain.value!!.messageLocationTypeValue
            } else {
                MessageLocationType.INBOX.messageLocationTypeValue
            }
        )
        outState.putString(STATE_MAILBOX_LABEL_LOCATION, mailboxLabelId)
        outState.putString(STATE_MAILBOX_LABEL_LOCATION_NAME, mailboxLabelName)
        super.onSaveInstanceState(outState)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.mailbox_options_menu, menu)
        val mailboxLocation = mailboxLocationMain.value
        menu.findItem(R.id.empty).isVisible =
            mailboxLocation in listOf(MessageLocationType.DRAFT, MessageLocationType.SPAM, MessageLocationType.TRASH)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.mailbox_options_menu, menu)
        val mailboxLocation = mailboxLocationMain.value
        menu.findItem(R.id.empty).isVisible =
            mailboxLocation in listOf(
            MessageLocationType.DRAFT,
            MessageLocationType.SPAM,
            MessageLocationType.TRASH,
            MessageLocationType.LABEL,
            MessageLocationType.LABEL_FOLDER
        )
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.search -> {
                val intent = AppUtil.decorInAppIntent(
                    Intent(
                        this@MailboxActivity,
                        SearchActivity::class.java
                    )
                )
                startActivity(intent)
                true
            }
            R.id.compose -> {
                mailboxViewModel.usedSpaceActionEvent(FLOW_TRY_COMPOSE)
                true
            }
            R.id.empty -> {
                val clickListener = DialogInterface.OnClickListener { dialog: DialogInterface, which: Int ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        setRefreshing(true)
                        mJobManager.addJobInBackground(EmptyFolderJob(mailboxLocationMain.value, this.mailboxLabelId))
                        setLoadingMore(false)
                    }
                    dialog.dismiss()
                }
                if (!isFinishing) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.empty_folder)
                        .setMessage(R.string.are_you_sure_empty)
                        .setNegativeButton(R.string.no, clickListener)
                        .setPositiveButton(R.string.yes, clickListener)
                        .create()
                        .show()
                }
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.syncState()
    }

    override fun onBackPressed() {
        saveLastInteraction()
        val drawerClosed = closeDrawer()
        if (!drawerClosed && mailboxLocationMain.value != MessageLocationType.INBOX) {
            setupNewMessageLocation(DrawerOptionType.INBOX.drawerOptionTypeValue)
        } else if (!drawerClosed) {
            finish()
        }
    }

    private fun initializeSwipeRefreshLayout(swipeRefreshLayoutAux: SwipeRefreshLayout) {
        swipeRefreshLayoutAux.setColorSchemeResources(R.color.ultramarine_blue, R.color.lake_blue)
        swipeRefreshLayoutAux.setOnRefreshListener(this)
    }

    fun setRefreshing(shouldRefresh: Boolean) {
        Timber.v("setRefreshing shouldRefresh:$shouldRefresh")
        swipe_refresh_layout.isRefreshing = shouldRefresh
        spinner_layout.isRefreshing = shouldRefresh
        no_messages_layout.isRefreshing = shouldRefresh
        spinner_layout.visibility = if (shouldRefresh) View.VISIBLE else View.GONE
    }

    private fun setLoadingMore(loadingMore: Boolean): Boolean {
        val previousValue = isLoadingMore.getAndSet(loadingMore)
        messages_list_view.post { messagesAdapter.includeFooter = isLoadingMore.get() }
        return previousValue
    }

    @Subscribe
    fun onSwitchedAccountEvent(event: ForceSwitchedAccountEvent) {
        showSignedInSnack(
            messages_list_view,
            String.format(getString(R.string.signed_in_with_logged_out_from), event.fromAccount, event.toAccount)
        )
        onSwitchedAccounts()
    }

    @Subscribe
    fun onMailSettingsEvent(event: MailSettingsEvent?) {
        loadMailSettings()
    }

    override fun onLogout() {
        onLogoutEvent(LogoutEvent(Status.SUCCESS))
    }

    override fun onInbox(type: DrawerOptionType) {
        AppUtil.clearNotifications(applicationContext, mUserManager.username)
        setupNewMessageLocation(type.drawerOptionTypeValue)
    }

    override fun onOtherMailBox(type: DrawerOptionType) {
        setupNewMessageLocation(type.drawerOptionTypeValue)
    }

    public override fun onLabelMailBox(type: DrawerOptionType, labelId: String, labelName: String, isFolder: Boolean) {
        setupNewMessageLocation(type.drawerOptionTypeValue, labelId, labelName, isFolder)
    }

    override val currentMailboxLocation: MessageLocationType
        get() = if (mailboxLocationMain.value != null) {
            mailboxLocationMain.value!!
        } else {
            MessageLocationType.INBOX
        }

    private fun setTitle() {
        val titleRes: Int = when (mailboxLocationMain.value) {
            MessageLocationType.INBOX -> R.string.inbox_option
            MessageLocationType.STARRED -> R.string.starred_option
            MessageLocationType.DRAFT -> R.string.drafts_option
            MessageLocationType.SENT -> R.string.sent_option
            MessageLocationType.ARCHIVE -> R.string.archive_option
            MessageLocationType.TRASH -> R.string.trash_option
            MessageLocationType.SPAM -> R.string.spam_option
            MessageLocationType.ALL_MAIL -> R.string.allmail_option
            else -> R.string.app_name
        }
        supportActionBar!!.setTitle(titleRes)
    }

    private fun showNoConnSnackAndScheduleRetry(connectivity: Constants.ConnectionState) {
        Timber.v("show NoConnection Snackbar ${mConnectivitySnackLayout != null}")
        mConnectivitySnackLayout?.let {
            networkSnackBarUtil.getNoConnectionSnackBar(
                parentView = it,
                user = mUserManager.user,
                netConfiguratorCallback = this,
                onRetryClick = { onConnectivityCheckRetry() },
                isOffline = connectivity == Constants.ConnectionState.NO_INTERNET
            ).show()
        }
    }

    private fun hideNoConnSnack() {
        Timber.v("hideNoConnSnack")
        networkSnackBarUtil.hideCheckingConnectionSnackBar()
        networkSnackBarUtil.hideNoConnectionSnackBar()
    }

    @Subscribe
    fun onMailboxLoginEvent(event: MailboxLoginEvent?) {
        if (event == null) {
            return
        }
        app.resetMailboxLoginEvent()
        when (event.status) {
            AuthStatus.INVALID_CREDENTIAL -> {
                showToast(R.string.invalid_mailbox_password, Toast.LENGTH_SHORT)
                startActivity(AppUtil.decorInAppIntent(Intent(this, MailboxLoginActivity::class.java)))
                finish()
            }
            else -> {
                mUserManager.isLoggedIn = true
            }
        }
    }

    @Subscribe
    fun onSettingsChangedEvent(event: SettingsChangedEvent) {
        val user = mUserManager.user
        if (event.status == AuthStatus.SUCCESS) {
            refreshDrawerHeader(user)
        } else {
            when (event.status) {
                AuthStatus.INVALID_CREDENTIAL -> {
                    showToast(R.string.settings_not_saved_password, Toast.LENGTH_SHORT, Gravity.CENTER)
                }
                AuthStatus.INVALID_SERVER_PROOF -> {
                    showToast(R.string.invalid_server_proof, Toast.LENGTH_SHORT, Gravity.CENTER)
                }
                AuthStatus.FAILED -> {
                    if (event.oldEmail != null) {
                        showToast(R.string.settings_not_saved_email, Toast.LENGTH_SHORT, Gravity.CENTER)
                    } else {
                        showToast(R.string.saving_failed_no_conn, Toast.LENGTH_LONG, Gravity.CENTER)
                    }
                }
                else -> {
                    if (event.oldEmail != null) {
                        showToast(R.string.settings_not_saved_email, Toast.LENGTH_SHORT, Gravity.CENTER)
                    } else {
                        showToast(R.string.saving_failed_no_conn, Toast.LENGTH_LONG, Gravity.CENTER)
                    }
                }
            }
        }
    }

    @Subscribe
    fun onLogoutEvent(event: LogoutEvent) {
        if (overlayDialog != null) {
            overlayDialog!!.dismiss()
            overlayDialog = null
        }
        if (event.status == Status.NO_NETWORK) {
            showToast(R.string.no_network, Toast.LENGTH_SHORT)
        }

        // destroy loader as database will be deleted on logout
        LoaderManager.getInstance(this).run {
            destroyLoader(LOADER_ID)
            destroyLoader(LOADER_ID_LABELS_OFFLINE)
        }
        messagesAdapter.clear()
        startActivity(AppUtil.decorInAppIntent(Intent(this, LoginActivity::class.java)))
        finish()
    }

    @Subscribe
    fun onMailboxLoaded(event: MailboxLoadedEvent?) {
        Timber.v("Mailbox loaded status ${event?.status}")
        if (event == null || (event.uuid != null && event.uuid != syncUUID)) {
            return
        }
        refreshMailboxJobRunning = false
        handler.postDelayed(SyncDoneRunnable(this), 1000)
        setLoadingMore(false)
        if (!isDohOngoing) {
            showToast(event.status)
        }
        val mailboxLocation = mailboxLocationMain.value
        val setOfLabels =
            setOf(
                MessageLocationType.LABEL,
                MessageLocationType.LABEL_FOLDER,
                MessageLocationType.LABEL_OFFLINE
            )
        if (event.status == Status.NO_NETWORK && setOfLabels.any { it == mailboxLocation }) {
            mailboxLocationMain.value = MessageLocationType.LABEL_OFFLINE
        }
        if (event.status == Status.FAILED && event.errorMessage.isNotEmpty()) {
            showToast(event.errorMessage, Toast.LENGTH_LONG)
        }
        mNetworkResults.setMailboxLoaded(MailboxLoadedEvent(Status.SUCCESS, null))
        setRefreshing(false)
    }

    private fun onConnectivityEvent(connectivity: Constants.ConnectionState) {
        Timber.v("onConnectivityEvent hasConnection: ${connectivity.name}")
        if (!isDohOngoing) {
            Timber.d("DoH NOT ongoing showing UI")
            if (connectivity != Constants.ConnectionState.CONNECTED) {
                setRefreshing(false)
                showNoConnSnackAndScheduleRetry(connectivity)
            } else {
                hideNoConnSnack()
            }
        } else {
            Timber.d("DoH ongoing, not showing UI")
        }
    }

    @Subscribe
    fun onMailboxNoMessages(event: MailboxNoMessagesEvent?) {
        // show toast only if user initiated load more
        handler.postDelayed(SyncDoneRunnable(this), 300)
        if (isLoadingMore.get()) {
            showToast(R.string.no_more_messages, Toast.LENGTH_SHORT)
            messagesAdapter.notifyDataSetChanged()
        }
        setLoadingMore(false)
    }

    @Subscribe
    fun onUpdatesLoaded(event: FetchUpdatesEvent?) {
        syncingDone()
        refreshDrawerHeader(mUserManager.user)
        handler.postDelayed(SyncDoneRunnable(this), 1000)
    }

    private fun showToast(status: Status) {
        when (status) {
            Status.UNAUTHORIZED -> {
                showNoConnSnackAndScheduleRetry(Constants.ConnectionState.CANT_REACH_SERVER)
            }
            Status.NO_NETWORK -> {
                showNoConnSnackAndScheduleRetry(Constants.ConnectionState.NO_INTERNET)
            }
            Status.SUCCESS -> {
                hideNoConnSnack()
            }
            else -> {
                return
            }
        }
    }

    @Subscribe
    fun onParentEvent(event: ParentEvent?) {
        OnParentEventTask(messageDetailsRepository, messagesAdapter, event!!).execute()
    }

    @Subscribe
    fun onLabelsLoadedEvent(event: FetchLabelsEvent) {
        if (/* messagesAdapter != null && */ event.status == Status.SUCCESS) {
            messagesAdapter.notifyDataSetChanged()
        }
    }

    @Subscribe
    fun onMessageCountsEvent(event: MessageCountsEvent) {
        //region old total count
        if (event.status != Status.SUCCESS) {
            refreshDrawer()
            return
        }
        val response = event.unreadMessagesResponse ?: return
        val messageCountsList = response.counts ?: emptyList()
        countersDatabase = CountersDatabaseFactory.getInstance(applicationContext, mUserManager.username).getDatabase()
        OnMessageCountsListTask(WeakReference(this), countersDatabase, messageCountsList).execute()
        refreshDrawer()
        //endregion
    }

    fun refreshEmptyView(count: Int) {
        if (count == 0) {
            spinner_layout.visibility = View.GONE
            no_messages_layout.visibility = View.VISIBLE
            swipe_refresh_layout.visibility = View.GONE
            swipe_refresh_wrapper.visibility = View.GONE
        } else {
            spinner_layout.visibility = View.VISIBLE
            no_messages_layout.visibility = View.GONE
            swipe_refresh_layout.visibility = View.VISIBLE
            swipe_refresh_wrapper.visibility = View.VISIBLE
        }
    }

    /* AbsListView.MultiChoiceModeListener */
    override fun onItemCheckedStateChanged(mode: ActionMode, position: Int, id: Long, checked: Boolean) {
        val checkedItems = messagesAdapter.checkedMessages.size
        // on many devices there is a strange UnknownFormatConversionException:
        // "Conversion: D" if using string formatting, which is probably a memory corruption issue
        mode.title = "$checkedItems ${getString(R.string.selected)}"
        if (checkedItems == 1) {
            mode.invalidate()
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        actionMode = mode
        UiUtil.setStatusBarColor(
            this,
            UiUtil.scaleColor(ContextCompat.getColor(this, R.color.dark_purple_statusbar), 1f, true)
        )
        mode.menuInflater.inflate(R.menu.message_selection_menu, menu)
        menu.findItem(R.id.move_to_trash).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.findItem(R.id.delete_message).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.findItem(R.id.add_star).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.findItem(R.id.remove_star).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        menu.findItem(R.id.mark_unread).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.findItem(R.id.mark_read).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.findItem(R.id.move_to_archive).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        menu.findItem(R.id.add_label).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        menu.findItem(R.id.add_folder).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        val moveToInbox = menu.findItem(R.id.move_to_inbox)
        val mailboxLocation = mailboxLocationMain.value

        val listOfLocationTypes =
            listOf(
                MessageLocationType.TRASH,
                MessageLocationType.SPAM,
                MessageLocationType.ARCHIVE
            )
        if (mailboxLocation in listOfLocationTypes) {
            moveToInbox.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        } else {
            menu.removeItem(moveToInbox.itemId)
        }
        menu.findItem(R.id.move_to_spam).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        swipe_refresh_layout.isEnabled = false
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        val messages = selectedMessages
        val mailboxLocation = mailboxLocationMain.value
        if (messages.size == 1) {
            val message = messages[0]
            menu.findItem(R.id.move_to_trash).isVisible =
                mailboxLocation != MessageLocationType.TRASH && mailboxLocation != MessageLocationType.DRAFT
            menu.findItem(R.id.delete_message).isVisible =
                mailboxLocation == MessageLocationType.TRASH || mailboxLocation == MessageLocationType.DRAFT
            menu.findItem(R.id.add_star).isVisible = !message.isStarred
            menu.findItem(R.id.remove_star).isVisible = message.isStarred
            menu.findItem(R.id.mark_read).isVisible = !message.isRead && mailboxLocation != MessageLocationType.DRAFT
            menu.findItem(R.id.mark_unread).isVisible = message.isRead && mailboxLocation != MessageLocationType.DRAFT
            menu.findItem(R.id.move_to_archive).isVisible = mailboxLocation != MessageLocationType.ARCHIVE
            val moveToInbox = menu.findItem(R.id.move_to_inbox)
            if (moveToInbox != null) {
                moveToInbox.isVisible = mailboxLocation != MessageLocationType.INBOX
            }
            menu.findItem(R.id.move_to_spam).isVisible = mailboxLocation != MessageLocationType.SPAM
            menu.findItem(R.id.add_label).isVisible = true
            menu.findItem(R.id.add_folder).isVisible = true
        } else {
            menu.findItem(R.id.move_to_trash).isVisible =
                mailboxLocation != MessageLocationType.TRASH && mailboxLocation != MessageLocationType.DRAFT
            menu.findItem(R.id.delete_message).isVisible =
                mailboxLocation == MessageLocationType.TRASH || mailboxLocation == MessageLocationType.DRAFT
            if (containsUnstar(messages)) menu.findItem(R.id.add_star).isVisible = true
            if (containsStar(messages)) menu.findItem(R.id.remove_star).isVisible = true
            val markReadItem = menu.findItem(R.id.mark_read)
            if (MessageUtils.areAllRead(messages)) {
                markReadItem.isVisible = false
            } else {
                markReadItem.isVisible = mailboxLocation != MessageLocationType.DRAFT
            }
            val markUnreadItem = menu.findItem(R.id.mark_unread)
            if (MessageUtils.areAllUnRead(messages)) {
                markUnreadItem.isVisible = false
            } else {
                markUnreadItem.isVisible = mailboxLocation != MessageLocationType.DRAFT
            }
            menu.findItem(R.id.move_to_archive).isVisible = mailboxLocation != MessageLocationType.ARCHIVE
            val moveToInbox = menu.findItem(R.id.move_to_inbox)
            if (moveToInbox != null) {
                moveToInbox.isVisible = mailboxLocation != MessageLocationType.INBOX
            }
            menu.findItem(R.id.move_to_spam).isVisible = mailboxLocation != MessageLocationType.SPAM
            menu.findItem(R.id.add_label).isVisible = true
            menu.findItem(R.id.add_folder).isVisible = true
        }
        return true
    }

    private fun containsStar(messages: List<SimpleMessage>): Boolean = messages.any { it.isStarred }

    private fun containsUnstar(messages: List<SimpleMessage>): Boolean = messages.any { !it.isStarred }

    override fun move(folderId: String) {
        MessageUtils.moveMessage(this, mJobManager, folderId, mutableListOf(mailboxLabelId), selectedMessages)
        if (actionModeRunnable != null) {
            actionModeRunnable!!.run()
        }
    }

    override fun showFoldersManager() {
        val foldersManagerIntent = Intent(this, LabelsManagerActivity::class.java)
        foldersManagerIntent.putExtra(EXTRA_MANAGE_FOLDERS, true)
        foldersManagerIntent.putExtra(EXTRA_POPUP_STYLE, true)
        foldersManagerIntent.putExtra(EXTRA_CREATE_ONLY, true)
        startActivity(AppUtil.decorInAppIntent(foldersManagerIntent))
    }

    override fun onDismiss(dialog: DialogInterface) {
        catchLabelEvents = true
    }

    internal inner class ActionModeInteractionRunnable(private val actionModeAux: ActionMode?) : Runnable {
        override fun run() {
            actionModeAux?.finish()
        }
    }

    private var actionModeRunnable: ActionModeInteractionRunnable? = null
    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        val messageIds = selectedMessages.map { message -> message.messageId }
        val menuItemId = menuItem.itemId
        var job: Job? = null
        when (menuItemId) {
            R.id.move_to_trash -> {
                job = PostTrashJobV2(messageIds, mailboxLabelId)
                undoSnack = showUndoSnackbar(
                    this@MailboxActivity,
                    findViewById(R.id.drawer_layout),
                    resources.getQuantityString(R.plurals.action_move_to_trash, messageIds.size),
                    { },
                    false
                )
                undoSnack!!.show()
            }
            R.id.delete_message ->
                showDeleteConfirmationDialog(
                    this,
                    getString(R.string.delete_messages),
                    getString(R.string.confirm_destructive_action)
                ) {
                    mailboxViewModel.deleteMessages(messageIds)
                    mode.finish()
                }
            R.id.mark_read -> job = PostReadJob(messageIds)
            R.id.mark_unread -> job = PostUnreadJob(messageIds)
            R.id.add_star -> job = PostStarJob(messageIds)
            R.id.add_label -> {
                actionModeRunnable = ActionModeInteractionRunnable(mode)
                ShowLabelsManagerDialogTask(supportFragmentManager, messageDetailsRepository, messageIds).execute()
            }
            R.id.add_folder -> {
                actionModeRunnable = ActionModeInteractionRunnable(mode)
                showFoldersManagerDialog(messageIds)
            }
            R.id.remove_star -> job = PostUnstarJob(messageIds)
            R.id.move_to_archive -> {
                job = PostArchiveJob(messageIds)
                undoSnack = showUndoSnackbar(
                    this@MailboxActivity,
                    findViewById(R.id.drawer_layout),
                    resources.getQuantityString(R.plurals.action_move_to_archive, messageIds.size),
                    { },
                    false
                )
                undoSnack!!.show()
            }
            R.id.move_to_inbox -> job = PostInboxJob(messageIds, listOf(mailboxLabelId))
            R.id.move_to_spam -> {
                job = PostSpamJob(messageIds)
                undoSnack = showUndoSnackbar(
                    this@MailboxActivity, findViewById(R.id.drawer_layout),
                    resources.getQuantityString(R.plurals.action_move_to_spam, messageIds.size),
                    { },
                    false
                )
                undoSnack!!.show()
            }
        }
        if (job != null) {
            // show progress bar for visual representation of work in background,
            // if all the messages inside the folder are impacted by the action
            if (messagesAdapter.itemCount == messageIds.size) {
                setRefreshing(true)
            }
            mJobManager.addJobInBackground(job)
        }

        if (menuItemId !in listOf(R.id.add_label, R.id.add_folder, R.id.delete_message)) {
            mode.finish()
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        swipe_refresh_layout.isEnabled = true
        messagesAdapter.endSelectionMode()
        UiUtil.setStatusBarColor(this, ContextCompat.getColor(this, R.color.dark_purple_statusbar))
    }

    private fun showFoldersManagerDialog(messageIds: List<String>) {
        // show progress bar for visual representation of work in background,
        // if all the messages inside the folder are impacted by the action
        if (messagesAdapter.itemCount == messageIds.size) {
            setRefreshing(true)
        }

        catchLabelEvents = false
        val moveToFolderDialogFragment = MoveToFolderDialogFragment.newInstance(mailboxLocationMain.value)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(moveToFolderDialogFragment, moveToFolderDialogFragment.fragmentKey)
        transaction.commitAllowingStateLoss()
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
                    val errorMessage = it.outputData.getString(KEY_POST_LABEL_WORKER_RESULT_ERROR)
                        ?: getString(R.string.label_invalid)
                    showToast(errorMessage, Toast.LENGTH_SHORT)
                }
            }
        )
    }

    override fun onLabelsDeleted(checkedLabelIds: List<String>) {
        // NOOP
    }

    override fun onLabelsChecked(
        checkedLabelIds: List<String>,
        unchangedLabelss: List<String>,
        messageIds: List<String>
    ) {
        var unchangedLabels: List<String>? = unchangedLabelss
        if (actionModeRunnable != null) {
            actionModeRunnable!!.run()
        }
        if (unchangedLabels == null) {
            unchangedLabels = mutableListOf()
        }
        mailboxViewModel.processLabels(messageIds, checkedLabelIds, unchangedLabels)
    }

    @Subscribe
    fun onRefreshDrawer(event: RefreshDrawerEvent?) {
        refreshDrawer()
    }

    override fun onLabelsChecked(
        checkedLabelIds: List<String>,
        unchangedLabels: List<String>,
        messageIds: List<String>,
        messagesToArchive: List<String>
    ) {
        mJobManager.addJobInBackground(PostArchiveJob(messagesToArchive))
        onLabelsChecked(checkedLabelIds, unchangedLabels, messageIds)
    }

    /* SwipeRefreshLayout.OnRefreshListener */
    override fun onRefresh() {
        if (!spinner_layout.isRefreshing) {
            fetchUpdates(true)
        }
    }

    /**
     * Request messages reload.
     *
     * @param isRefreshMessagesRequired flag set to true refreshes all the messages and deletes mesage content in the DB
     */
    private fun fetchUpdates(isRefreshMessagesRequired: Boolean = false) {
        setRefreshing(true)
        syncUUID = UUID.randomUUID().toString()
        reloadMessageCounts()
        mJobManager.addJobInBackground(
            FetchByLocationJob(
                mailboxLocationMain.value ?: MessageLocationType.INVALID,
                mailboxLabelId,
                true,
                syncUUID,
                isRefreshMessagesRequired
            )
        )
    }

    private fun syncingDone() {
        layout_sync.visibility = View.GONE
    }

    private class SyncDoneRunnable internal constructor(activity: MailboxActivity) : Runnable {
        private val mailboxActivityWeakReference = WeakReference(activity)
        override fun run() {
            val mailboxActivity = mailboxActivityWeakReference.get()
            mailboxActivity?.syncingDone()
        }
    }

    private fun setupNewMessageLocation(newLocation: Int) {
        val newMessageLocationType = fromInt(newLocation)
        swipe_refresh_layout.visibility = View.VISIBLE
        swipe_refresh_layout.isRefreshing = true
        swipe_refresh_wrapper.visibility = View.VISIBLE
        LoaderManager.getInstance(this).destroyLoader(LOADER_ID_LABELS_OFFLINE)
        no_messages_layout.visibility = View.GONE
        spinner_layout.visibility = View.VISIBLE
        if (actionMode != null) {
            actionMode!!.finish()
        }
        mailboxLabelId = null
        invalidateOptionsMenu()
        mailboxLocationMain.value = newMessageLocationType
        setTitle()
        closeDrawer()
        messages_list_view.clearFocus()
        messages_list_view.scrollToPosition(0)
        if (newMessageLocationType == MessageLocationType.STARRED) {
            startFetchFirstPage(newMessageLocationType)
        } else {
            syncUUID = UUID.randomUUID().toString()
            mJobManager.addJobInBackground(
                FetchByLocationJob(
                    newMessageLocationType,
                    mailboxLabelId,
                    false,
                    syncUUID,
                    false
                )
            )
        }
        checkForDraftedMessages()
        RefreshEmptyViewTask(
            WeakReference(this),
            countersDatabase,
            messagesDatabase,
            newMessageLocationType,
            mailboxLabelId
        ).execute()
        if (newMessageLocationType == MessageLocationType.ALL_DRAFT ||
            newMessageLocationType == MessageLocationType.DRAFT
        ) {
            AppUtil.clearSendingFailedNotifications(this, mUserManager.username)
        }
    }

    // version for label views
    private fun setupNewMessageLocation(
        newLocation: Int,
        labelId: String,
        labelName: String?,
        isFolder: Boolean
    ) {
        SetUpNewMessageLocationTask(
            WeakReference(this),
            messageDetailsRepository,
            labelId,
            isFolder,
            newLocation,
            labelName
        ).execute()
    }

    private var undoSnack: Snackbar? = null
    private fun buildSwipeProcessor() {
        mSwipeProcessor.addHandler(SwipeAction.TRASH, TrashSwipeHandler())
        mSwipeProcessor.addHandler(SwipeAction.SPAM, SpamSwipeHandler())
        mSwipeProcessor.addHandler(SwipeAction.STAR, StarSwipeHandler())
        mSwipeProcessor.addHandler(SwipeAction.ARCHIVE, ArchiveSwipeHandler())
        mSwipeProcessor.addHandler(SwipeAction.MARK_READ, MarkReadSwipeHandler())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_TRASH_MESSAGE_DETAILS -> {
                    move_to_trash.visibility = View.VISIBLE
                    handler.postDelayed({ move_to_trash.visibility = View.GONE }, 1000)
                }
                REQUEST_CODE_VALIDATE_PIN -> {
                    if (data!!.hasExtra(EXTRA_TOTAL_COUNT_EVENT)) {
                        val totalCountEvent: Any? = data.getSerializableExtra(
                            EXTRA_TOTAL_COUNT_EVENT
                        )
                        if (totalCountEvent is MessageCountsEvent) {
                            onMessageCountsEvent(totalCountEvent)
                        }
                    }
                    super.onActivityResult(requestCode, resultCode, data)
                }
                REQUEST_CODE_SWITCHED_USER -> {
                    if (data!!.hasExtra(EXTRA_SWITCHED_USER)) {
                        onSwitchedAccounts()
                    }
                    super.onActivityResult(requestCode, resultCode, data)
                }
                else -> super.onActivityResult(requestCode, resultCode, data)
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun checkPlayServices(): Boolean {
        val googleAPI = GoogleApiAvailability.getInstance()
        val result = googleAPI.isGooglePlayServicesAvailable(this)
        if (result != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(result)) {
                val setOfConnectionResults =
                    setOf(
                        ConnectionResult.SERVICE_MISSING,
                        ConnectionResult.SERVICE_INVALID,
                        ConnectionResult.SERVICE_DISABLED,
                        ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED
                    )
                if (setOfConnectionResults.any { it == result }) {
                    val prefs = app.defaultSharedPreferences
                    val dontShowPlayServices = prefs.getBoolean(Constants.Prefs.PREF_DONT_SHOW_PLAY_SERVICES, false)
                    if (!dontShowPlayServices) {
                        showInfoDialogWithTwoButtons(
                            this,
                            getString(R.string.push_notifications_alert_title),
                            getString(R.string.push_notifications_alert_subtitle),
                            getString(R.string.dont_remind_again),
                            getString(R.string.okay),
                            { prefs.edit().putBoolean(Constants.Prefs.PREF_DONT_SHOW_PLAY_SERVICES, true).apply() },
                            { },
                            true
                        )
                    }
                } else {
                    googleAPI.getErrorDialog(
                        this,
                        result,
                        PLAY_SERVICES_RESOLUTION_REQUEST
                    ) {
                        showToast("cancel", Toast.LENGTH_SHORT)
                    }.show()
                }
            } else {
                Timber.d("%s: This device is not GCM supported.", TAG_MAILBOX_ACTIVITY)
            }
            return false
        }
        return true
    }

    private fun checkForDraftedMessages() {
        val mailboxLocation = mailboxLocationMain.value
        if (mailboxLocation == MessageLocationType.ALL_DRAFT || mailboxLocation == MessageLocationType.DRAFT &&
            mDraftedMessageSnack != null
        ) {
            mDraftedMessageSnack.dismiss()
        }
        registerReceiver(showDraftedSnackBroadcastReceiver, IntentFilter(ACTION_MESSAGE_DRAFTED))
    }

    private fun showDraftedSnack(intent: Intent) {
        var errorText = getString(R.string.message_drafted)
        if (intent.hasExtra(Constants.ERROR)) {
            val extraText = intent.getStringExtra(Constants.ERROR)
            if (!extraText.isNullOrEmpty()) {
                errorText = extraText
            }
        }
        mDraftedMessageSnack = Snackbar.make(mConnectivitySnackLayout!!, errorText, Snackbar.LENGTH_INDEFINITE)
        val tv = mDraftedMessageSnack.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        tv.setTextColor(Color.WHITE)
        mDraftedMessageSnack.setAction(getString(R.string.dismiss)) { mDraftedMessageSnack.dismiss() }
        mDraftedMessageSnack.setActionTextColor(ContextCompat.getColor(this, R.color.icon_purple))
        mDraftedMessageSnack.show()
    }

    private val showDraftedSnackBroadcastReceiver: BroadcastReceiver = ShowDraftedSnackBroadcastReceiver()
    private val fcmBroadcastReceiver: BroadcastReceiver = FcmBroadcastReceiver()

    private class FetchMessagesRetryRunnable internal constructor(activity: MailboxActivity) : Runnable {
        // non leaky runnable
        private val mailboxActivityWeakReference = WeakReference(activity)
        override fun run() {
            val mailboxActivity = mailboxActivityWeakReference.get()
            mailboxActivity?.mJobManager?.addJobInBackground(
                FetchByLocationJob(
                    mailboxActivity.mailboxLocationMain.value ?: MessageLocationType.INVALID,
                    mailboxActivity.mailboxLabelId,
                    true,
                    mailboxActivity.syncUUID,
                    false
                )
            )
        }
    }

    private class OnMessageClickTask internal constructor(
        private val mailboxActivity: WeakReference<MailboxActivity>,
        private val messageDetailsRepository: MessageDetailsRepository,
        private val message: Message
    ) : AsyncTask<Unit, Unit, Message>() {

        override fun doInBackground(vararg params: Unit): Message? =
            messageDetailsRepository.findMessageByIdBlocking(message.messageId!!)

        public override fun onPostExecute(savedMessage: Message?) {
            val mailboxActivity = mailboxActivity.get()
            if (savedMessage != null) {
                message.isInline = savedMessage.isInline
            }
            val messageLocation = message.locationFromLabel()
            if (messageLocation == MessageLocationType.DRAFT || messageLocation == MessageLocationType.ALL_DRAFT) {
                TryToOpenMessageTask(
                    this.mailboxActivity,
                    mailboxActivity?.pendingActionsDatabase,
                    message.messageId,
                    message.isInline,
                    message.addressID
                ).execute()
            } else {
                val intent = AppUtil.decorInAppIntent(
                    Intent(
                        mailboxActivity, MessageDetailsActivity::class.java
                    )
                )
                if (!mailboxActivity!!.mailboxLabelId.isNullOrEmpty()) {
                    intent.putExtra(MessageDetailsActivity.EXTRA_TRANSIENT_MESSAGE, false)
                }
                intent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_ID, message.messageId)
                mailboxActivity.startActivityForResult(intent, REQUEST_CODE_TRASH_MESSAGE_DETAILS)
            }
        }
    }

    private class TryToOpenMessageTask internal constructor(
        private val mailboxActivity: WeakReference<MailboxActivity>,
        private val pendingActionsDatabase: PendingActionsDatabase?,
        private val messageId: String?,
        private val isInline: Boolean,
        private val addressId: String?
    ) : AsyncTask<Unit, Unit, Boolean>() {

        override fun doInBackground(vararg params: Unit): Boolean {
            // return true if message is not in sending process and can be opened
            val pendingForSending = pendingActionsDatabase?.findPendingSendByMessageId(messageId!!)
            return pendingForSending == null ||
                pendingForSending.sent != null &&
                !pendingForSending.sent!!
        }

        override fun onPostExecute(openMessage: Boolean) {
            val mailboxActivity = mailboxActivity.get()
            if (!openMessage) {
                mailboxActivity?.showToast(R.string.cannot_open_message_while_being_sent, Toast.LENGTH_SHORT)
                return
            }
            val intent = AppUtil.decorInAppIntent(Intent(mailboxActivity, ComposeMessageActivity::class.java))
            intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ID, messageId)
            intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_RESPONSE_INLINE, isInline)
            intent.putExtra(ComposeMessageActivity.EXTRA_MESSAGE_ADDRESS_ID, addressId)
            mailboxActivity?.startActivityForResult(intent, REQUEST_CODE_COMPOSE_MESSAGE)
        }
    }

    private class SetUpNewMessageLocationTask internal constructor(
        private val mailboxActivity: WeakReference<MailboxActivity>,
        private val messageDetailsRepository: MessageDetailsRepository,
        private val labelId: String,
        private val isFolder: Boolean,
        private val newLocation: Int,
        private val labelName: String?
    ) : AsyncTask<Unit, Unit, Label?>() {

        override fun doInBackground(vararg params: Unit): Label? {
            val labels = messageDetailsRepository.findAllLabelsWithIds(listOf(labelId))
            return if (labels.isEmpty()) null else labels[0]
        }

        override fun onPostExecute(label: Label?) {
            val mailboxActivity = mailboxActivity.get() ?: return
            mailboxActivity.swipe_refresh_layout.visibility = View.VISIBLE
            mailboxActivity.swipe_refresh_wrapper.visibility = View.VISIBLE
            mailboxActivity.swipe_refresh_layout.isRefreshing = true
            mailboxActivity.no_messages_layout.visibility = View.GONE
            mailboxActivity.spinner_layout.visibility = View.VISIBLE
            if (mailboxActivity.actionMode != null) {
                mailboxActivity.actionMode!!.finish()
            }
            mailboxActivity.invalidateOptionsMenu()
            val locationToSet: MessageLocationType = if (isFolder) {
                MessageLocationType.LABEL_FOLDER
            } else {
                fromInt(newLocation)
            }
            mailboxActivity.mailboxLabelId = labelId
            mailboxActivity.mailboxLabelName = labelName
            mailboxActivity.mailboxLocationMain.value = locationToSet
            if (label != null) {
                val actionBar = mailboxActivity.supportActionBar
                if (actionBar != null) {
                    actionBar.title = label.name
                }
            }
            mailboxActivity.closeDrawer()
            mailboxActivity.messages_list_view.scrollToPosition(0)
            startFetchFirstPageByLabel(fromInt(newLocation), labelId, false)
            RefreshEmptyViewTask(
                this.mailboxActivity,
                mailboxActivity.countersDatabase,
                mailboxActivity.messagesDatabase,
                locationToSet,
                labelId
            ).execute()
        }
    }

    private inner class FcmBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.extras != null
            ) {
                syncUUID = UUID.randomUUID().toString()
                checkUserAndFetchNews()
                if ((messages_list_view.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition() > 1) {
                    handler.postDelayed(
                        {
                            val newMessageSnack =
                                Snackbar.make(
                                    findViewById(R.id.drawer_layout),
                                    getString(R.string.new_message_arrived),
                                    Snackbar.LENGTH_LONG
                                )
                            val view = newMessageSnack.view
                            val tv =
                                view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                            tv.setTextColor(Color.WHITE)
                            newMessageSnack.show()
                        },
                        750
                    )
                }
                messagesAdapter.notifyDataSetChanged()
            }
        }
    }

    private inner class SwipeController : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            if (viewHolder is MessageViewHolder) {
                val mailboxLocation = mailboxLocationMain.value
                return if (mailboxLocationMain.value != null && mailboxLocation == MessageLocationType.DRAFT ||
                    mailboxLocation == MessageLocationType.ALL_DRAFT
                ) {
                    makeMovementFlags(0, 0)
                } else {
                    makeMovementFlags(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)
                }
            }
            return makeMovementFlags(0, 0)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            throw UnsupportedOperationException("Not implemented")
        }

        fun normalise(swipeAction: SwipeAction, mailboxLocation: MessageLocationType?): SwipeAction {
            return if (mailboxLocation == MessageLocationType.DRAFT ||
                mailboxLocation == MessageLocationType.ALL_DRAFT && swipeAction != SwipeAction.STAR
            ) {
                SwipeAction.TRASH
            } else swipeAction
        }

        override fun isItemViewSwipeEnabled(): Boolean {
            return if (actionMode != null) {
                false
            } else {
                super.isItemViewSwipeEnabled()
            }
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val position = viewHolder.adapterPosition
            val swipedItem = messagesAdapter.getItem(position)
            val messageSwiped = SimpleMessage(swipedItem)
            val mailboxLocation = mailboxLocationMain.value
            val swipeActionOrdinal: Int = when (direction) {
                ItemTouchHelper.RIGHT -> mUserManager.mailSettings!!.rightSwipeAction
                ItemTouchHelper.LEFT -> mUserManager.mailSettings!!.leftSwipeAction
                else -> throw IllegalArgumentException("Unrecognised direction: $direction")
            }
            val swipeAction = normalise(SwipeAction.values()[swipeActionOrdinal], mailboxLocationMain.value)
            mSwipeProcessor.handleSwipe(swipeAction, messageSwiped, mJobManager, mailboxLabelId)
            if (undoSnack != null && undoSnack!!.isShownOrQueued) {
                undoSnack!!.dismiss()
            }
            undoSnack = showUndoSnackbar(
                this@MailboxActivity,
                findViewById(R.id.drawer_layout),
                getString(swipeAction.actionDescription),
                {
                    mSwipeProcessor.handleUndo(swipeAction, messageSwiped, mJobManager, mailboxLocation, mailboxLabelId)
                    messagesAdapter.notifyDataSetChanged()
                },
                true
            )
            if (!(swipeAction == SwipeAction.TRASH && mailboxLocationMain.value == MessageLocationType.DRAFT)) {
                undoSnack!!.show()
            }
            if (swipeCustomizeSnack != null && !customizeSwipeSnackShown) {
                handler.postDelayed(
                    {
                        swipeCustomizeSnack!!.show()
                        customizeSwipeSnackShown = true
                    },
                    2750
                )
            }
            messagesAdapter.notifyDataSetChanged()
        }

        override fun onChildDraw(
            canvas: Canvas,
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            deltaX: Float,
            deltaY: Float,
            actionState: Int,
            isCurrentlyActive: Boolean
        ) {
            if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                val itemView = viewHolder.itemView
                val height = itemView.bottom - itemView.top
                val width = itemView.right - itemView.left
                val layoutId: Int = when {
                    mailboxLocationMain.value == MessageLocationType.DRAFT -> {
                        SwipeAction.TRASH.getActionBackgroundResource(deltaX < 0)
                    }
                    deltaX < 0 -> {
                        SwipeAction.values()[mUserManager.mailSettings!!.leftSwipeAction].getActionBackgroundResource(
                            false
                        )
                    }
                    else -> {
                        SwipeAction.values()[mUserManager.mailSettings!!.rightSwipeAction].getActionBackgroundResource(
                            true
                        )
                    }
                }
                val view = layoutInflater.inflate(layoutId, null)
                val widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
                val heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
                view.measure(widthSpec, heightSpec)
                view.layout(0, 0, width, height)
                canvas.save()
                canvas.translate(itemView.left.toFloat(), itemView.top.toFloat())
                view.draw(canvas)
                canvas.restore()
            }
            super.onChildDraw(canvas, recyclerView, viewHolder, deltaX, deltaY, actionState, isCurrentlyActive)
        }
    }

    private inner class ShowDraftedSnackBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            showDraftedSnack(intent)
        }
    }

    private class OnMessageCountsListTask internal constructor(
        private val mailboxActivity: WeakReference<MailboxActivity>,
        private val countersDatabase: CountersDatabase,
        private val messageCountsList: List<MessageCount>
    ) : AsyncTask<Unit, Unit, Int>() {

        override fun doInBackground(vararg params: Unit): Int {
            val totalInbox = countersDatabase.findTotalLocationById(MessageLocationType.INBOX.messageLocationTypeValue)
            return totalInbox?.count ?: -1
        }

        override fun onPostExecute(inboxMessagesCount: Int) {
            val mailboxActivity = mailboxActivity.get() ?: return
            var foundMailbox = false
            val locationCounters: MutableList<TotalLocationCounter> = ArrayList()
            val labelCounters: MutableList<TotalLabelCounter> = ArrayList()
            for (messageCount in messageCountsList) {
                val labelId = messageCount.labelId
                val total = messageCount.total
                if (labelId.length <= 2) {
                    val location = fromInt(Integer.valueOf(labelId))
                    if (location == MessageLocationType.INBOX &&
                        inboxMessagesCount in 0 until total &&
                        !mailboxActivity.refreshMailboxJobRunning
                    ) {
                        mailboxActivity.checkUserAndFetchNews()
                    }
                    if (mailboxActivity.mailboxLocationMain.value == location) {
                        mailboxActivity.refreshEmptyView(total)
                        foundMailbox = true
                    }
                    locationCounters.add(TotalLocationCounter(location.messageLocationTypeValue, total))
                } else {
                    // label
                    if (labelId == mailboxActivity.mailboxLabelId) {
                        mailboxActivity.refreshEmptyView(total)
                        foundMailbox = true
                    }
                    if (!foundMailbox) {
                        mailboxActivity.refreshEmptyView(0)
                    }
                    labelCounters.add(TotalLabelCounter(labelId, total))
                }
            }
            mailboxActivity.setRefreshing(false)
            RefreshTotalCountersTask(countersDatabase, locationCounters, labelCounters).execute()
        }
    }
}
