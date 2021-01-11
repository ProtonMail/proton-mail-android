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
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.getStringList
import ch.protonmail.android.utils.putStringList
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.takeIfNotEmpty
import javax.inject.Inject

// region constants
// TODO make private after the migration to use Ids instead of usernames, so tests can rely of AccountManager
//  implementation directly
const val PREF_ALL_SAVED = "Pref.saved.ids"
const val PREF_ALL_LOGGED_IN = "Pref.loggedIn.ids"
const val PREF_USERNAMES_LOGGED_IN = "PREF_USERNAMES_LOGGED_IN"
const val PREF_USERNAMES_LOGGED_OUT = "PREF_USERNAMES_LOGGED_OUT" // logged out but saved
// endregion

/**
 * AccountManager stores information about all local users currently logged in or logged out, but saved.
 */
class AccountManager private constructor(
    @DefaultSharedPreferences
    private val sharedPreferences: SharedPreferences
) {

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
        sharedPreferences.edit {
            // New
            remove(PREF_ALL_LOGGED_IN)
            remove(PREF_ALL_SAVED)
        }
    }

    /**
     * Migrate [AccountManager] to use Users' [Id] instead of username
     */
    class UsernameToIdMigration @Inject constructor(
        private val dispatchers: DispatcherProvider,
        private val accountManager: AccountManager,
        private val secureSharedPreferencesMigration: SecureSharedPreferences.UsernameToIdMigration,
        @DefaultSharedPreferences
        private val defaultSharedPreferences: SharedPreferences
    ) {

        suspend operator fun invoke() = withContext(dispatchers.Io) {
            // Read directly from SharedPreferences as no function for get usernames will be available from
            //  AccountManager after the migration
            val allSavedUsernames = defaultSharedPreferences
                .getStringList(PREF_USERNAMES_LOGGED_OUT, null)
                ?.takeIfNotEmpty() ?: return@withContext
            val allLoggedUsernames = defaultSharedPreferences
                .getStringList(PREF_USERNAMES_LOGGED_IN, emptyList())!!

            val allUsernamesToIds = secureSharedPreferencesMigration(allSavedUsernames + allLoggedUsernames)

            // TODO: use AccountManager.set after the migration to use Ids instead of usernames
            defaultSharedPreferences.edit {

                putStringSet(
                    PREF_ALL_SAVED,
                    allSavedUsernames
                        // SecureSharedPreferences.UsernameToIdMigration has already printed to log if some id is not
                        //   found, so we just ignore it here
                        .mapNotNull { allUsernamesToIds[it]?.s }
                        .toMutableSet()
                )
                remove(PREF_USERNAMES_LOGGED_OUT)

                putStringSet(
                    PREF_ALL_LOGGED_IN,
                    allLoggedUsernames
                        // SecureSharedPreferences.UsernameToIdMigration has already printed to log if some id is not
                        //   found, so we just ignore it here
                        .mapNotNull { allUsernamesToIds[it]?.s }
                        .toMutableSet()
                )
                remove(PREF_USERNAMES_LOGGED_IN)
            }
        }

    }

    companion object {

        @Deprecated("Inject the constructor directly")
        fun getInstance(context: Context): AccountManager =
            AccountManager(PreferenceManager.getDefaultSharedPreferences(context))
    }
}
