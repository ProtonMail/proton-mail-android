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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountType
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

class AccountViewModel @ViewModelInject constructor(
    private val accountManager: AccountManager,
    private var authOrchestrator: AuthOrchestrator
) : ViewModel() {

    private val _state = MutableStateFlow(State.Processing as State)

    sealed class State {
        object Processing : State()
        object LoginNeeded : State()
        data class AccountList(val accounts: List<Account>) : State()
    }

    val state = _state.asStateFlow()

    fun register(context: ComponentActivity) {
        authOrchestrator.register(context)

        // Handle Account states.
        with(authOrchestrator) {
            accountManager.observe(context.lifecycleScope)
                .onSessionSecondFactorNeeded { startSecondFactorWorkflow(it) }
                .onAccountTwoPassModeNeeded { startTwoPassModeWorkflow(it) }
                .onAccountCreateAddressNeeded { startChooseAddressWorkflow(it) }
                .onSessionHumanVerificationNeeded { startHumanVerificationWorkflow(it) }
                .onAccountTwoPassModeFailed { accountManager.disableAccount(it.userId) }
                .onAccountCreateAddressFailed { accountManager.disableAccount(it.userId) }
                .onAccountDisabled { accountManager.removeAccount(it.userId) }
        }

        // Raise LoginNeeded on empty account list.
        accountManager.getAccounts().onEach { accounts ->
            if (accounts.isEmpty()) _state.tryEmit(State.LoginNeeded)
            _state.tryEmit(State.AccountList(accounts))
        }.launchIn(viewModelScope)
    }

    fun getPrimaryUserId() = accountManager.getPrimaryUserId()

    fun getPrimaryAccount() = accountManager.getPrimaryAccount()

    fun startLoginWorkflow() = authOrchestrator.startLoginWorkflow(AccountType.Internal)
}
