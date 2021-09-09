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
package ch.protonmail.android.mailbox.presentation

import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Resources
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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.postDelayed
import androidx.core.view.isVisible
import androidx.core.view.postDelayed
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.loader.app.LoaderManager
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import ch.protonmail.android.R
import ch.protonmail.android.activities.EXTRA_FIRST_LOGIN
import ch.protonmail.android.activities.EXTRA_SETTINGS_ITEM_TYPE
import ch.protonmail.android.activities.EditSettingsItemActivity
import ch.protonmail.android.activities.EngagementActivity
import ch.protonmail.android.activities.NavigationActivity
import ch.protonmail.android.activities.SearchActivity
import ch.protonmail.android.activities.SettingsItem
import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.mailbox.RefreshEmptyViewTask
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.activities.settings.SettingsEnum
import ch.protonmail.android.adapters.messages.MailboxItemViewHolder.MessageViewHolder
import ch.protonmail.android.adapters.messages.MailboxRecyclerViewAdapter
import ch.protonmail.android.adapters.swipe.ArchiveSwipeHandler
import ch.protonmail.android.adapters.swipe.MarkReadSwipeHandler
import ch.protonmail.android.adapters.swipe.SpamSwipeHandler
import ch.protonmail.android.adapters.swipe.StarSwipeHandler
import ch.protonmail.android.adapters.swipe.SwipeAction
import ch.protonmail.android.adapters.swipe.TrashSwipeHandler
import ch.protonmail.android.api.models.SimpleMessage
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.DrawerOptionType
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.core.Constants.MessageLocationType.Companion.fromInt
import ch.protonmail.android.core.Constants.Prefs.PREF_DONT_SHOW_PLAY_SERVICES
import ch.protonmail.android.core.Constants.Prefs.PREF_SWIPE_GESTURES_DIALOG_SHOWN
import ch.protonmail.android.core.Constants.Prefs.PREF_USED_SPACE
import ch.protonmail.android.core.Constants.SWIPE_GESTURES_CHANGED_VERSION
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.CounterDatabase
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.PendingActionDatabase
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.events.FetchLabelsEvent
import ch.protonmail.android.events.MailboxLoadedEvent
import ch.protonmail.android.events.MailboxNoMessagesEvent
import ch.protonmail.android.events.SettingsChangedEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.fcm.MultiUserFcmTokenManager
import ch.protonmail.android.fcm.RegisterDeviceWorker
import ch.protonmail.android.fcm.model.FirebaseToken
import ch.protonmail.android.feature.account.AccountStateManager
import ch.protonmail.android.jobs.EmptyFolderJob
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import ch.protonmail.android.mailbox.presentation.MailboxViewModel.MaxLabelsReached
import ch.protonmail.android.mailbox.presentation.model.MailboxUiItem
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.servers.notification.EXTRA_MAILBOX_LOCATION
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.ui.actionsheet.MessageActionSheet
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.Event
import ch.protonmail.android.utils.MessageUtils
import ch.protonmail.android.utils.NetworkSnackBarUtil
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showDeleteConfirmationDialog
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showUndoSnackbar
import ch.protonmail.android.utils.ui.selection.SelectionModeEnum
import ch.protonmail.android.views.messageDetails.BottomActionsView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.iid.FirebaseInstanceId
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_mailbox.*
import kotlinx.android.synthetic.main.activity_mailbox.screenShotPreventerView
import kotlinx.android.synthetic.main.activity_message_details.*
import kotlinx.android.synthetic.main.navigation_drawer.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.observe
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.exhaustive
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.time.toDuration

private const val TAG_MAILBOX_ACTIVITY = "MailboxActivity"
private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9000
private const val STATE_MAILBOX_LOCATION = "mailbox_location"
private const val STATE_MAILBOX_LABEL_LOCATION = "mailbox_label_location"
private const val STATE_MAILBOX_LABEL_LOCATION_NAME = "mailbox_label_location_name"
const val LOADER_ID_LABELS_OFFLINE = 32
private const val REQUEST_CODE_COMPOSE_MESSAGE = 19
private const val ACTION_MODE_STATUS_BAR_COLOR_DELAY = 500L

