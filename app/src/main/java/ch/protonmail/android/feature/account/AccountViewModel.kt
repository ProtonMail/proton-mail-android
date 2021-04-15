/*
 * Copyright (c) 2020 Proton Technologies AG
 * This file is part of Proton Technologies AG and ProtonCore.
 *
 * ProtonCore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonCore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonCore.  If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.feature.account

import androidx.activity.ComponentActivity
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionHumanVerificationNeeded
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.onLoginResult
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserManager

class AccountViewModel @ViewModelInject constructor(
    private val accountManager: AccountManager,
    private var authOrchestrator: AuthOrchestrator,
    private val userManager: UserManager
) : ViewModel() {

    private val _state = MutableStateFlow(State.Processing as State)

    private suspend fun UserId?.getAccount() = this?.let { getAccount(it).firstOrNull() }

    private suspend fun prepareReadyAccounts(accounts: List<Account>) {
        accounts.forEach { account ->
            // Make sure we have Addresses before starting MailboxActivity.
            userManager.getAddresses(account.userId, refresh = userManager.getAddresses(account.userId).isEmpty())
        }
    }

    sealed class State {
        object Processing : State()
        object LoginNeeded : State()
        object LoginClosed : State()
        data class AccountList(val accounts: List<Account>) : State()
    }

    val state = _state.asStateFlow()

    fun register(context: ComponentActivity) {
        authOrchestrator.register(context)

        // Handle Account states.
        with(authOrchestrator) {
            onLoginResult { result -> if (result == null) _state.tryEmit(State.LoginClosed) }
            accountManager.observe(context.lifecycleScope)
                .onSessionSecondFactorNeeded { startSecondFactorWorkflow(it) }
                .onAccountTwoPassModeNeeded { startTwoPassModeWorkflow(it) }
                .onAccountCreateAddressNeeded { startChooseAddressWorkflow(it) }
                .onSessionHumanVerificationNeeded { startHumanVerificationWorkflow(it) }
                .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
                .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
                .onAccountDisabled {

                    /*
                    notifyLoggedOut.blocking(userId)
                    jobManager.stop()
                    jobManager.clear()
                    jobManager.cancelJobsInBackground(null, TagConstraint.ALL)
                    userManager.logoutOfflineBlocking(userId)
                    */


                    accountManager.removeAccount(it.userId)
                }
        }

        // Raise LoginNeeded on empty account list.
        accountManager.getAccounts().onEach { accounts ->
            when {
                accounts.isEmpty() -> {
                    _state.tryEmit(State.LoginNeeded)
                }
                accounts.any { it.isReady() } -> {
                    prepareReadyAccounts(accounts.filter { it.state == AccountState.Ready })
                    _state.tryEmit(State.AccountList(accounts))
                }
            }
        }.launchIn(viewModelScope)
    }

    fun getPrimaryUserId() = accountManager.getPrimaryUserId()

    fun getPrimaryAccount() = accountManager.getPrimaryAccount()

    fun getAccount(userId: UserId) = accountManager.getAccount(userId)

    fun getAccounts() = accountManager.getAccounts()

    /* Order: Primary account, ready account(s), other account(s). */
    fun getSortedAccounts() = accountManager.getAccounts().mapLatest { accounts ->
        val currentUser = getPrimaryUserId().firstOrNull()
        accounts
            .sortedByDescending { it.userId == currentUser }
            .sortedByDescending { it.isReady() }
    }

    fun login() = authOrchestrator.startLoginWorkflow(AccountType.Internal)

    fun loginOrSwitch(userId: UserId) = viewModelScope.launch {
        val primaryUserId = getPrimaryUserId().firstOrNull()
        val account = userId.getAccount()
        when {
            account == null -> Unit
            account.isDisabled() -> authOrchestrator.startLoginWorkflow(AccountType.Internal) // TODO: Add userId.
            account.userId != primaryUserId -> accountManager.setAsPrimary(account.userId)
        }
    }

    fun switch(userId: UserId) = viewModelScope.launch {
        accountManager.setAsPrimary(userId)
    }

    fun logout(userId: UserId) = viewModelScope.launch {
        accountManager.disableAccount(userId)
    }

    fun logoutPrimary() = viewModelScope.launch {
        getPrimaryUserId().first()?.let { logout(it) }
    }

    fun remove(userId: UserId) = viewModelScope.launch {
        accountManager.removeAccount(userId)
    }

    fun removeAll() = viewModelScope.launch {
        accountManager.getAccounts().first().forEach {
            accountManager.removeAccount(it.userId)
        }
    }

    data class AccountSwitch(val previous: Account? = null, val current: Account? = null)

    fun onAccountSwitched() = getPrimaryUserId().scan(AccountSwitch()) { previous: AccountSwitch, currentUserId ->
        AccountSwitch(
            previous = previous.current?.userId?.getAccount(),
            current = currentUserId.getAccount()
        )
    }.drop(2) // Initial from scan + initial from getPrimaryAccount.

    // region Deprecated

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("switch(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    fun switch(userId: Id) = switch(UserId(userId.s))

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("switch(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    fun switch(username: String) = viewModelScope.launch {
        getAccounts().first().firstOrNull { it.username == username }?.let { account ->
            switch(account.userId)
        }
    }

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("logout(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    fun logout(userId: Id) = logout(UserId(userId.s))

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("remove(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    fun remove(userId: Id) = remove(UserId(userId.s))

    // endregion
}
