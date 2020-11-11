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
import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.unsupported

class AccountManagerViewModel @ViewModelInject constructor(
    private val userManager: UserManager,
    private val accountManager: AccountManager
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

    fun logout(userId: Id) {
        viewModelScope.launch {
            userManager.logout(userId)
            _logoutAccountResult.value = true
        }
    }

    @Deprecated("Use with user id", ReplaceWith("logout(userId)"), DeprecationLevel.ERROR)
    fun logoutAccount(username: String) {
        unsupported
    }

    fun remove(userId: Id, notify: Boolean = true) {
        viewModelScope.launch {

            if (userId in accountManager.allLoggedIn())
                userManager.logoutAndRemove(userId)
            else
                accountManager.remove(userId)

            if (notify) _removedAccountResult.value = true
        }
    }

    @Deprecated("Use with user id", ReplaceWith("remove(userId, notify)"), DeprecationLevel.ERROR)
    fun removeAccount(username: String, notify: Boolean = true) {
        unsupported
    }

    fun removeAll(usersIds: Collection<Id>) {
        viewModelScope.launch {
            val otherUsersIds = (usersIds - userManager.currentUserId).filterNotNull()
            for (userId in otherUsersIds) {
                remove(userId, notify = false)
            }

            userManager.logoutLastActiveAccount {
                _removedAllAccountsResult.value = true
            }
        }
    }

    @Deprecated("Use with user id", ReplaceWith("removeAll(usersIds)"), DeprecationLevel.ERROR)
    fun removeAllAccounts(listUsername: List<String>) {
        unsupported
    }
}
