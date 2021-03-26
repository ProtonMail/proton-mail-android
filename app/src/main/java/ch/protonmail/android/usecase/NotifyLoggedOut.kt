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

package ch.protonmail.android.usecase

import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.servers.notification.NotificationServer
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class NotifyLoggedOut @Inject constructor(
    private val loadUser: LoadUser,
    private val notificationServer: NotificationServer
) {
    suspend operator fun invoke(userId: Id) {
        val user = loadUser(userId).orNull()
        notificationServer.notifyUserLoggedOut(user)
    }

    @Deprecated(
        "Should not be used, necessary only for when non-blocking variant is not suitable",
        ReplaceWith("invoke(userId)")
    )
    fun blocking(userId: Id) {
        runBlocking {
            invoke(userId)
        }
    }
}
