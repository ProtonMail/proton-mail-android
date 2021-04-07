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
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.util.requireNotBlank
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountDetails
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.account.domain.entity.SessionState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.session.Session
import me.proton.core.network.domain.session.SessionId
import timber.log.Timber
import javax.inject.Inject

class CoreAccountManagerMigration @Inject constructor(
    private val context: Context,
    private val accountManager: AccountManager
) {

    suspend fun migrateLoggedInAccounts(usernameIdMap: Map<String, Id>) {
        usernameIdMap.forEach { entry ->
            val username = entry.key
            val id = entry.value
            val tokenManager = TokenManager.getInstance(context, id)
            runCatching {
                Session(
                    sessionId = SessionId(requireNotBlank(tokenManager.sessionId)),
                    accessToken = requireNotBlank(tokenManager.accessToken),
                    refreshToken = requireNotBlank(tokenManager.refreshToken),
                    headers = null,
                    scopes = tokenManager.scope.split(" ")
                )
            }.fold(
                onFailure = { Timber.d(it) },
                onSuccess = { session ->
                    accountManager.addAccount(
                        account = Account(
                            userId = UserId(id.s),
                            username = username,
                            email = null,
                            state = AccountState.Ready,
                            sessionId = session.sessionId,
                            sessionState = SessionState.Authenticated,
                            details = AccountDetails(
                                session = null,
                                humanVerification = null
                            )
                        ),
                        session = session
                    )
                }
            )
            tokenManager.clear()
        }
    }
}
