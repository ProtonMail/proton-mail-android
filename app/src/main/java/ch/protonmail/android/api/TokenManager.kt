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
package ch.protonmail.android.api

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.models.RefreshResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.libs.core.utils.takeIfNotEmpty
import me.proton.core.util.android.sharedpreferences.clearAll
import me.proton.core.util.android.sharedpreferences.get

// region constants
private const val PREF_ENC_PRIV_KEY = "priv_key"
private const val PREF_REFRESH_TOKEN = "refresh_token"
private const val PREF_USER_UID = "user_uid"
private const val PREF_ACCESS_TOKEN = "access_token_plain"
private const val PREF_TOKEN_SCOPE = "access_token_scope"
// endregion

/**
 * TokenManager stores separate credentials for every user in
 * [SecureSharedPreferences][ch.protonmail.android.prefs.SecureSharedPreferences] file.
 */

class TokenManager private constructor(private val pref: SharedPreferences) {

    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var uID: String? = null
    var encPrivateKey: String? = null
        set(value) {
            field = value
            persist()
        }
    var scope: String = Constants.TOKEN_SCOPE_FULL
        set(value) {
            field = value
            persist()
        }

    init {
        load()
    }

    val uid: String
        get() = uID ?: ""

    val authAccessToken: String?
        get() = accessToken?.takeIfNotEmpty()?.let { "${Constants.TOKEN_TYPE} $it" }

    private fun load() {
        refreshToken = pref[PREF_REFRESH_TOKEN] ?: ""
        uID = pref[PREF_USER_UID] ?: ""
        accessToken = pref[PREF_ACCESS_TOKEN]
        scope = pref[PREF_TOKEN_SCOPE] ?: Constants.TOKEN_SCOPE_FULL
        encPrivateKey = pref[PREF_ENC_PRIV_KEY] ?: ""
    }

    private fun persist() {
        pref.edit {
            putString(PREF_REFRESH_TOKEN, refreshToken)
            putString(PREF_ENC_PRIV_KEY, encPrivateKey)
            putString(PREF_USER_UID, uID)
            putString(PREF_ACCESS_TOKEN, accessToken)
            putString(PREF_TOKEN_SCOPE, scope)
        }
    }

    fun handleRefresh(response: RefreshResponse) {
        if (response.refreshToken != null) {
            refreshToken = response.refreshToken
        }
        if (!response.privateKey.isNullOrEmpty()) {
            encPrivateKey = response.privateKey
        }
        accessToken = response.accessToken
        scope = response.scope
        persist()
    }

    fun handleLogin(response: LoginResponse) {
        accessToken = response.accessToken
        scope = response.scope
        refreshToken = response.refreshToken
        uID = response.uid
        encPrivateKey = response.privateKey
        persist()
    }

    fun clearAccessToken() {
        accessToken = null
        scope = ""
    }

    fun clear() {
        pref.clearAll()
        load()
    }

    fun createRefreshBody(): RefreshBody =
        RefreshBody(refreshToken)

    // TODO method only for debugging production issue
    fun isRefreshTokenBlank() = refreshToken.isNullOrBlank()

    // TODO method only for debugging production issue
    fun isUidBlank() = uid.isNullOrBlank()

    companion object {

        private val cache = mutableMapOf<Id, TokenManager>()

        @Synchronized
        fun clearInstance(userId: Id) {
            cache -= userId
        }

        @Synchronized
        fun clearAllInstances() {
            cache.clear()
        }

        /**
         * Creates and caches instances of TokenManager for given User
         */
        @Synchronized
        fun getInstance(context: Context, userId: Id): TokenManager =
            cache.getOrPut(userId) {
                TokenManager(SecureSharedPreferences.getPrefsForUser(context, userId))
            }
    }
}
