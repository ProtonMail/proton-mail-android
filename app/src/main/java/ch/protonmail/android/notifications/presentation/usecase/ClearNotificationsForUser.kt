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

package ch.protonmail.android.notifications.presentation.usecase

import android.app.NotificationManager
import ch.protonmail.android.notifications.domain.NotificationRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

internal class ClearNotificationsForUser @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationManager: NotificationManager
) {

    suspend operator fun invoke(
        userId: UserId,
        cancelStatusBarNotificationManually: Boolean = true
    ) {

        if (cancelStatusBarNotificationManually) {
            notificationManager.cancel(userId.hashCode())
        }
        notificationRepository.deleteAllNotificationsByUserId(userId)
    }
}
