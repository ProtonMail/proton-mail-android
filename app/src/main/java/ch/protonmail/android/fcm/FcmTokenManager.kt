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

package ch.protonmail.android.fcm

import android.content.SharedPreferences
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.core.Constants.Prefs.PREF_APP_VERSION
import ch.protonmail.android.core.Constants.Prefs.PREF_REGISTRATION_ID
import ch.protonmail.android.core.Constants.Prefs.PREF_SENT_TOKEN_TO_SERVER
import ch.protonmail.android.fcm.model.FirebaseToken
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber

/**
 * Local cache for Firebase token
 */
class FcmTokenManager @AssistedInject constructor(
    @Assisted private val userPreferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) {

    @AssistedInject.Factory
    interface Factory {
        fun create(userPreferences: SharedPreferences): FcmTokenManager
    }

    suspend fun getToken(): FirebaseToken? = withContext(dispatchers.Io) {
        val registrationId: FirebaseToken? = userPreferences[PREF_REGISTRATION_ID]
        if (registrationId == null) {
            Timber.v("Token not found")
            return@withContext null
        }

        // Check if app was updated; if so, it must clear the registration ID since the existing registration ID is not
        //  guaranteed to work with the new app version.
        val registeredVersion = userPreferences[PREF_APP_VERSION] ?: Int.MIN_VALUE
        val currentVersion = BuildConfig.VERSION_CODE
        if (registeredVersion != currentVersion) {
            Timber.v("App version changed")
            return@withContext null
        }
        return@withContext registrationId
    }

    @Deprecated("Use suspend variant", ReplaceWith("getToken()"))
    fun getTokenBlocking(): FirebaseToken? = runBlocking {
        getToken()
    }

    suspend fun saveToken(token: FirebaseToken) {
        withContext(dispatchers.Io) {
            val appVersion = BuildConfig.VERSION_CODE
            Timber.v("Saving version:%s registration ID: %s", appVersion, token)
            userPreferences[PREF_REGISTRATION_ID] = token
            userPreferences[PREF_APP_VERSION] = appVersion
        }
    }

    @Deprecated("Use suspend variant", ReplaceWith("saveToken(sent)"))
    fun saveTokenBlocking(token: FirebaseToken) {
        runBlocking { saveToken(token) }
    }

    suspend fun setTokenSent(sent: Boolean) {
        withContext(dispatchers.Io) {
            userPreferences[PREF_SENT_TOKEN_TO_SERVER] = sent
        }
    }

    @Deprecated("Use suspend variant", ReplaceWith("setTokenSent(sent)"))
    fun setTokenSentBlocking(sent: Boolean) {
        userPreferences[PREF_SENT_TOKEN_TO_SERVER] = sent
    }

    suspend fun isTokenSent(): Boolean = withContext(dispatchers.Io) {
        userPreferences[PREF_SENT_TOKEN_TO_SERVER] ?: false
    }

    @Deprecated("Use suspend variant", ReplaceWith("isTokenSent()"))
    fun isTokenSentBlocking(): Boolean =
        userPreferences[PREF_SENT_TOKEN_TO_SERVER] ?: false
}
