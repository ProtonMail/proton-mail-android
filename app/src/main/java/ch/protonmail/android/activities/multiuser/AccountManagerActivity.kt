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
package ch.protonmail.android.activities.multiuser

import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.multiuser.viewModel.AccountManagerViewModel
import ch.protonmail.android.adapters.AccountsAdapter
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.uiModel.DrawerUserModel
import ch.protonmail.android.utils.extensions.setBarColors
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToMailbox
import ch.protonmail.android.utils.moveToMailboxLogout
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import com.squareup.otto.Subscribe
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_account_manager.*
import kotlinx.android.synthetic.main.toolbar_white.*
import javax.inject.Inject

@AndroidEntryPoint
class AccountManagerActivity : BaseActivity() {

    @Inject
    lateinit var viewModelFactory: AccountManagerViewModel.Factory

    private var movingToMailbox = false

    /** A Lazy instance of [AccountManagerViewModel] */
    private val viewModel by lazy {
        ViewModelProviders.of(this, viewModelFactory)
                .get(AccountManagerViewModel::class.java)
    }

    /**
     * [AccountsAdapter] for the Accounts RecyclerView. It is used to the default [accountsRecyclerView]
     * to display the users (logged in and recently logged out) of the application.
     */
    private val accountsAdapter by lazy { AccountsAdapter() }

    override fun getLayoutId() = R.layout.activity_account_manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar.setNavigationIcon(R.drawable.ic_close)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        window?.apply {
            setBarColors(resources.getColor(R.color.new_purple_dark))
        }

        val nextLoggedInAccount = mUserManager.nextLoggedInAccountOtherThanCurrent
        val currentActiveAccount = mUserManager.username
        accountsAdapter.apply {
            onLogoutAccount = { username ->
                DialogUtils.showInfoDialogWithTwoButtons(this@AccountManagerActivity,
                        if (nextLoggedInAccount != null && username == currentActiveAccount) getString(R.string.logout) else String.format(getString(R.string.log_out), username),
                        if (nextLoggedInAccount != null && username == currentActiveAccount) String.format(getString(R.string.logout_question_next_account), nextLoggedInAccount) else getString(R.string.logout_question),
                        getString(R.string.cancel),
                        getString(R.string.okay), {
                    viewModel.logoutAccountResult.observe(this@AccountManagerActivity, Observer(::closeActivity))
                    viewModel.logoutAccount(username)
                }, false)
            }
            onRemoveAccount = { username ->
                DialogUtils.showInfoDialogWithTwoButtons(this@AccountManagerActivity,
                        getString(R.string.logout), String.format(getString(R.string.remove_account_question), username), getString(R.string.cancel),
                        getString(R.string.okay), {
                    viewModel.removedAccountResult.observe(this@AccountManagerActivity, Observer(::closeActivity))
                    viewModel.removeAccount(username)
                }, false)
            }
        }

        accountsRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    @Synchronized
    private fun closeAndMoveToLogin(result: Boolean) {
        if (result && !movingToMailbox) {
            movingToMailbox = true
            Handler().postDelayed({
                moveToMailboxLogout()
            }, 500)
        }
    }

    @Subscribe
    fun onLogoutEvent(event: LogoutEvent) {
        closeAndMoveToLogin(true)
    }

    private fun closeActivity(result: Boolean) {
        if (result) {
            Handler().postDelayed({
                moveToMailbox()
                saveLastInteraction()
                finish()
            }, 500)
        }
    }

    override fun onStart() {
        super.onStart()
        ProtonMailApplication.getApplication().bus.register(this)
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onStop() {
        super.onStop()
        ProtonMailApplication.getApplication().bus.unregister(this)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.account_manager_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                saveLastInteraction()
                finish()
                true
            }
            R.id.action_remove_all -> {
                showToast(R.string.account_manager_remove_all_accounts)
                val accountManager = AccountManager.getInstance(this)
                viewModel.removedAllAccountsResult.observe(this@AccountManagerActivity, Observer(::closeAndMoveToLogin))
                viewModel.removeAllAccounts(accountManager.getLoggedInUsers())
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun loadData() {
        val currentPrimaryAccount = mUserManager.username
        if (!mUserManager.accessTokenExists() || mUserManager.getUser(currentPrimaryAccount).addresses.isEmpty()) {
            mUserManager.logoutOffline(currentPrimaryAccount)
        }
        val accountManager = AccountManager.getInstance(this)
        val accounts = accountManager.getLoggedInUsers().map {
            val userAddresses = mUserManager.getUser(it).addresses
            val primaryAddress = userAddresses.find { address ->
                address.type == Constants.ADDRESS_TYPE_PRIMARY
            }
            val displayName = primaryAddress?.displayName ?: ""
            val primaryAddressEmail = if (primaryAddress == null) {
                it
            } else {
                primaryAddress.email
            }
            DrawerUserModel.BaseUser.AccountUser(it, primaryAddressEmail, true, it == currentPrimaryAccount, if (displayName.isNotEmpty()) displayName else it)
        }.sortedByDescending {
            it.primary
        }.plus(accountManager.getSavedUsers().map {
            DrawerUserModel.BaseUser.AccountUser(name = it, loggedIn = false, emailAddress = "", primary = false, displayName = it)
        }) as MutableList<DrawerUserModel>
        accounts.add(DrawerUserModel.AccFooter)
        accountsAdapter.items = accounts
        accountsRecyclerView.adapter = accountsAdapter
    }

    override fun onBackPressed() {
        saveLastInteraction()
        super.onBackPressed()
    }
}
