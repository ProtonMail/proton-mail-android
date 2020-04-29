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

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.EXTRA_SWITCHED_USER
import ch.protonmail.android.activities.REQUEST_CODE_SWITCHED_USER
import ch.protonmail.android.activities.multiuser.viewModel.AccountManagerViewModel
import ch.protonmail.android.adapters.AccountManagerAccountsAdapter
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.uiModel.AccountManagerUserModel
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.setBarColors
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToMailbox
import ch.protonmail.android.utils.moveToMailboxLogout
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import ch.protonmail.libs.core.utils.EMPTY_STRING
import ch.protonmail.libs.core.utils.takeIfNotBlank
import com.squareup.otto.Subscribe
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_account_manager.*
import kotlinx.android.synthetic.main.toolbar_white.*
import javax.inject.Inject

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
     * [AccountManagerAccountsAdapter] for the Accounts RecyclerView. It is used to the default [accountsRecyclerView]
     * to display the users (logged in and recently logged out) of the application.
     */
    private val accountsAdapter = AccountManagerAccountsAdapter(
        onItemClick = {
            if (it is AccountManagerUserModel.AddAccount) {
                val intent = Intent(this, ConnectAccountActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra(EXTRA_SWITCHED_USER, true)
                }
                ActivityCompat.startActivityForResult(
                    this,
                    AppUtil.decorInAppIntent(intent),
                    REQUEST_CODE_SWITCHED_USER,
                    null
                )
                finish()
            }
        },
        onLogin = {
            val username = it.name

            val intent = Intent(this, ConnectAccountActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra(EXTRA_USERNAME, username)
            }
            ContextCompat.startActivity(this, AppUtil.decorInAppIntent(intent), null)
        },
        onLogout = {
            val username = it.name
            val nextLoggedInAccount = mUserManager.nextLoggedInAccountOtherThanCurrent
            val isCurrentUserAndHasAnotherUser =
                nextLoggedInAccount != null && username == mUserManager.username

            val title =
                if (isCurrentUserAndHasAnotherUser) getString(R.string.logout)
                else String.format(getString(R.string.log_out), username)
            val message = if (isCurrentUserAndHasAnotherUser) String.format(
                getString(R.string.logout_question_next_account),
                nextLoggedInAccount
            ) else getString(R.string.logout_question)

            DialogUtils.showInfoDialogWithTwoButtons(
                this,
                title,
                message,
                getString(R.string.cancel),
                getString(R.string.okay),
                {
                    viewModel.logoutAccountResult.observe(this, Observer(::closeActivity))
                    viewModel.logoutAccount(username)
                },
                false
            )
        },
        onRemove = {
            val username = it.name

            val message = String.format(getString(R.string.remove_account_question), username)
            DialogUtils.showInfoDialogWithTwoButtons(
                this,
                getString(R.string.logout),
                message,
                getString(R.string.cancel),
                getString(R.string.okay),
                {
                    viewModel.removedAccountResult.observe(this, Observer(::closeActivity))
                    viewModel.removeAccount(username)
                },
                false
            )
        }
    )

    override fun getLayoutId() = R.layout.activity_account_manager

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        toolbar.setNavigationIcon(R.drawable.ic_close)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        window?.setBarColors(getColor(R.color.new_purple_dark))

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
        mApp.bus.register(this)
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onStop() {
        super.onStop()
        mApp.bus.unregister(this)
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

        val accounts = accountManager.getLoggedInUsers()
            .map { username ->
                val userAddresses = mUserManager.getUser(username).addresses
                val primaryAddress = userAddresses.find { address ->
                    address.type == Constants.ADDRESS_TYPE_PRIMARY
                }
                val displayName = primaryAddress?.displayName ?: EMPTY_STRING
                val primaryAddressEmail =
                    if (primaryAddress == null) username else primaryAddress.email

                AccountManagerUserModel.User(
                    name = username,
                    emailAddress = primaryAddressEmail,
                    loggedIn = true,
                    primary = username == currentPrimaryAccount,
                    displayName = displayName.takeIfNotBlank() ?: username
                )
            }
            .sortedByDescending { it.primary } +
            accountManager.getSavedUsers().map { AccountManagerUserModel.User(name = it) } +
            AccountManagerUserModel.AddAccount

        accountsAdapter.items = accounts
        accountsRecyclerView.adapter = accountsAdapter
    }

    override fun onBackPressed() {
        saveLastInteraction()
        super.onBackPressed()
    }
}
