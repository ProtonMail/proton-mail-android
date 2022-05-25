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
import android.preference.PreferenceManager
import androidx.core.content.edit
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.di.ApplicationModule
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.extensions.obfuscateUsername
import ch.protonmail.android.utils.getStringList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.android.sharedpreferences.clearOnly
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

// region constants
private const val PREF_ALL_SAVED = "Pref.saved.ids"
private const val PREF_ALL_LOGGED_IN = "Pref.loggedIn.ids"
const val PREF_USERNAMES_LOGGED_IN = "PREF_USERNAMES_LOGGED_IN"
const val PREF_USERNAMES_LOGGED_OUT = "PREF_USERNAMES_LOGGED_OUT" // logged out but saved
// endregion

@Deprecated("Replaced by Core AccountManager.")
class AccountManager(
    @DefaultSharedPreferences
    private val sharedPreferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) {

    @Deprecated("Replaced by Core AccountManager.")
    suspend fun getLoggedIn(): List<String> = withContext(dispatchers.Io) {
        sharedPreferences[PREF_ALL_LOGGED_IN] ?: emptyList()
    }

    @Deprecated("Replaced by Core AccountManager.")
    suspend fun clear() = withContext(dispatchers.Io) {
        sharedPreferences.clearOnly(PREF_ALL_LOGGED_IN, PREF_ALL_SAVED)
    }

    @Deprecated("For Test only.")
    suspend fun allLoggedInForTest(): Set<UserId> = withContext(dispatchers.Io) {
        sharedPreferences.get(PREF_ALL_LOGGED_IN, emptySet<String>()).map(::UserId).toSet()
    }

    @Deprecated("For Test only.")
    suspend fun allSavedForTest(): Set<UserId> = withContext(dispatchers.Io) {
        sharedPreferences.get(PREF_ALL_SAVED, emptySet<String>()).map(::UserId).toSet()
    }

    @Deprecated("For Test only.")
    suspend fun allLoggedOutForTest(): Set<UserId> = withContext(dispatchers.Io) {
        allSavedForTest() - allLoggedInForTest()
    }

    private suspend fun setLoggedIn(userIds: Collection<UserId>) = withContext(dispatchers.Io) {
        sharedPreferences[PREF_ALL_LOGGED_IN] =
            sharedPreferences.get(PREF_ALL_LOGGED_IN, emptySet<String>()) + userIds.map { it.id }

        setSaved(userIds)
    }

    private suspend fun setSaved(userIds: Collection<UserId>) = withContext(dispatchers.Io) {
        sharedPreferences[PREF_ALL_SAVED] =
            sharedPreferences.get(PREF_ALL_SAVED, emptySet<String>()) + userIds.map { it.id }
    }

    /**
     * Migrate [AccountManager] to use Users' [Id] instead of username
     */
    class UsernameToIdMigration @Inject constructor(
        private val dispatchers: DispatcherProvider,
        private val accountManager: AccountManager,
        private val secureSharedPreferencesMigration: SecureSharedPreferences.UsernameToIdMigration,
        private val userManagerMigration: UserManager.UsernameToIdMigration,
        @DefaultSharedPreferences
        private val defaultSharedPreferences: SharedPreferences
    ) {

        suspend operator fun invoke() = withContext(dispatchers.Io) {
            // Read directly from SharedPreferences as no function for get usernames will be available from
            //  AccountManager after the migration
            val allSavedUsernames = defaultSharedPreferences
                .getStringList(PREF_USERNAMES_LOGGED_OUT, null) ?: emptyList()
            val allLoggedUsernames = defaultSharedPreferences
                .getStringList(PREF_USERNAMES_LOGGED_IN, null) ?: emptyList()

            val allUsernames = allSavedUsernames + allLoggedUsernames
            if (allUsernames.isEmpty())
                return@withContext

            Timber.v(
                """
                |Migrating AccountManger:
                |  saved username: ${ allSavedUsernames.joinToString { it.obfuscateUsername() } }
                |  logged in usernames: ${ allLoggedUsernames.joinToString { it.obfuscateUsername() } }
            """.trimMargin("|")
            )

            val allUsernamesToIds = secureSharedPreferencesMigration(allUsernames)

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

            userManagerMigration(allUsernamesToIds)
        }

        fun blocking() {
            runBlocking { invoke() }
        }
    }

    companion object {

        @Deprecated("Inject the constructor directly", ReplaceWith("accountManager"))
        fun getInstance(context: Context): AccountManager =
            AccountManager(
                PreferenceManager.getDefaultSharedPreferences(context),
                ApplicationModule.dispatcherProvider()
            )
    }
}
