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

package ch.protonmail.android.viewmodel

import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.usecase.fetch.FetchPaymentMethods
import ch.protonmail.android.usecase.model.FetchPaymentMethodsResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.plan.presentation.PlansOrchestrator
import me.proton.core.plan.presentation.entity.UpgradeResult
import me.proton.core.plan.presentation.onUpgradeResult
import javax.inject.Inject

@HiltViewModel
class AccountTypeViewModel @Inject constructor(
    val fetchPaymentMethodsUseCase: FetchPaymentMethods,
    private val plansOrchestrator: PlansOrchestrator,
    private val accountManager: AccountManager
) : ViewModel() {

    private val fetchPaymentMethodsData = MutableLiveData<FetchPaymentMethodsResult>()
    private val upgradeState = MutableLiveData<State>()

    val upgradeStateResult: LiveData<State>
        get() = upgradeState

    val fetchPaymentMethodsResult: LiveData<FetchPaymentMethodsResult>
        get() = fetchPaymentMethodsData

    fun register(context: ComponentActivity) {
        plansOrchestrator.register(context)
    }

    fun fetchPaymentMethods() {
        viewModelScope.launch {
            val result = fetchPaymentMethodsUseCase()
            fetchPaymentMethodsData.value = result
        }
    }

    fun onUpgradeClicked() {
        viewModelScope.launch {
            accountManager.getPrimaryUserId().first()?.let {
                val account = accountManager.getAccount(it).first()
                if (account == null) {
                    upgradeState.value = State.Error.PrimaryUser
                    return@launch
                }
                with(plansOrchestrator) {
                    onUpgradeResult { upgradeResult ->
                        // do something with the upgrade result
                        if (upgradeResult != null) {
                            upgradeState.value = State.Success(upgradeResult)
                        }
                    }

                    plansOrchestrator.startUpgradeWorkflow(account.userId)
                }
            } ?: run {
                upgradeState.value = State.Error.PrimaryUser
            }
        }
    }

    sealed class State {
        data class Success(val result: UpgradeResult) : State()
        sealed class Error : State() {
            object PrimaryUser : Error()
            data class Message(val message: String?) : Error()
        }
    }
}
