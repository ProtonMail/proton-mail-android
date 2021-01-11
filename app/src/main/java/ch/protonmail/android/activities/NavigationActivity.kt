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
package ch.protonmail.android.activities

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.activities.dialogs.QuickSnoozeDialogFragment
import ch.protonmail.android.activities.mailbox.MailboxActivity
import ch.protonmail.android.activities.multiuser.AccountManagerActivity
import ch.protonmail.android.activities.multiuser.ConnectAccountActivity
import ch.protonmail.android.activities.multiuser.EXTRA_USERNAME
import ch.protonmail.android.activities.navigation.LabelWithUnreadCounter
import ch.protonmail.android.activities.navigation.NavigationViewModel
import ch.protonmail.android.activities.settings.EXTRA_CURRENT_MAILBOX_LABEL_ID
import ch.protonmail.android.activities.settings.EXTRA_CURRENT_MAILBOX_LOCATION
import ch.protonmail.android.adapters.AccountsAdapter
import ch.protonmail.android.adapters.DrawerAdapter
import ch.protonmail.android.adapters.mapLabelsToDrawerLabels
import ch.protonmail.android.adapters.setUnreadLocations
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.local.SnoozeSettings
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.room.counters.CountersDatabaseFactory
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.api.segments.event.FetchUpdatesJob
import ch.protonmail.android.contacts.ContactsActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.ForceSwitchedAccountNotifier
import ch.protonmail.android.jobs.FetchMessageCountsJob
import ch.protonmail.android.mapper.LabelUiModelMapper
import ch.protonmail.android.settings.pin.ValidatePinActivity
import ch.protonmail.android.uiModel.DrawerItemUiModel
import ch.protonmail.android.uiModel.DrawerItemUiModel.Primary
import ch.protonmail.android.uiModel.DrawerItemUiModel.Primary.Static.Type
import ch.protonmail.android.uiModel.DrawerUserModel
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.uiModel.setLabels
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.getColorCompat
import ch.protonmail.android.utils.extensions.ifNullElse
import ch.protonmail.android.utils.resettableLazy
import ch.protonmail.android.utils.resettableManager
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import ch.protonmail.android.views.DrawerHeaderView
import kotlinx.android.synthetic.main.drawer_header.*
import java.util.*
import javax.inject.Inject

// region constants
const val EXTRA_FIRST_LOGIN = "EXTRA_FIRST_LOGIN"
const val EXTRA_SWITCHED_USER = "EXTRA_SWITCHED_USER"
const val EXTRA_SWITCHED_TO_USER = "EXTRA_SWITCHED_TO_USER"
const val EXTRA_LOGOUT = "EXTRA_LOGOUT"

const val REQUEST_CODE_ACCOUNT_MANAGER = 997
const val REQUEST_CODE_SWITCHED_USER = 999
const val REQUEST_CODE_SNOOZED_NOTIFICATIONS = 555
// endregion

/**
 * Created by dkadrikj on 19.7.15.
 * Base activity that offers methods for the navigation drawer. Extend from this if your activity
 * needs support for the navigation drawer.
 *
 *
 * Note: These methods were all in the [MailboxActivity] class, but since it was
 * approaching 1000 lines of code, I had to split it this way. [MailboxActivity] needs more
 * refactoring because it still has more than 700 lines of code.
 */

