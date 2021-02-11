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

import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.humanverification.HumanVerificationDetails
import me.proton.core.network.domain.session.Session
import me.proton.core.network.domain.session.SessionId
import me.proton.core.network.domain.session.SessionListener
import me.proton.core.network.domain.session.SessionProvider
import javax.inject.Inject

/**
 * Provide, handle and persist any [Session] changes - based on [TokenManager].
 *
 * Makes the link between the Core Network module and old ProtonMail Token persistence.
 *
 * Note: This will be replaced by Core AccountManager in future.
 */
class TokenSessionManager @Inject constructor(
    val accountManager: AccountManager
) : SessionProvider, SessionListener {

    private fun getTokenManagerBySessionId(sessionId: String): TokenManager? =
        // TODO: Add HashMap for O(1) access time.
        accountManager.getLoggedInUsers()
            .map { username -> TokenManager.getInstance(username) }
            .firstOrNull { it?.uid == sessionId }

    override suspend fun getSession(sessionId: SessionId): Session? =
        getTokenManagerBySessionId(sessionId.id)?.session

    // ProtonMail do not persist userId - only sessionId (TokenManager.uid).
    // Let's assume userId === username.
    override suspend fun getSessionId(userId: UserId): SessionId? =
        TokenManager.getInstance(userId.id)?.uid?.let { SessionId(it) }

    override suspend fun onSessionTokenRefreshed(session: Session) {
        getTokenManagerBySessionId(session.sessionId.id)?.handleRefresh(session)
    }

    override suspend fun onSessionForceLogout(session: Session) {
        getTokenManagerBySessionId(session.sessionId.id)?.clear()
        // TODO: Properly logout user.
    }

    override suspend fun onHumanVerificationNeeded(
        session: Session,
        details: HumanVerificationDetails
    ): SessionListener.HumanVerificationResult {
        TODO("Show HumanVerification UI, and block until success or failure")
    }
}
