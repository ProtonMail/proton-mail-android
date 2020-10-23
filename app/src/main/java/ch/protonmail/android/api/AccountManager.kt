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
import ch.protonmail.android.di.ApplicationModule
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.getStringList
import kotlinx.coroutines.withContext
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.takeIfNotEmpty
import me.proton.core.util.kotlin.unsupported
import javax.inject.Inject

// region constants
private const val PREF_ALL_SAVED = "Pref.saved.ids"
private const val PREF_ALL_LOGGED_IN = "Pref.loggedIn.ids"
const val PREF_USERNAMES_LOGGED_IN = "PREF_USERNAMES_LOGGED_IN"
const val PREF_USERNAMES_LOGGED_OUT = "PREF_USERNAMES_LOGGED_OUT" // logged out but saved
// endregion

/**
 * AccountManager stores information about all local users currently logged in or logged out, but saved.
 */
class AccountManager(
    @DefaultSharedPreferences
    private val sharedPreferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) {

    suspend fun setLoggedIn(userId: Id) {
        setLoggedIn(setOf(userId))
    }

    suspend fun setLoggedIn(userIds: Collection<Id>) = withContext(dispatchers.Io) {
        sharedPreferences[PREF_ALL_LOGGED_IN] =
            sharedPreferences.get(PREF_ALL_LOGGED_IN, emptySet<String>()) + userIds.map { it.s }

        setSaved(userIds)
    }

    suspend fun setLoggedOut(userId: Id) {
        setLoggedOut(setOf(userId))
    }

    suspend fun setLoggedOut(userIds: Collection<Id>) = withContext(dispatchers.Io) {
        sharedPreferences[PREF_ALL_LOGGED_IN] =
            sharedPreferences.get(PREF_ALL_LOGGED_IN, emptySet<String>()) - userIds.map { it.s }

        setSaved(userIds)
    }

    /**
     * Completely remove the given [userId] from logged in and logged out lists
     */
    suspend fun remove(userId: Id) = withContext(dispatchers.Io) {
        sharedPreferences[PREF_ALL_LOGGED_IN] =
            sharedPreferences.get(PREF_ALL_LOGGED_IN, emptySet<String>()) - userId.s

        sharedPreferences[PREF_ALL_SAVED] =
            sharedPreferences.get(PREF_ALL_LOGGED_IN, emptySet<String>()) - userId.s
    }

    private suspend fun setSaved(userIds: Collection<Id>) = withContext(dispatchers.Io) {
        sharedPreferences[PREF_ALL_SAVED] =
            sharedPreferences.get(PREF_ALL_SAVED, emptySet<String>()) + userIds.map { it.s }
    }

    @Deprecated(
        "Use 'setLoggedIn' with User Id",
        ReplaceWith("setLoggedIn(userId)"),
        DeprecationLevel.ERROR
    )
    fun onSuccessfulLogin(username: String) {
        unsupported
    }

    @Deprecated(
        "Use 'setLoggedOut' with User Id",
        ReplaceWith("setLoggedOut(userId)"),
        DeprecationLevel.ERROR
    )
    fun onSuccessfulLogout(username: String) {
        unsupported
    }

    @Deprecated(
        "Use 'remove' with User Id",
        ReplaceWith("remove(userId)"),
        DeprecationLevel.ERROR
    )
    fun removeFromSaved(username: String) {
        unsupported
    }

    suspend fun allLoggedIn(): Set<Id> = withContext(dispatchers.Io) {
        sharedPreferences.get(PREF_ALL_LOGGED_IN, emptySet())
    }

    suspend fun allLoggedOut(): Set<Id> = withContext(dispatchers.Io) {
        allSaved() - allLoggedIn()
    }

    suspend fun allSaved(): Set<Id> = withContext(dispatchers.Io) {
        sharedPreferences.get(PREF_ALL_SAVED, emptySet())
    }

    @Deprecated(
        "Use 'allLoggedIn' with User Id",
        ReplaceWith("allLoggedIn()"),
        DeprecationLevel.ERROR
    )
    fun getLoggedInUsers(): List<String> =
        unsupported

    @Deprecated(
        "This components is not aware of the current user, nor the primary one, so it's not the right" +
            "candidate for this. No replacement provided",
        level = DeprecationLevel.ERROR
    )
    fun getNextLoggedInAccountOtherThan(username: String, currentPrimary: String): String? {
        val otherLoggedInAccounts = sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, null)
            ?.minus(username)

        return otherLoggedInAccounts
            ?.find { it == currentPrimary }
            ?: otherLoggedInAccounts?.firstOrNull()
    }

    @Deprecated(
        "Use 'allLoggedOut' with User Id",
        ReplaceWith("allLoggedOut()"),
        DeprecationLevel.ERROR
    )
    fun getSavedUsers(): List<String> =
        sharedPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, null) ?: emptyList()

    /**
     * Removes all known lists of usernames.
     */
    suspend fun clear() = withContext(dispatchers.Io) {
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

            accountManager.apply {
                // SecureSharedPreferences.UsernameToIdMigration has already printed to log if some id is not
                //   found, so we just ignore it here
                setSaved(allSavedUsernames.mapNotNull { allUsernamesToIds[it] })
                setLoggedIn(allLoggedUsernames.mapNotNull { allUsernamesToIds[it] })
            }

            defaultSharedPreferences.edit {
                remove(PREF_USERNAMES_LOGGED_OUT)
                remove(PREF_USERNAMES_LOGGED_IN)
            }
        }

    }

    companion object {

        @Deprecated("Inject the constructor directly")
        fun getInstance(context: Context): AccountManager =
            AccountManager(
                PreferenceManager.getDefaultSharedPreferences(context),
                ApplicationModule.dispatcherProvider()
            )
    }
}
