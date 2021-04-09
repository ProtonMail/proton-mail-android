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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.api.segments.event.EventManager
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.usecase.delete.ClearUserData
import ch.protonmail.android.usecase.fetch.LaunchInitialDataFetch
import ch.protonmail.android.utils.AppUtil
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.launch
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountType
import me.proton.core.account.domain.entity.isDisabled
import me.proton.core.account.domain.entity.isReady
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.accountmanager.presentation.disableInitialNotReadyAccounts
import me.proton.core.accountmanager.presentation.observe
import me.proton.core.accountmanager.presentation.onAccountCreateAddressFailed
import me.proton.core.accountmanager.presentation.onAccountCreateAddressNeeded
import me.proton.core.accountmanager.presentation.onAccountDisabled
import me.proton.core.accountmanager.presentation.onAccountReady
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeFailed
import me.proton.core.accountmanager.presentation.onAccountTwoPassModeNeeded
import me.proton.core.accountmanager.presentation.onSessionHumanVerificationNeeded
import me.proton.core.accountmanager.presentation.onSessionSecondFactorNeeded
import me.proton.core.auth.presentation.AuthOrchestrator
import me.proton.core.auth.presentation.onLoginResult
import me.proton.core.domain.entity.UserId

class AccountViewModel @ViewModelInject constructor(
    private val accountManager: AccountManager,
    private var authOrchestrator: AuthOrchestrator,
    private val eventManager: EventManager,
    private val jobManager: JobManager,
    private val oldUserManager: ch.protonmail.android.core.UserManager,
    private val launchInitialDataFetch: LaunchInitialDataFetch,
    private val clearUserData: ClearUserData
) : ViewModel() {

    private val _state = MutableStateFlow(State.Processing as State)

    private suspend fun UserId?.getAccount() = this?.let { getAccount(it).firstOrNull() }

    sealed class State {
        object Processing : State()
        object LoginClosed : State()
        object AccountNeeded : State()
        data class AccountList(val accounts: List<Account>) : State()
    }

    val state = _state.asStateFlow()

    fun register(context: ComponentActivity) {
        authOrchestrator.register(context)

        // Handle Account states.
        with(authOrchestrator) {
            onLoginResult { result -> if (result == null) _state.tryEmit(State.LoginClosed) }
            accountManager.observe(context.lifecycle, minActiveState = Lifecycle.State.CREATED)
                .onSessionSecondFactorNeeded { startSecondFactorWorkflow(it) }
                .onAccountTwoPassModeNeeded { startTwoPassModeWorkflow(it) }
                .onAccountCreateAddressNeeded { startChooseAddressWorkflow(it) }
                .onSessionHumanVerificationNeeded { startHumanVerificationWorkflow(it) }
                .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
                .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
                .onAccountDisabled { onAccountDisabled(it) }
                .onAccountReady { onAccountReady(it) }
                .disableInitialNotReadyAccounts()
        }

        // Raise LoginNeeded on empty account list.
        accountManager.getAccounts().onEach { accounts ->
            when {
                accounts.isEmpty() || accounts.all { it.isDisabled() } -> {
                    onAccountNeeded()
                    _state.tryEmit(State.AccountNeeded)
                }
                accounts.any { it.isReady() } -> {
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

    fun logout(userId: UserId) = viewModelScope.launch {
        accountManager.disableAccount(userId)
    }

    fun logoutPrimary() = viewModelScope.launch {
        getPrimaryUserId().first()?.let { logout(it) }
    }

    fun login() {
        authOrchestrator.startLoginWorkflow(AccountType.Internal)
    }

    fun switch(userId: UserId) = viewModelScope.launch {
        val account = userId.getAccount() ?: return@launch
        when {
            account.isDisabled() -> authOrchestrator.startLoginWorkflow(AccountType.Internal) // TODO: Add userId.
            account.isReady() -> accountManager.setAsPrimary(userId)
        }
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

    fun onAccountSwitched() = getPrimaryUserId().scan(AccountSwitch()) { previous, currentUserId ->
        AccountSwitch(
            previous = previous.current?.userId?.getAccount(),
            current = currentUserId.getAccount()
        )
    }.filter { it.previous != null && it.current != it.previous }

    // region Deprecated

    @Deprecated(
        message = "Use UserId version of the function",
        replaceWith = ReplaceWith("switch(UserId(userId.s))", "me.proton.core.domain.entity.UserId")
    )
    fun switch(userId: Id) = switch(UserId(userId.s))

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

    // region Legacy User Management & Cleaning.

    private suspend fun onAccountNeeded() {
        AppUtil.clearTasks(jobManager)
        AppUtil.deletePrefs()
        AppUtil.deleteBackupPrefs()
    }

    private suspend fun onAccountReady(account: Account) {
        // Update account data (TODO: add cache).
        launchInitialDataFetch.invoke(
            userId = Id(account.userId.id),
            shouldRefreshDetails = true,
            shouldRefreshContacts = true
        )
        AlarmReceiver().setAlarm(ProtonMailApplication.getApplication())
        jobManager.start()
    }

    private suspend fun onAccountDisabled(account: Account) {
        val userId = Id(account.userId.id)
        clearUserData.invoke(userId)
        eventManager.clearState(userId)
        AppUtil.deleteSecurePrefs(oldUserManager.preferencesFor(userId), false)
    }

    // endregion
}
