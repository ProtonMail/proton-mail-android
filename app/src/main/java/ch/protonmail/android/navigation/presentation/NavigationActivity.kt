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
package ch.protonmail.android.navigation.presentation

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.StartContacts
import ch.protonmail.android.activities.StartReportBugs
import ch.protonmail.android.activities.StartSettings
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.api.segments.event.FetchUpdatesJob
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.CreateItem
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.Primary
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.Primary.Static.Type
import ch.protonmail.android.drawer.presentation.model.DrawerLabelUiModel
import ch.protonmail.android.drawer.presentation.ui.view.ProtonSideDrawer
import ch.protonmail.android.feature.account.AccountStateManager
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.ui.EXTRA_MANAGE_FOLDERS
import ch.protonmail.android.labels.presentation.ui.LabelsManagerActivity
import ch.protonmail.android.servers.notification.EXTRA_USER_ID
import ch.protonmail.android.settings.pin.ValidatePinActivity
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.setDrawBehindSystemBars
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.presentation.view.AccountPrimaryView
import me.proton.core.accountmanager.presentation.viewmodel.AccountSwitcherViewModel
import me.proton.core.domain.entity.UserId
import me.proton.core.presentation.utils.setDarkStatusBar
import me.proton.core.presentation.utils.setLightStatusBar
import javax.inject.Inject

// region constants
const val EXTRA_FIRST_LOGIN = "extra.first.login"

const val REQUEST_CODE_ACCOUNT_MANAGER = 997
const val REQUEST_CODE_SNOOZED_NOTIFICATIONS = 555

/**
 * Set drawer behind system bars only on Android 11, as Drawer items don't fit correctly below API 30
 *  ( fitsSystemWindow = true doesn't work as expected )
 *
 * Tracked on MAILAND-2123
 */
private val SHOULD_DRAW_DRAWER_BEHIND_SYSTEM_BARS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
// endregion

/**
 * Base activity that offers methods for the navigation drawer. Extend from this if your activity
 * needs support for the navigation drawer.
 */
@AndroidEntryPoint
internal abstract class NavigationActivity : BaseActivity() {

    // region views
    private val toolbar by lazy { findViewById<Toolbar>(R.id.toolbar) }
    private val drawerLayout: DrawerLayout by lazy { findViewById(R.id.drawer_layout) }
    private val sideDrawer: ProtonSideDrawer by lazy { findViewById(R.id.sideDrawer) }
    private val accountPrimaryView: AccountPrimaryView by lazy { findViewById(R.id.accountPrimaryView) }
    private lateinit var drawerToggle: ActionBarDrawerToggle
    // endregion

    @Inject
    lateinit var accountManager: AccountManager

    @Inject
    lateinit var databaseProvider: DatabaseProvider

    @Inject
    lateinit var userManager: UserManager

    private val accountSwitcherViewModel by viewModels<AccountSwitcherViewModel>()
    private val navigationViewModel by viewModels<NavigationViewModel>()

    private val startSettingsLauncher = registerForActivityResult(StartSettings()) {}
    private val startContactsLauncher = registerForActivityResult(StartContacts()) {}
    private val startReportBugsLauncher = registerForActivityResult(StartReportBugs()) {}

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

    protected open fun onAccountSwitched(switch: AccountStateManager.AccountSwitch) {
        val currentUsername = switch.current?.username
        if (switch.previous != null && currentUsername != null) {
            val message = String.format(getString(R.string.signed_in_with), currentUsername)
            val snackBar = Snackbar.make(drawerLayout, message, Snackbar.LENGTH_SHORT)
            snackBar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
                .setTextColor(this.getColor(R.color.text_inverted))
            snackBar.show()
        }
    }

    open fun onPrimaryUserId(userId: UserId) {
        // Fetch the events immediately after switching the users to avoid the mailbox refresh
        // when the first scheduled event comes in at a later point.
        // TODO: Investigate why the first event after logging in triggers a refresh to start with; MAILAND-2654
        app.startJobManager()
        mJobManager.addJobInBackground(FetchUpdatesJob())
    }

    protected abstract fun onInbox(type: Constants.DrawerOptionType)

    protected abstract fun onOtherMailBox(type: Constants.DrawerOptionType)

