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

package ch.protonmail.android.feature.account

import android.content.Context
import androidx.core.content.edit
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.core.Constants
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.util.requireNotBlank
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.libs.core.preferences.getString
import kotlinx.coroutines.runBlocking
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountDetails
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.account.domain.entity.SessionState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.crypto.common.keystore.encryptWith
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.Session
import me.proton.core.network.domain.session.SessionId
import me.proton.core.user.domain.UserManager
import timber.log.Timber
import javax.inject.Inject

class CoreAccountManagerMigration @Inject constructor(
    private val context: Context,
    private val keyStoreCrypto: KeyStoreCrypto,
    private val accountManager: AccountManager,
    private val userManager: UserManager,
    private val oldAccountManager: ch.protonmail.android.api.AccountManager
) {

    data class Migration(
        val account: Account,
        val session: Session,
        val passphrase: String
    )

    fun migrateBlocking() = runBlocking {
        val loggedInUserIds = oldAccountManager.getLoggedIn().map { Id(it) }
        if (loggedInUserIds.isNotEmpty()) {
            migrateLoggedInAccounts(loggedInUserIds)
            oldAccountManager.clear()
            // TODO: SetPrimary as it was in prefs[PREF_CURRENT_USER_ID] ?
        }
    }

    private suspend fun migrateLoggedInAccounts(userIds: List<Id>) {
        userIds.forEach { id ->
            val tokenManager = TokenManager.getInstance(context, id)
            val prefs = SecureSharedPreferences.getPrefsForUser(context, id)
            val username = prefs.getString(Constants.Prefs.PREF_USER_NAME)
            val passphrase = prefs.getString(PREF_MAILBOX_PASSWORD)
            runCatching {
                val session = Session(
                    sessionId = SessionId(requireNotBlank(tokenManager.sessionId)),
                    accessToken = requireNotBlank(tokenManager.accessToken),
                    refreshToken = requireNotBlank(tokenManager.refreshToken),
                    headers = null,
                    scopes = tokenManager.scope.split(" ")
                )
                val account = Account(
                    userId = UserId(id.s),
                    username = requireNotBlank(username),
                    email = null,
                    state = AccountState.Ready,
                    sessionId = session.sessionId,
                    sessionState = SessionState.Authenticated,
                    details = AccountDetails(
                        session = null,
                        humanVerification = null
                    )
                )
                Migration(account, session, requireNotBlank(passphrase))
            }.fold(
                onFailure = { Timber.w(it) },
                onSuccess = { migration ->
                    // Add account to core AccountManager.
                    accountManager.addAccount(account = migration.account, session = migration.session)
                    // Unlock Account/User with provided passphrase.
                    PlainByteArray(migration.passphrase.toByteArray()).use {
                        userManager.unlockWithPassphrase(migration.account.userId, it.encryptWith(keyStoreCrypto))
                    }
                }
            )
            // Clear migrated data.
            tokenManager.clear()
            prefs.edit { remove(PREF_MAILBOX_PASSWORD) }
        }
    }

    companion object {

        private const val PREF_MAILBOX_PASSWORD = "mailbox_password"
    }
}
