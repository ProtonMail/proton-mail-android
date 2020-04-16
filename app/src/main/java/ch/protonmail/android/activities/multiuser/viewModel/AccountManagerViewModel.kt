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

import android.app.Application
import androidx.lifecycle.*
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.core.UserManager

class AccountManagerViewModel(
        application: Application,
        private val userManager: UserManager,
        private val accountManager: AccountManager
) : AndroidViewModel(application) {

    private val _removedAccountResult: MutableLiveData<Boolean> = MutableLiveData()
    private val _removedAllAccountsResult: MutableLiveData<Boolean> = MutableLiveData()
    private val _logoutAccountResult: MutableLiveData<Boolean> = MutableLiveData()

    val removedAccountResult: LiveData<Boolean>
        get() = _removedAccountResult
    val removedAllAccountsResult: LiveData<Boolean>
        get() = _removedAllAccountsResult
    val logoutAccountResult: LiveData<Boolean>
        get() = _logoutAccountResult

    class Factory(
            private val application: Application,
            private val userManager: UserManager,
            private val accountManager: AccountManager
    ) : ViewModelProvider.NewInstanceFactory() {

        /** @return new instance of [ConnectAccountMailboxLoginViewModel] casted as T */
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                AccountManagerViewModel(application, userManager, accountManager) as T
    }

    fun logoutAccount(username: String) {
        userManager.logoutAccount(username) {
            // notify the activity
            _logoutAccountResult.value = true
        }
    }

    fun removeAccount(username: String, notify: Boolean = true) {
        if (accountManager.getLoggedInUsers().contains(username)) {
            userManager.removeAccount(username) {
                if (notify) {
                    // notify the activity
                    _removedAccountResult.value = true
                }
            }
        } else {
            removeLoggedOutAccount(username)
            if (notify) {
                _removedAccountResult.value = true
            }
        }
    }

    private fun removeLoggedOutAccount(username: String) {
        accountManager.removeFromSaved(username)
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