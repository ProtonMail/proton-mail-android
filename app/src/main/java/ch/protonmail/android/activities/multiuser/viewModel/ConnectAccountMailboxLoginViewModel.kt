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
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ch.protonmail.android.core.UserManager

class ConnectAccountMailboxLoginViewModel(
        application: Application,
        private val userManager: UserManager
) : AndroidViewModel(application) {

    var username: String? = null
    var currentPrimary: String? = null

    class Factory(
            private val application: Application,
            private val userManager: UserManager
    ) : ViewModelProvider.NewInstanceFactory() {

        /** @return new instance of [ConnectAccountMailboxLoginViewModel] casted as T */
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                ConnectAccountMailboxLoginViewModel(application, userManager) as T
    }

    fun mailboxLogin(mailboxPassword: String, keySalt: String) {
        userManager.connectAccountMailboxLogin(username!!, currentPrimary!!, mailboxPassword, keySalt)
    }

    fun removeAccount(username: String) {
        userManager.removeAccount(username)
    }

    fun logoutAccount(username: String) {
        userManager.logoutAccount(username)
    }
}