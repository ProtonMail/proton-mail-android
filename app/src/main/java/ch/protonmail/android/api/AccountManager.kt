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
import android.preference.PreferenceManager
import androidx.core.content.edit
import ch.protonmail.android.utils.getStringList
import ch.protonmail.android.utils.putStringList
import me.proton.core.util.android.sharedpreferences.minusAssign

// region constants
private const val PREF_USERNAMES_LOGGED_IN = "PREF_USERNAMES_LOGGED_IN"
private const val PREF_USERNAMES_LOGGED_OUT = "PREF_USERNAMES_LOGGED_OUT" // logged out but saved
// endregion

/**
 * AccountManager stores information about all local users currently logged in or logged out, but saved.
 */
class AccountManager private constructor(private val sharedPreferences: SharedPreferences) {

    fun onSuccessfulLogin(username: String) {
        if (username.isBlank()) return

        sharedPreferences.edit {
            putStringList(
                PREF_USERNAMES_LOGGED_IN,
                sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, emptyList())?.plus(username)?.distinct()
            )
            putStringList(
                PREF_USERNAMES_LOGGED_OUT,
                sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, emptyList())?.minus(username)
            )
        }
    }

    fun onSuccessfulLogout(username: String) {
        if (username.isBlank()) return

        sharedPreferences.edit {
            putStringList(
                PREF_USERNAMES_LOGGED_IN,
                sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, emptyList())?.minus(username)
            )
            putStringList(
                PREF_USERNAMES_LOGGED_OUT,
                sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, emptyList())?.plus(username)?.distinct()
            )
        }
    }

    /**
     * Removes from logged-out list.
     */
    fun removeFromSaved(username: String) {
        sharedPreferences.edit {
            putStringList(
                PREF_USERNAMES_LOGGED_OUT,
                sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, null)?.minus(username)
            )
        }
    }

    fun getLoggedInUsers(): List<String> =
        sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, null) ?: emptyList()

    fun getNextLoggedInAccountOtherThan(username: String, currentPrimary: String): String? {
        val otherLoggedInAccounts = sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, null)
            ?.minus(username)

        return otherLoggedInAccounts
            ?.find { it == currentPrimary }
            ?: otherLoggedInAccounts?.firstOrNull()
    }

    fun getSavedUsers(): List<String> =
        sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, null) ?: emptyList()

    /**
     * Removes all known lists of usernames.
     */
    fun clear() {
        sharedPreferences.apply {
            this -= PREF_USERNAMES_LOGGED_IN
            this -= PREF_USERNAMES_LOGGED_OUT
        }
    }

    companion object {

        fun getInstance(context: Context): AccountManager =
            AccountManager(PreferenceManager.getDefaultSharedPreferences(context))
    }
}
