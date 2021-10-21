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

package ch.protonmail.android.mailbox.presentation

import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationModeEnabled
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ConversationModeEnabled @Inject constructor(
    private val observeConversationModeEnabled: ObserveConversationModeEnabled,
    private val accountManager: AccountManager
) {

    /**
     * When location is null, the location is ignored.
     * When userId is null, use the current primary user id.
     */
    operator fun invoke(location: MessageLocationType?, userId: UserId? = null): Boolean {
        return runBlocking {
            val primaryUserId = userId ?: accountManager.getPrimaryUserId().first()
            primaryUserId?.let { observeConversationModeEnabled(primaryUserId, location).first() } ?: false
        }
    }
}
