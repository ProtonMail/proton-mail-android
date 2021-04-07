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
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.activities.multiuser.viewModel.AccountManagerViewModel
import ch.protonmail.android.adapters.AccountsAdapter
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.uiModel.DrawerUserModel
import ch.protonmail.android.utils.extensions.app
import ch.protonmail.android.utils.extensions.setBarColors
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.moveToMailbox
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_account_manager.*
import kotlinx.android.synthetic.main.toolbar_white.*
import javax.inject.Inject

@AndroidEntryPoint
class AccountManagerActivity : BaseActivity() {

    private val viewModel by viewModels<AccountManagerViewModel>()

    /**
     * [AccountsAdapter] for the Accounts RecyclerView. It is used to the default [accountsRecyclerView]
     * to display the users (logged in and recently logged out) of the application.
     */
    private val accountsAdapter by lazy { AccountsAdapter() }
    override fun getLayoutId() = R.layout.activity_account_manager

    @Inject
    lateinit var accountManager: AccountManager
    @Inject
    lateinit var userManager: UserManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar.setNavigationIcon(R.drawable.ic_close)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        window?.setBarColors(getColor(R.color.new_purple_dark))

        accountsAdapter.apply {

            onLogoutAccount = { userId ->

                lifecycleScope.launchWhenCreated {
                    val nextLoggedInUserId = userManager.getNextLoggedInUser()
                    val (title, message) = if (nextLoggedInUserId != null) {
                        val next = userManager.getUser(nextLoggedInUserId)
                        getString(R.string.logout) to getString(R.string.logout_question_next_account, next.name.s)
                    } else {
                        val current = checkNotNull(userManager.getCurrentUser())
                        getString(R.string.log_out, current.name.s) to getString(R.string.logout_question)
                    }

                    showTwoButtonInfoDialog(title = title, message = message) {
                        viewModel.logoutAccountResult.observe(this@AccountManagerActivity, ::closeActivity)
                        viewModel.logout(userId)
                    }
                }
            }

            onRemoveAccount = { userId ->

                lifecycleScope.launchWhenCreated {
                    val username = checkNotNull(userManager.getUser(userId)).name.s
                    showTwoButtonInfoDialog(
                        titleStringId = R.string.logout,
                        message = getString(R.string.remove_account_question, username)
                    ) {
                        viewModel.removedAccountResult.observe(this@AccountManagerActivity, ::closeActivity)
                        viewModel.remove(userId)
                    }
                }
            }

        }
        accountsRecyclerView.layoutManager = LinearLayoutManager(this)
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
        app.bus.register(this)
        loadData()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    override fun onStop() {
        super.onStop()
        app.bus.unregister(this)
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
                viewModel.removeAllLoggedIn()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    private fun loadData() {
        lifecycleScope.launchWhenCreated {
            val currentUser = userManager.currentUserId
            val allUsers = accountManager.allLoggedIn().map { it to true } +
                accountManager.allLoggedOut().map { it to false }

            val accounts = allUsers
                // Current user as first position, then logged in first
                .sortedByDescending { it.first == currentUser && it.second }
                .map { (id, loggedIn) ->
                    val user = userManager.getUser(id)
                    user.toUiModel(loggedIn, id == currentUser)
                }

            accountsAdapter.items = accounts + DrawerUserModel.AccFooter
            accountsRecyclerView.adapter = accountsAdapter
        }
    }

    private fun User.toUiModel(loggedIn: Boolean, currentPrimary: Boolean): DrawerUserModel.BaseUser.AccountUser {
        val primaryAddress = addresses.primary
        val username = name.s
        val displayName = primaryAddress?.displayName?.s
        return DrawerUserModel.BaseUser.AccountUser(
            id = id,
            name = displayName ?: username,
            emailAddress = primaryAddress?.email?.s ?: username,
            loggedIn = loggedIn,
            primary = currentPrimary,
            displayName = displayName ?: username
        )
    }

    override fun onBackPressed() {
        saveLastInteraction()
        super.onBackPressed()
    }
}