    protected abstract fun onLabelMailBox(
        type: Constants.DrawerOptionType,
        labelId: String,
        labelName: String,
        isFolder: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {

        if (SHOULD_DRAW_DRAWER_BEHIND_SYSTEM_BARS) {
            // This is needed for the status bar to change correctly, it doesn't without this. Is there a way to mime
            //  the behaviour with newer API?
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            setDrawBehindSystemBars()
        }

        super.onCreate(savedInstanceState)

        with(accountStateManager) {
            register(this@NavigationActivity)

            // Start Splash on AccountNeeded.
            state
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .onEach {
                    when (it) {
                        AccountStateManager.State.Processing,
                        AccountStateManager.State.PrimaryExist -> Unit
                        AccountStateManager.State.AccountNeeded -> addAccount()
                    }
                }.launchIn(lifecycleScope)

            onAddAccountClosed {
                if (userManager.currentUserId == null) {
                    finish()
                }
            }

            onAccountSwitched()
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .onEach { switch -> onAccountSwitched(switch) }
                .launchIn(lifecycleScope)

            getPrimaryUserId().filterNotNull()
                .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
                .filter { userId -> navigationViewModel.verifyPrimaryUserId(userId) }
                .onEach { userId -> onPrimaryUserId(userId) }
                .launchIn(lifecycleScope)
        }

        accountPrimaryView.setViewModel(accountSwitcherViewModel)
        accountSwitcherViewModel.onAction()
            .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
            // Handle Dialog/Drawer state.
            .onEach {
                val primaryUserId = accountStateManager.getPrimaryUserId().value

                fun dismiss() {
                    closeDrawerAndDialog()
                }

                fun dismissIfPrimary(userId: UserId) {
                    if (primaryUserId == userId) dismiss()
                }

                when (it) {
                    is AccountSwitcherViewModel.Action.Add,
                    is AccountSwitcherViewModel.Action.SetPrimary,
                    is AccountSwitcherViewModel.Action.SignIn -> dismiss()
                    is AccountSwitcherViewModel.Action.Remove -> dismissIfPrimary(it.account.userId)
                    is AccountSwitcherViewModel.Action.SignOut -> dismissIfPrimary(it.account.userId)
                }
            }
            // Handle action.
            .onEach {
                when (it) {
                    is AccountSwitcherViewModel.Action.Add -> accountStateManager.signIn()
                    is AccountSwitcherViewModel.Action.SetPrimary -> accountStateManager.switch(it.account.userId)
                    is AccountSwitcherViewModel.Action.SignIn -> accountStateManager.signIn(it.account.userId)
                    is AccountSwitcherViewModel.Action.Remove -> accountStateManager.remove(it.account.userId)
                    is AccountSwitcherViewModel.Action.SignOut -> accountStateManager.signOut(it.account.userId)
                }
            }
            .launchIn(lifecycleScope)

        fun launchCreateLabel() {
            val createLabelIntent = AppUtil.decorInAppIntent(
                Intent(this, LabelsManagerActivity::class.java)
            )
            startActivity(createLabelIntent)
        }
        fun launchCreateFolder() {
            val createFolderIntent = AppUtil.decorInAppIntent(
                Intent(this, LabelsManagerActivity::class.java)
            ).putExtra(EXTRA_MANAGE_FOLDERS, true)
            startActivity(createFolderIntent)
        }
        sideDrawer.setClickListeners(
            onItemClick = { drawerItem ->
                when (drawerItem) {
                    is Primary -> {
                        onDrawerClose = {
                            if (drawerItem is Primary.Static) onDrawerStaticItemSelected(drawerItem.type)
                            else if (drawerItem is Primary.Label) onDrawerLabelSelected(drawerItem.uiModel)
                        }
                        drawerLayout.closeDrawer(GravityCompat.START)
                    }
                    is CreateItem.Folder -> launchCreateFolder()
                    is CreateItem.Label -> launchCreateLabel()
                    else -> {}
                }
            },
            onCreateLabel = ::launchCreateLabel,
            onCreateFolder = ::launchCreateFolder
        )

        setUpDrawer()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (SHOULD_DRAW_DRAWER_BEHIND_SYSTEM_BARS)
            if (navigationViewModel.isAppInDarkMode(this)) {
                setDarkStatusBar()
            } else {
                setLightStatusBar()
            }

        checkUserId()
        closeDrawerAndDialog()

        val alarmReceiver = AlarmReceiver()
        alarmReceiver.setAlarm(this)
    }

    override fun onStart() {
        super.onStart()
        // events updates
        mApp.bus.register(this)
    }

    override fun onStop() {
        super.onStop()
        app.bus.unregister(this)
    }

    private fun checkUserId() {
        // Requested UserId match the current ?
        intent.getStringExtra(EXTRA_USER_ID)?.let { extraUserId ->
            intent.removeExtra(EXTRA_USER_ID)
            val requestedUserId = UserId(extraUserId)
            if (requestedUserId != accountStateManager.getPrimaryUserId().value) {
                accountStateManager.switch(requestedUserId)
            }
        }
    }

    private fun closeDrawerAndDialog() {
        closeDrawer(animate = false)
        accountPrimaryView.dismissDialog()
    }

    protected fun closeDrawer(animate: Boolean = true): Boolean {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START, animate)
            return true
        }
        return false
    }

    protected fun setUpDrawer() {
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.open_drawer,
            R.string.close_drawer
        )
        drawerLayout.setStatusBarBackgroundColor(
            UiUtil.scaleColor(
                getColor(R.color.dark_purple),
                0.6f,
                true
            )
        )
        drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
        setUpInitialDrawerItems(userManager.currentLegacyUser?.isUsePin ?: false)

        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {

            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                if (SHOULD_DRAW_DRAWER_BEHIND_SYSTEM_BARS) setDarkStatusBar()
            }

            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                if (SHOULD_DRAW_DRAWER_BEHIND_SYSTEM_BARS)
                    if (navigationViewModel.isAppInDarkMode(this@NavigationActivity)) {
                        setDarkStatusBar()
                    } else {
                        setLightStatusBar()
                    }

                onDrawerClose()
                onDrawerClose = {}
            }
        })
    }

    private fun setUpInitialDrawerItems(isPinEnabled: Boolean) {
        val hasPin = isPinEnabled && userManager.getMailboxPin() != null

        sideDrawer.setLocationItems(
            listOf(
                Primary.Static(Type.INBOX, R.string.inbox, R.drawable.ic_inbox),
                Primary.Static(Type.DRAFTS, R.string.drafts, R.drawable.ic_drafts),
                Primary.Static(Type.SENT, R.string.sent, R.drawable.ic_paper_plane),
                Primary.Static(Type.STARRED, R.string.starred, R.drawable.ic_star),
                Primary.Static(Type.ARCHIVE, R.string.archive, R.drawable.ic_archive),
                Primary.Static(Type.SPAM, R.string.spam, R.drawable.ic_fire),
                Primary.Static(Type.TRASH, R.string.trash, R.drawable.ic_trash),
                Primary.Static(Type.ALLMAIL, R.string.all_mail, R.drawable.ic_envelope_all_emails)
            )
        )

        sideDrawer.setMoreItems(
            R.string.x_more,
            listOfNotNull(
                Primary.Static(Type.SETTINGS, R.string.drawer_settings, R.drawable.ic_sliders_two),
                Primary.Static(Type.CONTACTS, R.string.drawer_contacts, R.drawable.ic_book_contacts),
                Primary.Static(Type.REPORT_BUGS, R.string.drawer_report_bug, R.drawable.ic_bug),
                if (hasPin) Primary.Static(Type.LOCK, R.string.drawer_lock_the_app, R.drawable.ic_lock)
                else null,
                Primary.Static(Type.SIGNOUT, R.string.drawer_sign_out, R.drawable.ic_sign_out)
            )
        )

        sideDrawer.setFooterText(
            getString(R.string.x_app_version_name_code, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
        )
    }

    private fun onDrawerStaticItemSelected(type: Type) {

        fun onSignOutSelected() {

            fun onLogoutConfirmed(currentUserId: UserId) {
                accountStateManager.signOut(currentUserId)
            }

            lifecycleScope.launch {
                val nextLoggedInUserId = userManager.getPreviousCurrentUserId()

                val (title, message) = if (nextLoggedInUserId != null) {
                    val next = userManager.getUser(nextLoggedInUserId)
                    getString(R.string.sign_out) to getString(R.string.logout_question_next_account, next.name.s)
                } else {
                    val current = checkNotNull(userManager.currentUser)
                    getString(R.string.sign_out, current.name.s) to getString(R.string.sign_out_question)
                }

                showTwoButtonInfoDialog(
                    title = title,
                    message = message,
                    rightStringId = R.string.yes,
                    leftStringId = R.string.no
                ) {
                    onLogoutConfirmed(checkNotNull(userManager.currentUserId))
                }
            }
        }

        when (type) {
            Type.SIGNOUT -> onSignOutSelected()
            Type.CONTACTS -> startContactsLauncher.launch(Unit)
            Type.REPORT_BUGS -> startReportBugsLauncher.launch(Unit)
            Type.SETTINGS -> startSettingsLauncher.launch(
                StartSettings.Input(currentMailboxLocation, currentLabelId)
            )
            Type.INBOX -> onInbox(type.drawerOptionType)
            Type.ARCHIVE, Type.STARRED, Type.DRAFTS, Type.SENT, Type.TRASH, Type.SPAM, Type.ALLMAIL ->
                onOtherMailBox(type.drawerOptionType)
            Type.LOCK -> {
                val user = userManager.currentLegacyUser
                if (user != null && user.isUsePin && userManager.getMailboxPin() != null) {
                    user.setManuallyLocked(true)
                    val intent = Intent(this, ValidatePinActivity::class.java)
                    startActivity(intent)
                }
            }
            Type.LABEL -> { /* We don't need it, perhaps we could remove the value from enum */
            }
        }
    }

    private fun onDrawerLabelSelected(label: DrawerLabelUiModel) {
        val isFolder = label.type == LabelType.FOLDER
        onLabelMailBox(Constants.DrawerOptionType.LABEL, label.labelId, label.name, isFolder)
    }

}