@AndroidEntryPoint
internal class MailboxActivity :
    NavigationActivity(),
    ActionMode.Callback,
    OnRefreshListener {

    private lateinit var counterDao: CounterDao
    private lateinit var pendingActionDao: PendingActionDao

    @Inject
    lateinit var messageDetailsRepositoryFactory: MessageDetailsRepository.AssistedFactory
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @Inject
    lateinit var networkSnackBarUtil: NetworkSnackBarUtil

    @Inject
    lateinit var registerDeviceWorkerEnqueuer: RegisterDeviceWorker.Enqueuer

    @Inject
    lateinit var multiUserFcmTokenManager: MultiUserFcmTokenManager

    @Inject
    lateinit var isConversationModeEnabled: ConversationModeEnabled

    @Inject
    @DefaultSharedPreferences
    lateinit var defaultSharedPreferences: SharedPreferences

    private lateinit var mailboxAdapter: MailboxRecyclerViewAdapter
    private var swipeController: SwipeController = SwipeController()

    private val isLoadingMore = AtomicBoolean(false)
    private var scrollStateChanged = false
    private var actionMode: ActionMode? = null
    // For the time being we don't show it at all; will be re-added in MAILAND-2320
    private var swipeCustomizeSnack: Snackbar? = null
    private var mailboxLabelId: String? = null
        set(value) {
            field = value
            // we need to set it on view model for filtering by label
            setNewLabel(value ?: EMPTY_STRING)
        }
    private var mailboxLabelName: String? = null
    private var lastFetchedMailboxItemsIds = emptyList<String>()
    private var refreshMailboxJobRunning = false
    private lateinit var syncUUID: String
    private var customizeSwipeSnackShown = false
    private val mailboxViewModel: MailboxViewModel by viewModels()
    private var storageLimitApproachingAlertDialog: AlertDialog? = null
    private val handler = Handler(Looper.getMainLooper())

    override val currentLabelId get() = mailboxLabelId

    override fun getLayoutId(): Int = R.layout.activity_mailbox

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.ProtonTheme_Mail)
        super.onCreate(savedInstanceState)

        val userId = userManager.currentUserId ?: return

        messageDetailsRepository = messageDetailsRepositoryFactory.create(userId)
        counterDao = CounterDatabase.getInstance(this, userId).getDao()
        pendingActionDao = PendingActionDatabase.getInstance(this, userId).getDao()

        // force reload of MessageDetailsRepository's internal dependencies in case we just switched user

        // TODO if we decide to use special flag for switching (and not login), change this
        if (intent.getBooleanExtra(EXTRA_FIRST_LOGIN, false)) {
            messageDetailsRepository.reloadDependenciesForUser(userId)
            multiUserFcmTokenManager.setTokenUnsentForAllSavedUsersBlocking() // force FCM to re-register
        }
        val extras = intent.extras
        if (!userManager.isEngagementShown) {
            startActivity(AppUtil.decorInAppIntent(Intent(this, EngagementActivity::class.java)))
        }
        // Set the padding to match the Status Bar height
        if (savedInstanceState != null) {
            val locationInt = savedInstanceState.getInt(STATE_MAILBOX_LOCATION)
            mailboxLabelId = savedInstanceState.getString(STATE_MAILBOX_LABEL_LOCATION)
            mailboxLabelName = savedInstanceState.getString(STATE_MAILBOX_LABEL_LOCATION_NAME)
            setMailboxLocation(fromInt(locationInt))
        }
        if (extras != null && extras.containsKey(EXTRA_MAILBOX_LOCATION)) {
            switchToMailboxLocation(extras.getInt(EXTRA_MAILBOX_LOCATION))
        }
        startObserving()
        mailboxViewModel.toastMessageMaxLabelsReached.observe(this) { event: Event<MaxLabelsReached?> ->
            val maxLabelsReached = event.getContentIfNotHandled()
            if (maxLabelsReached != null) {
                val message = getString(
                    R.string.max_labels_exceeded,
                    maxLabelsReached.subject,
                    maxLabelsReached.maxAllowedLabels
                )
                showToast(message, Toast.LENGTH_SHORT)
            }
        }

        mailboxViewModel.hasConnectivity.observe(this, ::onConnectivityEvent)

        startObservingUsedSpace()

        var actionModeAux: ActionMode? = null
        mailboxAdapter = MailboxRecyclerViewAdapter(this) { selectionModeEvent ->
            when (selectionModeEvent) {
                SelectionModeEnum.STARTED -> {
                    actionModeAux = startActionMode(this@MailboxActivity)
                    mailboxActionsView.layoutParams.height = ConstraintLayout.LayoutParams.WRAP_CONTENT
                    mailboxActionsView.visibility = View.VISIBLE
                }
                SelectionModeEnum.ENDED -> {
                    val actionModeEnd = actionModeAux
                    if (actionModeEnd != null) {
                        actionModeEnd.finish()
                        actionModeAux = null
                    }
                    mailboxActionsView.visibility = View.GONE
                }
            }
        }

        mailboxViewModel.pendingSendsLiveData.observe(this, mailboxAdapter::setPendingForSendingList)
        mailboxViewModel.pendingUploadsLiveData.observe(this, mailboxAdapter::setPendingUploadsList)
        messageDetailsRepository.getAllLabelsLiveData().observe(this, mailboxAdapter::setLabels)

        mailboxViewModel.hasSuccessfullyDeletedMessages.observe(this) { isSuccess ->
            Timber.v("Delete message status is success $isSuccess")
            if (!isSuccess) {
                showToast(R.string.message_deleted_error)
            }
        }

        checkUserAndFetchNews()

        setTitle()

        mailboxRecyclerView.adapter = mailboxAdapter
        mailboxRecyclerView.layoutManager = LinearLayoutManager(this)
        // Set the list divider
        val itemDecoration = DividerItemDecoration(mailboxRecyclerView.context, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(getDrawable(R.drawable.list_divider)!!)
        mailboxRecyclerView.addItemDecoration(itemDecoration)

        buildSwipeProcessor()
        initializeSwipeRefreshLayout(mailboxSwipeRefreshLayout)

        if (userManager.isFirstMailboxLoad) {
            userManager.firstMailboxLoadDone()
        }

        mailboxAdapter.setItemClick { mailboxUiItem: MailboxUiItem ->
            OnMessageClickTask(
                WeakReference(this@MailboxActivity),
                messageDetailsRepository,
                mailboxUiItem.itemId,
                mailboxUiItem.subject,
                currentMailboxLocation.messageLocationTypeValue
            ).execute()
        }

        mailboxAdapter.setOnItemSelectionChangedListener {
            val checkedItems = mailboxAdapter.checkedMailboxItems.size
            actionMode?.title = "$checkedItems ${getString(R.string.selected)}"

            mailboxActionsView.setAction(
                BottomActionsView.ActionPosition.ACTION_SECOND,
                true,
                if (MessageUtils.areAllUnRead(
                        selectedMessages
                    )
                ) R.drawable.ic_envelope_open_text else R.drawable.ic_envelope_dot
            )
        }

        checkRegistration()

        mailboxRecyclerView.addOnScrollListener(listScrollListener)

        fetchOrganizationData()

        with(mailboxViewModel) {

            mailboxState
                .onEach { renderState(it) }
                .launchIn(lifecycleScope)

            mailboxLocation
                .onEach { mailboxAdapter.setNewLocation(it) }
                .launchIn(lifecycleScope)

            drawerLabels
                .onEach { sideDrawer.setFoldersAndLabelsSection(it) }
                .launchIn(lifecycleScope)

            unreadCounters
                .onEach { sideDrawer.setUnreadCounters(it) }
                .launchIn(lifecycleScope)
        }

        setUpMailboxActionsView()

        lifecycleScope.launch {
            mailboxViewModel.getMailSettingsState().onEach {
                when (it) {
                    is GetMailSettings.MailSettingsState.Error.Message -> {
                        showToast(it.message.toString(), Toast.LENGTH_LONG)
                    }
                    is GetMailSettings.MailSettingsState.Error.NoPrimaryAccount -> {
                        Timber.w("No Primary Account")
                    }
                    is GetMailSettings.MailSettingsState.Success -> {
                        it.mailSettings?.let { mailSettings ->
                            swipeController.setCurrentMailSetting(mailSettings)
                            ItemTouchHelper(swipeController).attachToRecyclerView(mailboxRecyclerView)
                        }
                        mailboxViewModel.refreshMessages()
                    }
                    else -> Unit
                }.exhaustive
            }.launchIn(lifecycleScope)
        }
    }

    override fun secureContent(): Boolean = true

    override fun enableScreenshotProtector() {
        screenShotPreventerView.visibility = View.VISIBLE
    }

    override fun disableScreenshotProtector() {
        screenShotPreventerView.visibility = View.GONE
    }

    private fun startObserving() {
        val owner = this
        mailboxViewModel.run {
            usedSpaceActionEvent(FLOW_START_ACTIVITY)
            manageLimitReachedWarning.observe(owner, setupUpLimitReachedObserver)
            manageLimitApproachingWarning.observe(owner, setupUpLimitApproachingObserver)
            manageLimitBelowCritical.observe(owner, setupUpLimitBelowCriticalObserver)
            manageLimitReachedWarningOnTryCompose.observe(owner, setupUpLimitReachedTryComposeObserver)
        }
    }

    private fun startObservingUsedSpace() {
        val preferences = SecureSharedPreferences.getPrefsForUser(this, userManager.requireCurrentUserId())
        preferences.observe<Long>(PREF_USED_SPACE)
            .onEach { mailboxViewModel.usedSpaceActionEvent(FLOW_USED_SPACE_CHANGED) }
            .launchIn(lifecycleScope)
    }

    private val setupUpLimitReachedObserver = Observer { limitReached: Event<Boolean> ->
        if (limitReached.getContentIfNotHandled() == true) {
            if (storageLimitApproachingAlertDialog != null) {
                storageLimitApproachingAlertDialog!!.dismiss()
                storageLimitApproachingAlertDialog = null
            }
            if (userManager.canShowStorageLimitReached()) {

                showTwoButtonInfoDialog(
                    titleStringId = R.string.storage_limit_warning_title,
                    messageStringId = R.string.storage_limit_reached_text,
                    rightStringId = R.string.okay,
                    leftStringId = R.string.learn_more,
                    onPositiveButtonClicked = {
                        userManager.setShowStorageLimitReached(false)
                    },
                    onNegativeButtonClicked = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(getString(R.string.limit_reached_learn_more))
                        )
                        startActivity(browserIntent)
                        userManager.setShowStorageLimitReached(false)
                    }
                )
            }
            userManager.setShowStorageLimitWarning(true)
            storageLimitAlert.apply {
                visibility = View.VISIBLE
                setIcon(getDrawable(R.drawable.inbox)!!)
                setText(getString(R.string.storage_limit_alert))
            }
        }
    }

    private fun showStorageLimitApproachingAlertDialog() {
        storageLimitApproachingAlertDialog = showTwoButtonInfoDialog(
            titleStringId = R.string.storage_limit_warning_title,
            messageStringId = R.string.storage_limit_approaching_text,
            leftStringId = R.string.dont_remind_again,
            onPositiveButtonClicked = { storageLimitApproachingAlertDialog = null },
            onNegativeButtonClicked = {
                userManager.setShowStorageLimitWarning(false)
                storageLimitApproachingAlertDialog = null
            }
        )
    }

    private val setupUpLimitApproachingObserver = { limitApproaching: Event<Boolean> ->

        if (limitApproaching.getContentIfNotHandled() == true) {
            if (userManager.canShowStorageLimitWarning()) {
                if (storageLimitApproachingAlertDialog == null || !storageLimitApproachingAlertDialog!!.isShowing) {
                    // This is the first time the dialog is going to be showed or
                    // the dialog is not showing and had previously been dismissed by clicking the positive
                    // or negative button or the dialog is not showing and had previously been dismissed on touch
                    // outside or by clicking the back button
                    showStorageLimitApproachingAlertDialog()
                }
            }
            userManager.setShowStorageLimitReached(true)
            storageLimitAlert.visibility = View.GONE
        }
    }

    private val setupUpLimitBelowCriticalObserver = { limitReached: Event<Boolean> ->
        if (limitReached.getContentIfNotHandled() == true) {
            userManager.setShowStorageLimitWarning(true)
            userManager.setShowStorageLimitReached(true)
            storageLimitAlert.visibility = View.GONE
        }
    }

    private val setupUpLimitReachedTryComposeObserver = Observer { limitReached: Event<Boolean> ->
        if (limitReached.getContentIfNotHandled() == true) {
            showTwoButtonInfoDialog(
                titleStringId = R.string.storage_limit_warning_title,
                messageStringId = R.string.storage_limit_reached_text,
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
        get() = mailboxAdapter.checkedMailboxItems.map { SimpleMessage(it) }

    private var firstLogin: Boolean? = null

    private fun startObservingPendingActions() {
        val owner = this
        mailboxViewModel.run {
            pendingSendsLiveData.removeObservers(owner)
            pendingUploadsLiveData.removeObservers(owner)
            reloadDependenciesForUser()
            pendingSendsLiveData.observe(owner) { mailboxAdapter.setPendingForSendingList(it) }
            pendingUploadsLiveData.observe(owner) { mailboxAdapter.setPendingUploadsList(it) }
        }
    }

    override fun onAccountSwitched(switch: AccountStateManager.AccountSwitch) {
        super.onAccountSwitched(switch)

        val currentUserId = userManager.currentUserId ?: return
        Timber.v("onAccountSwitched, new userId: $currentUserId")

        mJobManager.start()
        counterDao = CounterDatabase.getInstance(this, currentUserId).getDao()
        pendingActionDao = PendingActionDatabase.getInstance(this, currentUserId).getDao()
        messageDetailsRepository.reloadDependenciesForUser(currentUserId)

        startObservingPendingActions()
        AppUtil.clearNotifications(this, currentUserId)
        lazyManager.reset()
        setUpDrawer()
        checkRegistration()
        // This should trigger loading mailbox items for the newly switched account.
        // This method also "reloads dependencies" for the instance of `messageDetailsRepo` held by
        // MailboxVM. This should be done before triggering an "update" of the Mailbox for the new user
        mailboxViewModel.setNewUserId(currentUserId)
        switchToMailboxLocation(DrawerOptionType.INBOX.drawerOptionTypeValue)

        messageDetailsRepository.getAllLabelsLiveData().observe(this, mailboxAdapter::setLabels)
        // Account has been switched, so used space changed as well
        mailboxViewModel.usedSpaceActionEvent(FLOW_USED_SPACE_CHANGED)
        // Observe used space for current account
        startObservingUsedSpace()

        // manually update the flags for preventing screenshots
        if (isPreventingScreenshots || userManager.currentLegacyUser?.isPreventTakingScreenshots == true) {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }

        // Set the elevation to 0 since after account switch the list is scrolled to the top
        setElevationOnToolbarAndStatusView(false)
    }

    private fun renderState(state: MailboxState) {
        Timber.v("New mailbox state: ${state.javaClass.canonicalName}")
        setLoadingMore(false)

        when (state) {
            is MailboxState.Loading -> setRefreshing(true)
            is MailboxState.DataRefresh -> {
                lastFetchedMailboxItemsIds = state.lastFetchedItemsIds
                setRefreshing(false)
                include_mailbox_no_messages.isVisible =
                    state.lastFetchedItemsIds.isEmpty() && mailboxAdapter.itemCount == 0
            }
            is MailboxState.Data -> {
                Timber.v("Data state items count: ${state.items.size}")
                include_mailbox_error.isVisible = false
                include_mailbox_no_messages.isVisible = state.isFreshData && state.items.isEmpty()
                mailboxRecyclerView.isVisible != state.items.isEmpty()

                mailboxAdapter.submitList(state.items) {
                    if (state.shouldResetPosition) mailboxRecyclerView.scrollToPosition(0)
                }
            }
            is MailboxState.Error -> {
                setRefreshing(false)
                Timber.e(state.throwable, "Mailbox error ${state.error}")
                include_mailbox_no_messages.isVisible = false

                if (mailboxAdapter.itemCount > 0) {
                    include_mailbox_error.isVisible = false
                    if (state.isOffline.not()) showToast(R.string.inbox_could_not_retrieve_messages)
                } else {
                    include_mailbox_error.isVisible = true
                }
            }
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
            val layoutManager = recyclerView.layoutManager as LinearLayoutManager

            val firstCompletelyVisibleItemPosition = layoutManager.findFirstCompletelyVisibleItemPosition()

            // Load more when showing last fetched messages or at the end of the list
            if (dy > 0 && isLoadingMore.get().not()) {
                val lastCompletelyVisibleItemPosition = layoutManager.findLastCompletelyVisibleItemPosition()
                val isAtBottomOfTheList = lastCompletelyVisibleItemPosition == mailboxAdapter.itemCount - 1
                fun inAnyLastFetchedMailboxItemVisible() =
                    mailboxAdapter.isAnyMailboxItemWithinPositions(
                        mailboxItemsIds = lastFetchedMailboxItemsIds,
                        startPosition = firstCompletelyVisibleItemPosition,
                        endPosition = lastCompletelyVisibleItemPosition
                    )
                if (isAtBottomOfTheList || inAnyLastFetchedMailboxItemVisible()) {
                    setLoadingMore(true)
                    mailboxViewModel.loadMore()
                    lastFetchedMailboxItemsIds = emptyList()
                }
            }

            // Increase the elevation if the list is scrolled down and decrease if it is scrolled to the top
            if (firstCompletelyVisibleItemPosition == 0) {
                setElevationOnToolbarAndStatusView(false)
            } else {
                setElevationOnToolbarAndStatusView(true)
            }
        }
    }

    private fun loadMailboxItems() {
        mailboxViewModel.loadMore()
    }

    private fun setElevationOnToolbarAndStatusView(shouldIncreaseElevation: Boolean) {
        val elevation = if (shouldIncreaseElevation) {
            resources.getDimensionPixelSize(R.dimen.action_bar_elevation)
        } else {
            resources.getDimensionPixelSize(R.dimen.action_bar_no_elevation)
        }.toFloat()
        supportActionBar?.elevation = elevation
        mailboxStatusLayout.elevation = elevation
    }

    private fun registerFcmReceiver() {
        val filter = IntentFilter(getString(R.string.action_notification))
        filter.priority = 2
        LocalBroadcastManager.getInstance(this).registerReceiver(fcmBroadcastReceiver, filter)
    }

    private fun onConnectivityCheckRetry() {
        mConnectivitySnackLayout?.let {
            networkSnackBarUtil.getCheckingConnectionSnackBar(it, mailboxActionsView.id).show()
        }
        syncUUID = UUID.randomUUID().toString()
        lifecycleScope.launch {
            delay(3.toDuration(TimeUnit.SECONDS))
            mailboxViewModel.loadMore()
        }
        mailboxViewModel.checkConnectivityDelayed()
    }

    override fun onDohFailed() {
        super.onDohFailed()
        showNoConnSnackAndScheduleRetry(Constants.ConnectionState.CANT_REACH_SERVER)
    }

    private fun checkRegistration() {
        // Check device for Play Services APK.
        lifecycleScope.launchWhenCreated {
            if (checkPlayServices()) {
                val tokenSent = multiUserFcmTokenManager.isTokenSentForAllLoggedUsers()
                if (!tokenSent) {
                    runCatching {
                        FirebaseInstanceId.getInstance().instanceId.addOnCompleteListener { task ->
                            if (task.isSuccessful && task.result != null) {
                                multiUserFcmTokenManager.saveTokenBlocking(FirebaseToken(task.result!!.token))
                                registerDeviceWorkerEnqueuer()
                            } else {
                                Timber.e(task.exception, "Could not retrieve FirebaseInstanceId")
                            }
                        }
                    }.onFailure {
                        showToast(R.string.invalid_firebase_api_key_message)
                        Timber.e(it, getString(R.string.invalid_firebase_api_key_message))
                    }
                }
            }
        }
    }

    private fun checkUserAndFetchNews(): Boolean {
        syncUUID = UUID.randomUUID().toString()

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
            // TODO: remove?
            loadMailboxItems()
            true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        checkRegistration()
        checkUserAndFetchNews()
        switchToMailboxLocation(DrawerOptionType.INBOX.drawerOptionTypeValue)
    }

    private fun shouldShowSwipeGesturesChangedDialog(): Boolean {
        val prefs: SharedPreferences = defaultSharedPreferences
        val previousVersion: Int = prefs.getInt(Constants.Prefs.PREF_PREVIOUS_APP_VERSION, Int.MIN_VALUE)
        // The dialog should be shown once on the update when swiping gestures are switched
        return previousVersion in 1 until SWIPE_GESTURES_CHANGED_VERSION &&
            !prefs.getBoolean(PREF_SWIPE_GESTURES_DIALOG_SHOWN, false)
    }

    private fun showSwipeGesturesChangedDialog() {
        showTwoButtonInfoDialog(
            titleStringId = R.string.swipe_gestures_changed,
            messageStringId = R.string.swipe_gestures_changed_message,
            leftStringId = R.string.go_to_settings,
            onNegativeButtonClicked = {
                val swipeGestureIntent = Intent(
                    this,
                    EditSettingsItemActivity::class.java
                )
                swipeGestureIntent.putExtra(EXTRA_SETTINGS_ITEM_TYPE, SettingsItem.SWIPE)
                startActivityForResult(
                    AppUtil.decorInAppIntent(swipeGestureIntent),
                    SettingsEnum.SWIPING_GESTURE.ordinal
                )
            }
        )
        defaultSharedPreferences.edit().putBoolean(PREF_SWIPE_GESTURES_DIALOG_SHOWN, true).apply()
    }

    override fun onResume() {
        super.onResume()

        registerFcmReceiver()
        checkDelinquency()
        mailboxViewModel.checkConnectivity()
        val mailboxLocation = mailboxViewModel.mailboxLocation.value
        if (mailboxLocation == MessageLocationType.INBOX) {
            AppUtil.clearNotifications(this, userManager.requireCurrentUserId())
        }

        if (shouldShowSwipeGesturesChangedDialog()) {
            showSwipeGesturesChangedDialog()
        }

    }

    override fun onPause() {
        runCatching {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(fcmBroadcastReceiver)
        }
        networkSnackBarUtil.hideAllSnackBars()
        super.onPause()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(
            STATE_MAILBOX_LOCATION,
            mailboxViewModel.mailboxLocation.value.messageLocationTypeValue
        )
        outState.putString(STATE_MAILBOX_LABEL_LOCATION, mailboxLabelId)
        outState.putString(STATE_MAILBOX_LABEL_LOCATION_NAME, mailboxLabelName)
        super.onSaveInstanceState(outState)
    }

    private fun setUpMenuItems(composeMenuItem: MenuItem, searchMenuItem: MenuItem) {
        composeMenuItem.actionView.findViewById<ImageView>(R.id.composeImageButton)
            .setOnClickListener {
                mailboxViewModel.usedSpaceActionEvent(FLOW_TRY_COMPOSE)
            }
        searchMenuItem.actionView.findViewById<ImageView>(R.id.searchImageButton)
            .setOnClickListener {
                val intent = AppUtil.decorInAppIntent(
                    Intent(
                        this@MailboxActivity,
                        SearchActivity::class.java
                    )
                )
                startActivity(intent)
            }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_mailbox_options, menu)
        setUpMenuItems(menu.findItem(R.id.compose), menu.findItem(R.id.search))
        val mailboxLocation = mailboxViewModel.mailboxLocation.value
        menu.findItem(R.id.empty).isVisible =
            mailboxLocation in listOf(MessageLocationType.DRAFT, MessageLocationType.SPAM, MessageLocationType.TRASH)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.clear()
        menuInflater.inflate(R.menu.menu_mailbox_options, menu)
        setUpMenuItems(menu.findItem(R.id.compose), menu.findItem(R.id.search))
        val mailboxLocation = mailboxViewModel.mailboxLocation.value
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
            R.id.empty -> {
                if (!isFinishing) {
                    showTwoButtonInfoDialog(
                        titleStringId = R.string.empty_folder,
                        messageStringId = R.string.are_you_sure_empty,
                        leftStringId = R.string.no
                    ) {
                        mJobManager.addJobInBackground(
                            EmptyFolderJob(mailboxViewModel.mailboxLocation.value, this.mailboxLabelId)
                        )
                        setLoadingMore(false)
                    }
                }
                true
            }
            else -> super.onOptionsItemSelected(menuItem)
        }
    }

    override fun onBackPressed() {
        saveLastInteraction()
        val drawerClosed = closeDrawer()
        if (!drawerClosed && mailboxViewModel.mailboxLocation.value != MessageLocationType.INBOX) {
            switchToMailboxLocation(DrawerOptionType.INBOX.drawerOptionTypeValue)
        } else if (!drawerClosed) {
            moveTaskToBack(true)
        }
    }

    private fun initializeSwipeRefreshLayout(swipeRefreshLayoutAux: SwipeRefreshLayout) {
        swipeRefreshLayoutAux.setColorSchemeResources(R.color.cornflower_blue)
        swipeRefreshLayoutAux.setOnRefreshListener(this)
    }

    private fun setRefreshing(shouldRefresh: Boolean) {
        Timber.v("setRefreshing shouldRefresh: $shouldRefresh")
        mailboxSwipeRefreshLayout.isRefreshing = shouldRefresh
    }

    private fun setLoadingMore(loadingMore: Boolean): Boolean =
        isLoadingMore.getAndSet(loadingMore)

    override fun onInbox(type: DrawerOptionType) {
        AppUtil.clearNotifications(applicationContext, userManager.requireCurrentUserId())
        switchToMailboxLocation(type.drawerOptionTypeValue)
    }

    override fun onOtherMailBox(type: DrawerOptionType) {
        switchToMailboxLocation(type.drawerOptionTypeValue)
    }

    public override fun onLabelMailBox(
        type: DrawerOptionType,
        labelId: String,
        labelName: String,
        isFolder: Boolean
    ) {
        switchToMailboxCustomLocation(type.drawerOptionTypeValue, labelId, labelName, isFolder)
    }

    override val currentMailboxLocation: MessageLocationType
        get() = mailboxViewModel.mailboxLocation.value

    private fun setTitle() {
        val titleRes: Int = when (mailboxViewModel.mailboxLocation.value) {
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
        supportActionBar?.setTitle(titleRes)
    }

    private fun showNoConnSnackAndScheduleRetry(connectivity: Constants.ConnectionState) {
        Timber.v("show NoConnection Snackbar ${mConnectivitySnackLayout != null}")

        mConnectivitySnackLayout?.let { snackBarLayout ->
            lifecycleScope.launchWhenCreated {

                networkSnackBarUtil.getNoConnectionSnackBar(
                    parentView = snackBarLayout,
                    user = userManager.requireCurrentLegacyUser(),
                    netConfiguratorCallback = this@MailboxActivity,
                    onRetryClick = ::onConnectivityCheckRetry,
                    isOffline = connectivity == Constants.ConnectionState.NO_INTERNET,
                    anchorViewId = mailboxActionsView.id
                ).show()
            }
        }
    }

    private fun hideNoConnSnack() {
        Timber.v("hideNoConnSnack")
        networkSnackBarUtil.hideCheckingConnectionSnackBar()
        networkSnackBarUtil.hideNoConnectionSnackBar()
    }

    @Subscribe
    fun onSettingsChangedEvent(event: SettingsChangedEvent) {
        if (!event.success) {
            showToast(R.string.saving_failed_no_conn, Toast.LENGTH_LONG, Gravity.CENTER)
        }
    }

    @Subscribe
    fun onMailboxLoaded(event: MailboxLoadedEvent?) {
        Timber.v("Mailbox loaded status ${event?.status}")
        if (event == null || event.uuid != null && event.uuid != syncUUID) {
            return
        }
        refreshMailboxJobRunning = false
        setLoadingMore(false)
        setRefreshing(false)
        if (!isDohOngoing) {
            showToast(event.status)
        }
        mNetworkResults.setMailboxLoaded(MailboxLoadedEvent(Status.SUCCESS, null))
    }

    private fun onConnectivityEvent(connectivity: Constants.ConnectionState) {
        Timber.v("onConnectivityEvent hasConnection: ${connectivity.name}")
        if (!isDohOngoing) {
            Timber.d("DoH NOT ongoing showing UI")
            if (connectivity != Constants.ConnectionState.CONNECTED) {
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
        if (isLoadingMore.get()) {
            showToast(R.string.no_more_messages, Toast.LENGTH_SHORT)
            mailboxAdapter.notifyDataSetChanged()
        }
        setLoadingMore(false)
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
    fun onLabelsLoadedEvent(event: FetchLabelsEvent) {
        if (event.status == Status.SUCCESS) {
            mailboxAdapter.notifyDataSetChanged()
        }
    }

    // region Action mode
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        actionMode = mode
        mailboxSwipeRefreshLayout.isEnabled = false
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = true

    override fun onActionModeStarted(mode: ActionMode?) {
        super.onActionModeStarted(mode)
        // We need to set a solid color for status bar during Action mode, or it will be black
        window.statusBarColor = getColor(R.color.status_bar)
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem) = true

    override fun onActionModeFinished(mode: ActionMode?) {
        super.onActionModeFinished(mode)
        // The ActionMode will be visually closed with a small delay after this callback ( or any other, like
        //  onDestroyActionMode ), for this reason we apply a small delay before making the status bar transparent
        //  again, in order to avoid to flash a black status bar
        window.decorView.postDelayed(ACTION_MODE_STATUS_BAR_COLOR_DELAY) {
            window.statusBarColor = getColor(R.color.transparent)
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        actionMode = null
        mailboxActionsView.visibility = View.GONE
        mailboxSwipeRefreshLayout.isEnabled = true
        mailboxAdapter.endSelectionMode()
    }
    // endregion

    private fun setUpMailboxActionsView() {
        val actionsUiModel = BottomActionsView.UiModel(
            if (currentMailboxLocation in arrayOf(
                    MessageLocationType.TRASH,
                    MessageLocationType.DRAFT,
                    MessageLocationType.SPAM,
                    MessageLocationType.SENT
                )
            ) R.drawable.ic_trash_empty else R.drawable.ic_trash,
            R.drawable.ic_envelope_dot,
            R.drawable.ic_folder_move,
            R.drawable.ic_label
        )
        mailboxActionsView.bind(actionsUiModel)
        mailboxActionsView.setOnFirstActionClickListener {
            val messageIds = getSelectedMessageIds()
            if (currentMailboxLocation in arrayOf(
                    MessageLocationType.TRASH,
                    MessageLocationType.DRAFT,
                    MessageLocationType.SPAM,
                    MessageLocationType.SENT
                )
            ) {
                showDeleteConfirmationDialog(
                    this,
                    getString(R.string.delete_messages),
                    getString(R.string.confirm_destructive_action)
                ) {
                    mailboxViewModel.deleteAction(
                        messageIds,
                        UserId(userManager.requireCurrentUserId().id),
                        currentMailboxLocation
                    )
                }
            } else {
                undoSnack = showUndoSnackbar(
                    this@MailboxActivity,
                    findViewById(R.id.drawer_layout),
                    resources.getQuantityString(R.plurals.action_move_to_trash, messageIds.size),
                    { },
                    false
                ).apply { anchorView = mailboxActionsView }
                undoSnack!!.show()
                mailboxViewModel.moveToFolder(
                    messageIds,
                    UserId(userManager.requireCurrentUserId().id),
                    currentMailboxLocation,
                    MessageLocationType.TRASH.messageLocationTypeValue.toString()
                )
            }
            actionMode?.finish()
        }
        mailboxActionsView.setOnSecondActionClickListener {
            val messageIds = getSelectedMessageIds()
            if (MessageUtils.areAllUnRead(selectedMessages)) {
                mailboxViewModel.markRead(
                    messageIds,
                    UserId(userManager.requireCurrentUserId().id),
                    currentMailboxLocation,
                    mailboxLabelId ?: currentMailboxLocation.messageLocationTypeValue.toString()
                )
            } else {
                mailboxViewModel.markUnRead(
                    messageIds,
                    UserId(userManager.requireCurrentUserId().id),
                    currentMailboxLocation,
                    mailboxLabelId ?: currentMailboxLocation.messageLocationTypeValue.toString()
                )
            }
            actionMode?.finish()
        }
        mailboxActionsView.setOnThirdActionClickListener {
            showFoldersManager(getSelectedMessageIds())
            actionMode?.finish()
        }
        mailboxActionsView.setOnFourthActionClickListener {
            showLabelsManager(getSelectedMessageIds())
            actionMode?.finish()
        }
        mailboxActionsView.setOnMoreActionClickListener {
            showActionSheet(getSelectedMessageIds(), isConversationModeEnabled(currentMailboxLocation))
            actionMode?.finish()
        }
    }

    private fun getSelectedMessageIds(): List<String> = selectedMessages.map { it.messageId }

    private fun showActionSheet(
        messagesIds: List<String>,
        isConversationsModeOn: Boolean
    ) {
        val messagesStringRes = if (isConversationsModeOn) {
            R.plurals.x_conversations_count
        } else {
            R.plurals.x_messages_count
        }

        MessageActionSheet.newInstance(
            actionSheetTarget = ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN,
            messagesIds = messagesIds,
            currentFolderLocationId = currentMailboxLocation.messageLocationTypeValue,
            mailboxLabelId = mailboxLabelId ?: currentMailboxLocation.messageLocationTypeValue.toString(),
            title = resources.getQuantityString(
                messagesStringRes,
                messagesIds.size,
                messagesIds.size
            )
        )
            .show(supportFragmentManager, MessageActionSheet::class.qualifiedName)
    }

    private fun showFoldersManager(messageIds: List<String>) {
        LabelsActionSheet.newInstance(
            messageIds = messageIds,
            currentFolderLocationId = currentMailboxLocation.messageLocationTypeValue,
            labelActionSheetType = LabelsActionSheet.Type.FOLDER,
            actionSheetTarget = ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN
        )
            .show(supportFragmentManager, LabelsActionSheet::class.qualifiedName)
    }

    private fun showLabelsManager(messageIds: List<String>) {
        LabelsActionSheet.newInstance(
            messageIds = messageIds,
            currentFolderLocationId = currentMailboxLocation.messageLocationTypeValue,
            actionSheetTarget = ActionSheetTarget.MAILBOX_ITEMS_IN_MAILBOX_SCREEN
        )
            .show(supportFragmentManager, LabelsActionSheet::class.qualifiedName)
    }

    /* SwipeRefreshLayout.OnRefreshListener */
    override fun onRefresh() {
        mailboxViewModel.checkConnectivity()
        syncUUID = UUID.randomUUID().toString()
        mailboxViewModel.refreshMessages()
    }

    private fun now() = System.currentTimeMillis() / 1000

    private fun switchToMailboxLocation(newLocation: Int) {
        val newMessageLocationType = fromInt(newLocation)
        setElevationOnToolbarAndStatusView(false)
        LoaderManager.getInstance(this).destroyLoader(LOADER_ID_LABELS_OFFLINE)
        if (actionMode != null) {
            actionMode!!.finish()
        }
        mailboxLabelId = null
        invalidateOptionsMenu()
        syncUUID = UUID.randomUUID().toString()
        setMailboxLocation(newMessageLocationType)
        setTitle()
        closeDrawer(animate = false)
        mailboxRecyclerView.clearFocus()
        mailboxRecyclerView.scrollToPosition(0)
        setUpMailboxActionsView()
        RefreshEmptyViewTask(
            WeakReference(this),
            counterDao,
            messagesDatabase,
            newMessageLocationType,
            mailboxLabelId
        ).execute()
    }

    private fun switchToMailboxCustomLocation(
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
        mSwipeProcessor.apply {
            addHandler(SwipeAction.TRASH, TrashSwipeHandler())
            addHandler(SwipeAction.SPAM, SpamSwipeHandler())
            addHandler(SwipeAction.STAR, StarSwipeHandler())
            addHandler(SwipeAction.ARCHIVE, ArchiveSwipeHandler())
            addHandler(SwipeAction.MARK_READ, MarkReadSwipeHandler())
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
                    val dontShowPlayServices = defaultSharedPreferences[PREF_DONT_SHOW_PLAY_SERVICES] ?: false
                    if (!dontShowPlayServices) {
                        showTwoButtonInfoDialog(
                            titleStringId = R.string.push_notifications_alert_title,
                            messageStringId = R.string.push_notifications_alert_subtitle,
                            leftStringId = R.string.dont_remind_again,
                            onNegativeButtonClicked = { defaultSharedPreferences[PREF_DONT_SHOW_PLAY_SERVICES] = true }
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

    private fun setMailboxLocation(locationToSet: MessageLocationType) {
        mailboxViewModel.setNewMailboxLocation(locationToSet)
    }

    private fun setNewLabel(labelId: String) {
        mailboxViewModel.setNewMailboxLabel(labelId)
    }

    private val fcmBroadcastReceiver: BroadcastReceiver = FcmBroadcastReceiver()

    private class OnMessageClickTask internal constructor(
        private val mailboxActivity: WeakReference<MailboxActivity>,
        private val messageDetailsRepository: MessageDetailsRepository,
        private val messageId: String,
        private val messageSubject: String,
        private val currentMailboxLocationType: Int
    ) : AsyncTask<Unit, Unit, Message>() {

        override fun doInBackground(vararg params: Unit): Message? =
            messageDetailsRepository.findMessageByIdBlocking(messageId)

        public override fun onPostExecute(savedMessage: Message?) {
            val mailboxActivity = mailboxActivity.get()
            val messageLocation = savedMessage?.locationFromLabel()
            if (messageLocation == MessageLocationType.DRAFT || messageLocation == MessageLocationType.ALL_DRAFT) {
                TryToOpenMessageTask(
                    this.mailboxActivity,
                    mailboxActivity?.pendingActionDao,
                    savedMessage.messageId,
                    savedMessage.isInline,
                    savedMessage.addressID
                ).execute()
            } else {
                val intent = AppUtil.decorInAppIntent(
                    Intent(
                        mailboxActivity, MessageDetailsActivity::class.java
                    )
                )
                intent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_OR_CONVERSATION_ID, messageId)
                intent.putExtra(
                    MessageDetailsActivity.EXTRA_MESSAGE_LOCATION_ID,
                    currentMailboxLocationType
                )
                intent.putExtra(MessageDetailsActivity.EXTRA_MAILBOX_LABEL_ID, mailboxActivity?.mailboxLabelId)
                intent.putExtra(MessageDetailsActivity.EXTRA_MESSAGE_SUBJECT, messageSubject)
                mailboxActivity?.startActivity(intent)
            }
        }
    }

    private class TryToOpenMessageTask internal constructor(
        private val mailboxActivity: WeakReference<MailboxActivity>,
        private val pendingActionDao: PendingActionDao?,
        private val messageId: String?,
        private val isInline: Boolean,
        private val addressId: String?
    ) : AsyncTask<Unit, Unit, Boolean>() {

        override fun doInBackground(vararg params: Unit): Boolean {
            // return true if message is not in sending process and can be opened
            val pendingForSending = pendingActionDao?.findPendingSendByMessageId(messageId!!)
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
            mailboxActivity.setElevationOnToolbarAndStatusView(false)
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
            mailboxActivity.setMailboxLocation(locationToSet)
            if (label != null) {
                val actionBar = mailboxActivity.supportActionBar
                if (actionBar != null) {
                    actionBar.title = label.name
                }
            }
            mailboxActivity.closeDrawer()
            mailboxActivity.mailboxRecyclerView.scrollToPosition(0)

            RefreshEmptyViewTask(
                this.mailboxActivity,
                mailboxActivity.counterDao,
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
                if ((mailboxRecyclerView.layoutManager as LinearLayoutManager?)!!.findFirstVisibleItemPosition() > 1) {
                    handler.postDelayed(750) {
                        val newMessageSnack =
                            Snackbar.make(
                                findViewById(R.id.drawer_layout),
                                getString(R.string.new_message_arrived),
                                Snackbar.LENGTH_LONG
                            ).apply { anchorView = mailboxActionsView }
                        val view = newMessageSnack.view
                        val tv =
                            view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                        tv.setTextColor(Color.WHITE)
                        newMessageSnack.show()
                    }
                }
                mailboxAdapter.notifyDataSetChanged()
            }
        }
    }

    private inner class SwipeController : ItemTouchHelper.Callback() {

        private var mailSettings: MailSettings? = null

        @Deprecated("Subscribe for changes instead of reloading on current User/MailSettings changed.")
        fun setCurrentMailSetting(mailSettings: MailSettings) {
            this.mailSettings = mailSettings
        }

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int {
            if (viewHolder is MessageViewHolder) {
                val mailboxLocation = currentMailboxLocation
                return if (mailboxLocation == MessageLocationType.DRAFT ||
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
            val mailboxItem = mailboxAdapter.getMailboxItem(position)
            val messageSwiped = SimpleMessage(mailboxItem)
            val mailboxLocation = currentMailboxLocation
            val settings = mailSettings ?: return
            val swipeActionOrdinal: Int = when (direction) {
                ItemTouchHelper.RIGHT -> settings.swipeRight?.value ?: SwipeAction.TRASH.ordinal
                ItemTouchHelper.LEFT -> settings.swipeLeft?.value ?: SwipeAction.ARCHIVE.ordinal
                else -> throw IllegalArgumentException("Unrecognised direction: $direction")
            }
            val swipeAction = normalise(SwipeAction.values()[swipeActionOrdinal], currentMailboxLocation)
            if (isConversationModeEnabled(mailboxLocation)) {
                mailboxViewModel.handleConversationSwipe(
                    swipeAction,
                    mailboxItem.itemId,
                    mailboxLocation,
                    mailboxLabelId ?: mailboxLocation.messageLocationTypeValue.toString()
                )
            } else {
                mSwipeProcessor.handleSwipe(swipeAction, messageSwiped, mJobManager, mailboxLabelId)
            }
            if (undoSnack != null && undoSnack!!.isShownOrQueued) {
                undoSnack!!.dismiss()
            }
            undoSnack = showUndoSnackbar(
                this@MailboxActivity,
                findViewById(R.id.drawer_layout),
                getString(swipeAction.actionDescription),
                {
                    mSwipeProcessor.handleUndo(swipeAction, messageSwiped, mJobManager, mailboxLocation, mailboxLabelId)
                    mailboxAdapter.notifyDataSetChanged()
                },
                false
            ).apply { anchorView = mailboxActionsView }
            if (!(swipeAction == SwipeAction.TRASH && currentMailboxLocation == MessageLocationType.DRAFT)) {
                undoSnack!!.show()
            }
            if (swipeCustomizeSnack != null && !customizeSwipeSnackShown) {
                handler.postDelayed(2750) {
                    swipeCustomizeSnack!!.show()
                    customizeSwipeSnackShown = true
                }
            }
            mailboxAdapter.notifyDataSetChanged()
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
                    currentMailboxLocation == MessageLocationType.DRAFT -> {
                        SwipeAction.TRASH.getActionBackgroundResource(deltaX < 0)
                    }
                    deltaX < 0 -> {
                        mailSettings?.swipeLeft?.let {
                            SwipeAction.values()[it.value].getActionBackgroundResource(false)
                        } ?: Resources.ID_NULL
                    }
                    else -> {
                        mailSettings?.swipeRight?.let {
                            SwipeAction.values()[it.value].getActionBackgroundResource(true)
                        } ?: Resources.ID_NULL
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

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(viewHolder, actionState)
            val isSwiping = actionState == ItemTouchHelper.ACTION_STATE_SWIPE;
            mailboxSwipeRefreshLayout.isEnabled = isSwiping.not();
        }
    }

}
