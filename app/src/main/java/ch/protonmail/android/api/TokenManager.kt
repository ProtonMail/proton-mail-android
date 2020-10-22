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

import android.content.SharedPreferences
import android.text.TextUtils
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.models.RefreshResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication.getApplication

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

class TokenManager private constructor(val username: String) {

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

    private val pref: SharedPreferences by lazy {
        getApplication().getSecureSharedPreferences(username)
    }

    init {
        load()
    }

    val uid: String
        get() = uID ?: ""

    val authAccessToken: String?
        get() = if (TextUtils.isEmpty(accessToken)) null else {
            Constants.TOKEN_TYPE + " " + accessToken
        }

    private fun load() {
        refreshToken = pref.getString(PREF_REFRESH_TOKEN, "")
        uID = pref.getString(PREF_USER_UID, "")
        accessToken = pref.getString(PREF_ACCESS_TOKEN, null)
        val scope = pref.getString(PREF_TOKEN_SCOPE, Constants.TOKEN_SCOPE_FULL) ?: ""
        encPrivateKey = pref.getString(PREF_ENC_PRIV_KEY, "")
        this.scope = scope
    }

    private fun persist() {
        pref.edit()
            .putString(PREF_REFRESH_TOKEN, refreshToken)
            .putString(PREF_ENC_PRIV_KEY, encPrivateKey)
            .putString(PREF_USER_UID, uID)
            .putString(PREF_ACCESS_TOKEN, accessToken)
            .putString(PREF_TOKEN_SCOPE, scope)
            .apply()
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
        pref.edit().clear().apply()
        load()
    }

    fun createRefreshBody(): RefreshBody {
        return RefreshBody(refreshToken)
    }

    // TODO method only for debugging production issue
    fun isRefreshTokenBlank() = refreshToken.isNullOrBlank()

    // TODO method only for debugging production issue
    fun isUidBlank() = uid.isNullOrBlank()

    companion object {

        private val tokenManagers = mutableMapOf<String, TokenManager>()

        @Synchronized
        fun clearInstance(username: String) {
            if (username.isNotEmpty()) tokenManagers.remove(username)
        }

        @Synchronized
        fun clearAllInstances() {
            tokenManagers.clear()
        }

        fun removeEmptyTokenManagers() {
            tokenManagers.remove("")
        }

        /**
         * Creates and caches instances of TokenManager for usernames.
         */
        @Synchronized
        fun getInstance(username: String): TokenManager? {
            return if (!username.isBlank()) tokenManagers.getOrPut(username) {
                TokenManager(username)
            } else null
        }
    }
}
