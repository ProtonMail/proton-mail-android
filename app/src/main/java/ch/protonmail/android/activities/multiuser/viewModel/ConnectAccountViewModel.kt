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
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.core.UserManager

class ConnectAccountViewModel(
        application: Application,
        private val userManager: UserManager
) : AndroidViewModel(application) {

    var username: String? = null
    var currentPrimaryUsername: String = userManager.username

    class Factory(
            private val application: Application,
            private val userManager: UserManager
    ) : ViewModelProvider.NewInstanceFactory() {

        /** @return new instance of [ConnectAccountViewModel] casted as T */
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T =
                ConnectAccountViewModel(application, userManager) as T
    }

    fun info(username: String, password: ByteArray) {
        this.username = username
        userManager.info(username, password)
    }

    fun login(username: String, password: ByteArray, twoFactor: String? = null, response: LoginInfoResponse?, fallbackAuthVersion: Int) {
        userManager.connectAccountLogin(username, password, twoFactor, response, fallbackAuthVersion)
    }

    fun removeAccount(username: String) {
        userManager.removeAccount(username)
    }
}