abstract class NavigationActivity :
    BaseActivity(),
    DrawerHeaderView.IDrawerHeaderListener,
    QuickSnoozeDialogFragment.IQuickSnoozeListener {

    // region views
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val drawerLayout: DrawerLayout by lazy { findViewById<DrawerLayout>(R.id.drawer_layout) }
    private val navigationDrawerRecyclerView by lazy { findViewById<RecyclerView>(R.id.left_drawer_navigation) }
    private val navigationDrawerUsersRecyclerView by lazy { findViewById<RecyclerView>(R.id.left_drawer_users) }
    protected var overlayDialog: Dialog? = null
    protected lateinit var drawerToggle: ActionBarDrawerToggle
    // endregion

    /**
     * [DrawerAdapter] for the Drawer. Now all the elements in the Drawer are handled by this
     * Adapter
     */
    private val drawerAdapter = DrawerAdapter()

    /**
     * [AccountsAdapter] for the Drawer. It is used as a replacement to the default [navigationDrawerRecyclerView]
     * to display the users (logged in and recently logged out) of the application.
     */
    private val accountsAdapter = AccountsAdapter()

    /** [DrawerItemUiModel.Header] for the Drawer  */
    private var drawerHeader: DrawerItemUiModel.Header? = null

    /**
     * List of [DrawerItemUiModel] that are static from the app, with relative
     * [DrawerItemUiModel.Divider]s
     */
    private var staticDrawerItems: List<DrawerItemUiModel> = ArrayList()

    /** List of [DrawerItemUiModel.Primary.Label] for the Drawer  */
    private var drawerLabels: List<Primary.Label> = ArrayList()

    val lazyManager = resettableManager()

    private val countersDatabase by resettableLazy(lazyManager) {
        CountersDatabaseFactory.getInstance(applicationContext).getDatabase()
    }

    val messagesDatabase by resettableLazy(lazyManager) {
        MessagesDatabaseFactory.getInstance(applicationContext).getDatabase()
    }

    @Inject
    lateinit var databaseProvider: DatabaseProvider

    private val navigationViewModel by resettableLazy(lazyManager) {
        ViewModelProviders.of(this@NavigationActivity, navigationViewModelFactory).get(NavigationViewModel::class.java)
    }

    private val navigationViewModelFactory by resettableLazy(lazyManager) {
        NavigationViewModel.Factory(databaseProvider)
    }

    protected abstract val currentMailboxLocation: Constants.MessageLocationType

    protected abstract val currentLabelId: String?

    /**
     * A lambda that holds an operation that needs to be executed after the Drawer has been closed
     *
     * Note by Davide: I guess this is a workaround for avoid the Drawer's animation stuttering
     * while the other component is loading
     * TODO: Optimize loading and remove this delay
     */
    private var onDrawerClose: () -> Unit = {}

    init {
        drawerAdapter.onItemClick = { drawerItem ->
            // Header clicked
            if (drawerItem is DrawerItemUiModel.Header) {
                onQuickSnoozeClicked()
                // Primary item clicked
            } else if (drawerItem is Primary) {
                onDrawerClose = {

                    // Static item clicked
                    if (drawerItem is Primary.Static) selectItem(drawerItem.type)

                    // Label clicked
                    else if (drawerItem is Primary.Label) onNavLabelItemClicked(drawerItem.uiModel)
                }
                drawerAdapter.setSelected(drawerItem)
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        }

        accountsAdapter.onItemClick = { account ->
            if (account is DrawerUserModel.BaseUser) {
                val currentActiveAccount = userManager.username
                if (!AccountManager.getInstance(this).getLoggedInUsers().contains(account.name)) {
                    val intent = Intent(this, ConnectAccountActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    intent.putExtra(EXTRA_USERNAME, account.name)
                    ContextCompat.startActivity(this, AppUtil.decorInAppIntent(intent), null)
                } else if (currentActiveAccount != account.name) {
                    switchAccountProcedure(account.name)
                }
            }
        }
    }

    protected abstract fun onLogout()

    protected abstract fun onSwitchedAccounts()

    protected abstract fun onInbox(type: Constants.DrawerOptionType)

    protected abstract fun onOtherMailBox(type: Constants.DrawerOptionType)

    protected abstract fun onLabelMailBox(
        type: Constants.DrawerOptionType,
        labelId: String,
        labelName: String,
        isFolder: Boolean
    )

    override fun onStart() {
        super.onStart()
        // events updates
        mApp.bus.register(this)
    }

    override fun onResume() {
        super.onResume()
        ProtonMailApplication.getApplication().startJobManager()
        mJobManager.addJobInBackground(FetchUpdatesJob())
        val alarmReceiver = AlarmReceiver()
        alarmReceiver.setAlarm(this)
    }

    override fun onStop() {
        super.onStop()
        mApp.bus.unregister(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_ACCOUNT_MANAGER) {
            onLogout()
        } else if (resultCode == RESULT_OK && requestCode == REQUEST_CODE_SNOOZED_NOTIFICATIONS) {
            refreshDrawerHeader(userManager.user)
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    protected fun switchAccountProcedure(accountName: String) {
        userManager.switchToAccount(accountName)
        onSwitchedAccounts()
        DialogUtils.showSignedInSnack(drawerLayout, String.format(getString(R.string.signed_in_with), accountName))
    }

    @JvmOverloads
    protected fun closeDrawer(ignoreIfPossible: Boolean = false): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            var closeIt = true
            if (drawerHeaderView.state == DrawerHeaderView.State.OPENED) {
                onUserClicked(false)
                drawerHeaderView.switchState()
                if (!ignoreIfPossible) {
                    closeIt = false
                }
            }
            if (closeIt) {
                drawerLayout.closeDrawers()
            }
            return true
        }
        return false
    }

    protected fun setUpDrawer() {
        navigationViewModel.reloadDependencies()
        drawerToggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.open_drawer, R.string.close_drawer
        )
        drawerLayout.setStatusBarBackgroundColor(UiUtil.scaleColor(getColorCompat(R.color.dark_purple), 0.6f, true))
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)

        setUpInitialDrawerItems(mUserManager.user)

        refreshDrawer()

        // LayoutManager set from xml
        navigationDrawerRecyclerView.adapter = drawerAdapter

        navigationDrawerUsersRecyclerView.adapter = accountsAdapter

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                if (drawerHeaderView.state == DrawerHeaderView.State.OPENED) {
                    onUserClicked(false)
                    drawerHeaderView.switchState()
                }
                drawerToggle.syncState()
                navigationDrawerRecyclerView!!.smoothScrollToPosition(0)
                onDrawerClose()
                onDrawerClose = {}
            }
        })

        navigationViewModel.labelsWithUnreadCounterLiveData().observe(this, CreateLabelsMenuObserver())
        navigationViewModel.locationsUnreadLiveData().observe(this, LocationsMenuObserver())
        refreshDrawerHeader(mUserManager.user)

        for (username in AccountManager.getInstance(this).getLoggedInUsers()) {
            val tokenManager = mUserManager.getTokenManager(username)
            if (tokenManager != null && !tokenManager.scope.toLowerCase().split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray().contains(Constants.TOKEN_SCOPE_FULL)) {
                val nextLoggedInAccount = userManager.nextLoggedInAccountOtherThanCurrent
                mUserManager.logoutAccount(username)
                nextLoggedInAccount?.let {
                    Handler(Looper.getMainLooper()).postDelayed(
                        {
                            switchAccountProcedure(it)
                        },
                        100
                    )
                }
                onSwitchedAccounts()
            }
        }

        setupAccountsList()
        ForceSwitchedAccountNotifier.notifier.postValue(null)
    }

    protected fun setupAccountsList() {
        navigationViewModel.reloadDependencies()
        navigationViewModel.notificationsCounts()
        navigationViewModel.notificationsCounterLiveData.observe(
            this,
            { map ->
                val accountsManager = AccountManager.getInstance(this)
                val currentPrimaryAccount = mUserManager.username
                val accounts = accountsManager.getLoggedInUsers().sortedByDescending {
                    it == currentPrimaryAccount
                }.map {
                    val userAddresses = userManager.getUser(it).addresses
                    val primaryAddress = userAddresses.find { address ->
                        address.type == Constants.ADDRESS_TYPE_PRIMARY
                    }
                    val primaryAddressEmail = if (primaryAddress == null) {
                        it
                    } else {
                        primaryAddress.email
                    }
                    val displayName = primaryAddress?.displayName ?: ""
                    DrawerUserModel.BaseUser.DrawerUser(
                        it,
                        primaryAddressEmail,
                        true,
                        map[it] ?: 0,
                        areNotificationSnoozed(it),
                        if (displayName.isNotEmpty()) displayName else it
                    )
                } as MutableList<DrawerUserModel>
                accounts.addAll(
                    accountsManager.getSavedUsers().map {
                        DrawerUserModel.BaseUser.DrawerUser(
                            name = it,
                            loggedIn = false,
                            notifications = 0,
                            notificationsSnoozed = false,
                            displayName = it
                        )
                    } as MutableList<DrawerUserModel>
                )
                // todo fetch all user missing data (email, notifications etc)
                accounts.add(DrawerUserModel.Footer)
                accountsAdapter.items = accounts
            }
        )
    }

    private fun areNotificationSnoozed(username: String): Boolean {
        val rightNow = Calendar.getInstance()
        val snoozeSettings = SnoozeSettings.load(username)
        var areSnoozed = false
        snoozeSettings.ifNullElse(
            {},
            {
                val shouldShowNotification = !snoozeSettings.shouldSuppressNotification(rightNow)
                val isQuickSnoozeEnabled = snoozeSettings.snoozeQuick
                val isScheduledSnoozeEnabled = snoozeSettings.getScheduledSnooze(username)
                areSnoozed = isQuickSnoozeEnabled || (isScheduledSnoozeEnabled && !shouldShowNotification)
            }
        )
        return areSnoozed
    }

    private fun setUpInitialDrawerItems(user: User?) {
        val hasPin = user != null &&
            user.isUsePin &&
            mUserManager.getMailboxPin() != null

        staticDrawerItems = mutableListOf<DrawerItemUiModel>().apply {
            add(Primary.Static(Type.INBOX, R.string.inbox, R.drawable.inbox))
            add(Primary.Static(Type.SENT, R.string.sent, R.drawable.sent))
            add(Primary.Static(Type.DRAFTS, R.string.drafts, R.drawable.draft))
            add(Primary.Static(Type.STARRED, R.string.starred, R.drawable.starred))
            add(Primary.Static(Type.ARCHIVE, R.string.archive, R.drawable.archive))
            add(Primary.Static(Type.SPAM, R.string.spam, R.drawable.spam))
            add(Primary.Static(Type.TRASH, R.string.trash, R.drawable.trash))
            add(Primary.Static(Type.ALLMAIL, R.string.all_mail, R.drawable.allmail))
            add(DrawerItemUiModel.Divider)
            add(Primary.Static(Type.CONTACTS, R.string.contacts, R.drawable.contact))
            add(Primary.Static(Type.SETTINGS, R.string.settings, R.drawable.settings))
            add(Primary.Static(Type.REPORT_BUGS, R.string.report_bugs, R.drawable.bug))
            if (hasPin) {
                add(Primary.Static(Type.LOCK, R.string.lock_the_app, R.drawable.notification_icon))
            }
            add(Primary.Static(Type.UPSELLING, R.string.upselling, R.drawable.cubes))
            add(Primary.Static(Type.SIGNOUT, R.string.logout, R.drawable.signout))
        }.toList()
    }

    protected fun refreshDrawerHeader(user: User) {
        val addresses = user.addresses

        if (addresses != null && addresses.size > 0) {
            val address = addresses[0]

            drawerHeader = DrawerItemUiModel.Header(
                address.displayName,
                address.email,
                areNotificationSnoozed(mUserManager.username)
            )
            drawerHeaderView.setUser(address.displayName, address.email)
            drawerHeaderView.refresh(areNotificationSnoozed(mUserManager.username))
            refreshDrawer()
        }
    }

    /** Creates a properly formatted List for the Drawer and deliver to the Adapter  */
    fun refreshDrawer() {
        drawerAdapter.items = staticDrawerItems.setLabels(drawerLabels)
    }

    override fun onQuickSnoozeClicked() {
        val quickSnoozeDialogFragment = QuickSnoozeDialogFragment.newInstance()
        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(quickSnoozeDialogFragment, quickSnoozeDialogFragment.fragmentKey)
        transaction.commitAllowingStateLoss()
    }

    override fun onUserClicked(open: Boolean) {
        navigationDrawerRecyclerView.visibility = if (open) View.GONE else View.VISIBLE
        navigationDrawerUsersRecyclerView.visibility = if (open) View.VISIBLE else View.GONE
    }

    override fun onQuickSnoozeSet(enabled: Boolean) {
        drawerHeader = drawerHeader?.copy(snoozeEnabled = enabled)
        refreshDrawer()
        refreshDrawerHeader(mUserManager.user)
        setupAccountsList()
    }

    protected fun reloadMessageCounts() {
        mJobManager.addJobInBackground(FetchMessageCountsJob(null))
    }

    private fun selectItem(type: Type) {
        when (type) {
            Type.SIGNOUT -> {
                val clickListener = { dialog: DialogInterface, which: Int ->
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        val bar = findViewById<View>(R.id.spinner_layout)
                        if (bar != null) {
                            bar.visibility = View.VISIBLE
                        }
                        val nextLoggedInAccount = userManager.nextLoggedInAccountOtherThanCurrent
                        if (nextLoggedInAccount == null) {
                            overlayDialog = Dialog(this@NavigationActivity, android.R.style.Theme_Panel)
                            overlayDialog!!.setCancelable(false)
                            overlayDialog!!.show()
                            mUserManager.logoutLastActiveAccount()
                            onLogout()
                        } else {
                            mUserManager.logoutAccount(userManager.username)
                            onSwitchedAccounts()
                        }
                    }

                    dialog.dismiss()

                }
                if (!isFinishing) {
                    val nextLoggedInAccount = userManager.nextLoggedInAccountOtherThanCurrent
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(if (nextLoggedInAccount != null) getString(R.string.logout) else String.format(getString(R.string.log_out), mUserManager.username))
                        .setMessage(if (nextLoggedInAccount != null) String.format(getString(R.string.logout_question_next_account), nextLoggedInAccount) else getString(R.string.logout_question))
                        .setNegativeButton(R.string.no, clickListener)
                        .setPositiveButton(R.string.yes, clickListener)
                        .create()
                        .show()
                }

            }
            Type.CONTACTS -> startActivity(AppUtil.decorInAppIntent(Intent(this, ContactsActivity::class.java)))
            Type.REPORT_BUGS -> startActivity(AppUtil.decorInAppIntent(Intent(this, ReportBugsActivity::class.java)))
            Type.SETTINGS -> {
                val settingsIntent = AppUtil.decorInAppIntent(Intent(this@NavigationActivity, SettingsActivity::class.java))
                settingsIntent.putExtra(EXTRA_CURRENT_MAILBOX_LOCATION, currentMailboxLocation.messageLocationTypeValue)
                settingsIntent.putExtra(EXTRA_CURRENT_MAILBOX_LABEL_ID, currentLabelId)
                startActivity(settingsIntent)
            }
            Type.ACCOUNT_MANAGER -> {
                startActivityForResult(AppUtil.decorInAppIntent(Intent(this@NavigationActivity, AccountManagerActivity::class.java)), REQUEST_CODE_ACCOUNT_MANAGER)
            }
            Type.INBOX -> onInbox(type.drawerOptionType)
            Type.ARCHIVE, Type.STARRED, Type.DRAFTS, Type.SENT, Type.TRASH, Type.SPAM, Type.ALLMAIL -> onOtherMailBox(type.drawerOptionType)
            Type.UPSELLING -> startActivity(AppUtil.decorInAppIntent(Intent(this, UpsellingActivity::class.java)))
            Type.LOCK -> {
                val user = mUserManager.user
                if (user.isUsePin && ProtonMailApplication.getApplication().userManager.getMailboxPin() != null) {
                    user.setManuallyLocked(true)
                    user.save()
                    val pinIntent = AppUtil.decorInAppIntent(Intent(this@NavigationActivity, ValidatePinActivity::class.java))
                    startActivityForResult(pinIntent, REQUEST_CODE_VALIDATE_PIN)
                }
            }
            Type.LABEL -> { /* We don't need it, perhaps we could remove the value from enum */
            }
        }
    }

    private fun onNavLabelItemClicked(label: LabelUiModel) {
        val exclusive = label.type == LabelUiModel.Type.FOLDERS
        onLabelMailBox(Constants.DrawerOptionType.LABEL, label.labelId, label.name, exclusive)
    }

    override fun getUserManager(): UserManager {
        return mUserManager
    }

    private inner class CreateLabelsMenuObserver : Observer<List<LabelWithUnreadCounter>> {
        override fun onChanged(labels: List<LabelWithUnreadCounter>?) {
            if (labels == null)
                return

            // Get a mapper for create LabelUiModels. TODO this dependency could be handled better
            val mapper = LabelUiModelMapper( /* isLabelEditable */false)

            // Prepare new Labels for the Adapter
            drawerLabels = mapLabelsToDrawerLabels(mapper, labels)
            refreshDrawer()
        }
    }

    private inner class LocationsMenuObserver : Observer<Map<Int, Int>> {
        override fun onChanged(unreadLocations: Map<Int, Int>) {
            // Prepare drawer Items by injecting unreadLocations
            staticDrawerItems = staticDrawerItems.setUnreadLocations(unreadLocations)
                .toMutableList()
            refreshDrawer()
        }
    }
}
