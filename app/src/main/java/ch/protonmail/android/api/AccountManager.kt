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
import ch.protonmail.android.utils.getStringList
import ch.protonmail.android.utils.putStringList

// region constants
private const val PREF_USERNAMES_LOGGED_IN = "PREF_USERNAMES_LOGGED_IN"
private const val PREF_USERNAMES_LOGGED_OUT = "PREF_USERNAMES_LOGGED_OUT" // logged out but saved
// endregion

/**
 * AccountManager stores information about all local users currently logged in or logged out, but saved.
 */

class AccountManager private constructor(private val sharedPreferences: SharedPreferences) {

    fun onSuccessfulLogin(username: String) {
        if (username.isBlank()) {
            return
        }
        sharedPreferences.edit()
                .putStringList(PREF_USERNAMES_LOGGED_IN, sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, emptyList())
                        ?.plus(username)?.distinct()).apply()
        sharedPreferences.edit()
                .putStringList(PREF_USERNAMES_LOGGED_OUT, sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, emptyList())
                        ?.minus(username)).apply()
    }

    fun onSuccessfulLogout(username: String) {
        if (username.isBlank()) {
            return
        }
        sharedPreferences.edit()
                .putStringList(PREF_USERNAMES_LOGGED_IN, sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, emptyList())
                        ?.minus(username)).apply()
        sharedPreferences.edit()
                .putStringList(PREF_USERNAMES_LOGGED_OUT, sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, emptyList())
                        ?.plus(username)?.distinct()).apply()
    }

    /**
     * Removes from logged-out list.
     */
    fun removeFromSaved(username: String) {
        sharedPreferences.edit()
                .putStringList(PREF_USERNAMES_LOGGED_OUT, sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, null)
                        ?.minus(username)).apply()
    }

    fun getLoggedInUsers(): List<String> {
        return sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, null) ?: emptyList()
    }

    fun getNextLoggedInAccountOtherThan(username: String, currentPrimary: String) : String? {
        val otherLoggedInAccounts = sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, null)?.minus(username) ?: emptyList()
        if (otherLoggedInAccounts.isNotEmpty()) {
            return otherLoggedInAccounts.find { it == currentPrimary } ?: otherLoggedInAccounts[0]
        }
        return null
    }

    fun getSavedUsers(): List<String> {
        return sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, null) ?: emptyList()
    }

    /**
     * Removes all known lists of usernames.
     */
    fun clear() {
        sharedPreferences.edit().remove(PREF_USERNAMES_LOGGED_IN).remove(PREF_USERNAMES_LOGGED_OUT).apply()
    }

    companion object {

        fun getInstance(context: Context): AccountManager {
            return AccountManager(PreferenceManager.getDefaultSharedPreferences(context))
        }
    }
}
