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
package ch.protonmail.android.activities.multiuser.viewModel

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.usecase.FindUserIdForUsername
import kotlinx.coroutines.launch

class AccountManagerViewModel @ViewModelInject constructor(
    private val userManager: UserManager,
    private val accountManager: AccountManager,
    private val findUserIdForUsername: FindUserIdForUsername
) : ViewModel() {

    private val _removedAccountResult: MutableLiveData<Boolean> = MutableLiveData()
    private val _removedAllAccountsResult: MutableLiveData<Boolean> = MutableLiveData()
    private val _logoutAccountResult: MutableLiveData<Boolean> = MutableLiveData()

    val removedAccountResult: LiveData<Boolean>
        get() = _removedAccountResult
    val removedAllAccountsResult: LiveData<Boolean>
        get() = _removedAllAccountsResult
    val logoutAccountResult: LiveData<Boolean>
        get() = _logoutAccountResult

    fun logoutAccount(username: String) {
        userManager.logoutAccount(username) {
            // notify the activity
            _logoutAccountResult.value = true
        }
    }

    fun removeAccount(username: String, notify: Boolean = true) {
        viewModelScope.launch {
            if (accountManager.getLoggedInUsers().contains(username)) {
                userManager.removeAccount(username) {
                    if (notify) {
                        // notify the activity
                        _removedAccountResult.value = true
                    }
                }
            } else {
                accountManager.remove(findUserIdForUsername(Name(username)))
                if (notify) {
                    _removedAccountResult.value = true
                }
            }
        }
    }

    fun removeAllAccounts(listUsername: List<String>) {
        val currentActiveAccount = userManager.username
        listUsername.minus(currentActiveAccount).forEach {
            removeAccount(it, false)
        }
        userManager.logoutLastActiveAccount {
            _removedAllAccountsResult.value = true
        }
    }
}
