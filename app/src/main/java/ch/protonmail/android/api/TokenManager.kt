/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.api

import android.content.Context
import android.content.SharedPreferences
import ch.protonmail.android.core.Constants
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.prefs.SecureSharedPreferences
import me.proton.core.util.android.sharedpreferences.clearOnly
import me.proton.core.util.android.sharedpreferences.get

// region constants
private const val PREF_ENC_PRIV_KEY = "priv_key"
private const val PREF_REFRESH_TOKEN = "refresh_token"
private const val PREF_SESSION_ID = "user_uid"
private const val PREF_ACCESS_TOKEN = "access_token_plain"
private const val PREF_TOKEN_SCOPE = "access_token_scope"
// endregion

@Deprecated("Replaced by SessionProvider. Now fully handled by Core AccountManager.")
class TokenManager private constructor(private val pref: SharedPreferences) {

    internal var accessToken: String? = pref[PREF_ACCESS_TOKEN]
    internal var refreshToken: String? = pref[PREF_REFRESH_TOKEN]
    internal var sessionId: String? = pref[PREF_SESSION_ID]
    internal var scope: String = pref[PREF_TOKEN_SCOPE] ?: Constants.TOKEN_SCOPE_FULL

    fun clear() {
        pref.clearOnly(
            PREF_REFRESH_TOKEN,
            PREF_SESSION_ID,
            PREF_ACCESS_TOKEN,
            PREF_TOKEN_SCOPE,
            PREF_ENC_PRIV_KEY
        )
    }

    companion object {

        @Deprecated("Replaced by SessionProvider. Now fully handled by Core AccountManager.")
        fun getInstance(context: Context, userId: UserId): TokenManager =
            TokenManager(SecureSharedPreferences.getPrefsForUser(context, userId))
    }
}
