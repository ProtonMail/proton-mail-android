/*
 * Copyright (c) 2022 Proton Technologies AG
 * This file is part of Proton Technologies AG and Proton Mail.
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
 * along with Proton Mail. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.protonmail.android.testdata

import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountDetails
import me.proton.core.account.domain.entity.AccountState.Ready
import me.proton.core.account.domain.entity.AccountType.Internal
import me.proton.core.account.domain.entity.SessionDetails
import me.proton.core.account.domain.entity.SessionState.Authenticated
import me.proton.core.network.domain.session.SessionId

object AccountTestData {
    private const val RAW_USERNAME = "username"
    private const val RAW_EMAIL = "email@protonmail.ch"
    private const val INITIAL_EVENT_ID = "event_id"

    val primaryAccount = Account(
        userId = UserTestData.userId,
        username = RAW_USERNAME,
        email = RAW_EMAIL,
        state = Ready,
        sessionId = SessionId(UserTestData.userId.id),
        sessionState = Authenticated,
        details = AccountDetails(
            null,
            SessionDetails(
                initialEventId = INITIAL_EVENT_ID,
                requiredAccountType = Internal,
                secondFactorEnabled = true,
                twoPassModeEnabled = true,
                password = null
            )
        )
    )

    val secondaryAccount = Account(
        userId = UserTestData.secondaryUserId,
        username = RAW_USERNAME,
        email = RAW_EMAIL,
        state = Ready,
        sessionId = SessionId(UserTestData.secondaryUserId.id),
        sessionState = Authenticated,
        details = AccountDetails(
            null,
            SessionDetails(
                initialEventId = INITIAL_EVENT_ID,
                requiredAccountType = Internal,
                secondFactorEnabled = true,
                twoPassModeEnabled = true,
                password = null
            )
        )
    )

    val accounts = listOf(primaryAccount, secondaryAccount)

}
