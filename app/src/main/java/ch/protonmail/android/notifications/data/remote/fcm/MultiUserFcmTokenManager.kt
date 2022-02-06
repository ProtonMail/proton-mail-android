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

package ch.protonmail.android.notifications.data.remote.fcm

import ch.protonmail.android.feature.account.allLoggedIn
import ch.protonmail.android.feature.account.allSaved
import ch.protonmail.android.notifications.data.remote.fcm.model.FirebaseToken
import ch.protonmail.android.prefs.SecureSharedPreferences
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

class MultiUserFcmTokenManager @Inject constructor(
    private val accountManager: AccountManager,
    private val secureSharedPreferencesFactory: SecureSharedPreferences.Factory,
    private val userFcmTokenManagerFactory: FcmTokenManager.Factory
) {

    suspend fun isTokenSentForAllLoggedUsers(): Boolean {
        withEachLoggedUserTokenManager {
            if (isTokenSent().not())
                return false
        }
        return true
    }

    @Deprecated("Use suspend function", ReplaceWith("isTokenSentForAllLoggedUsers()"))
    fun isTokenSentForAllLoggedUsersBlocking(): Boolean =
        runBlocking { isTokenSentForAllLoggedUsers() }

    suspend fun saveToken(token: FirebaseToken) {
        withEachLoggedUserTokenManager {
            saveToken(token)
        }
    }

    @Deprecated("Use suspend function", ReplaceWith("saveToken(token)"))
    fun saveTokenBlocking(token: FirebaseToken) {
        runBlocking { saveToken(token) }
    }

    suspend fun setTokenUnsentForAllSavedUsers() {
        withEachSavedUserTokenManager {
            setTokenSent(false)
        }
    }

    @Deprecated("Use suspend function", ReplaceWith("setTokenUnsentForAllSavedUsers()"))
    fun setTokenUnsentForAllSavedUsersBlocking() {
        runBlocking { setTokenUnsentForAllSavedUsers() }
    }

    private suspend inline fun withEachLoggedUserTokenManager(block: FcmTokenManager.() -> Unit) {
        for (userId in accountManager.allLoggedIn()) {
            val userPrefs = secureSharedPreferencesFactory.userPreferences(userId)
            val userTokenManger = userFcmTokenManagerFactory.create(userPrefs)
            block(userTokenManger)
        }
    }

    private suspend inline fun withEachSavedUserTokenManager(block: FcmTokenManager.() -> Unit) {
        for (userId in accountManager.allSaved()) {
            val userPrefs = secureSharedPreferencesFactory.userPreferences(userId)
            val userTokenManger = userFcmTokenManagerFactory.create(userPrefs)
            block(userTokenManger)
        }
    }
}
