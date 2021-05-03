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
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.BaseActivity
import ch.protonmail.android.adapters.AccountsAdapter
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.uiModel.DrawerUserModel
import ch.protonmail.android.utils.extensions.showToast
import ch.protonmail.android.utils.ui.dialogs.DialogUtils.Companion.showTwoButtonInfoDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_account_manager.*
import kotlinx.android.synthetic.main.toolbar_white.*
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.isReady
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

@AndroidEntryPoint
class AccountManagerActivity : BaseActivity() {

    private val accountsAdapter by lazy { AccountsAdapter() }

    @Inject
    lateinit var userManager: UserManager

    override fun getLayoutId() = R.layout.activity_account_manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        toolbar.setNavigationIcon(R.drawable.ic_close)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        accountsAdapter.apply {
            onLoginAccount = { userId -> accountStateManager.login() }
            onLogoutAccount = { userId -> onLogoutClicked(userId) }
            onRemoveAccount = { userId -> onRemoveClicked(userId) }
        }
        accountsRecyclerView.layoutManager = LinearLayoutManager(this)
        accountsRecyclerView.adapter = accountsAdapter

        accountStateManager.getSortedAccounts()
            .combine(accountStateManager.getPrimaryUserId()) { accounts, primaryUserId -> accounts to primaryUserId }
            .flowWithLifecycle(lifecycle, Lifecycle.State.CREATED)
            .onEach { (sortedAccounts, primaryUserId) ->
                val accounts = sortedAccounts.map { account ->
                    val id = Id(account.userId.id)
                    val user = userManager.getLegacyUserOrNull(id)
                    account.toUiModel(account.isReady(), account.userId == primaryUserId, user)
                }
                accountsAdapter.items = accounts + DrawerUserModel.AccFooter
            }.launchIn(lifecycleScope)
    }

    private fun onRemoveClicked(userId: UserId) {
        lifecycleScope.launchWhenCreated {
            val account = checkNotNull(accountStateManager.getAccountOrNull(userId))
            val username = account.username
            showTwoButtonInfoDialog(
                titleStringId = R.string.logout,
                message = getString(R.string.remove_account_question, username)
            ) {
                accountStateManager.remove(userId)
            }
        }
    }

    private fun onLogoutClicked(userId: UserId) {
        lifecycleScope.launchWhenCreated {
            val nextLoggedInUserId = userManager.getPreviousCurrentUserId()
            val (title, message) = if (nextLoggedInUserId != null) {
                val next = userManager.getUser(nextLoggedInUserId)
                getString(R.string.logout) to getString(R.string.logout_question_next_account, next.name.s)
            } else {
                val current = checkNotNull(userManager.currentUser)
                getString(R.string.log_out, current.name.s) to getString(R.string.logout_question)
            }

            showTwoButtonInfoDialog(title = title, message = message) {
                accountStateManager.logout(userId)
            }
        }
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
                accountStateManager.removeAll()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onBackPressed() {
        saveLastInteraction()
        super.onBackPressed()
    }

    private fun Account.toUiModel(
        loggedIn: Boolean,
        currentPrimary: Boolean,
        user: User?
    ) = DrawerUserModel.BaseUser.AccountUser(
        id = userId,
        name = username,
        emailAddress = email ?: user?.defaultAddressEmail ?: username,
        loggedIn = loggedIn,
        primary = currentPrimary,
        displayName = user?.displayName ?: username
    )
}
