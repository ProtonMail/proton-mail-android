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

internal class ClearNotification @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationManager: NotificationManager
) {

    suspend operator fun invoke(
        userId: UserId,
        notificationId: String,
        cancelStatusBarNotificationManually: Boolean = true
    ) {

        if (checkIfShouldCancelSummaryNotification(cancelStatusBarNotificationManually, userId)) {
            notificationManager.cancel(userId.hashCode())
        } else {
            notificationManager.cancel(notificationId.hashCode())
        }
        notificationRepository.deleteNotification(
            userId,
            notificationId
        )
    }

    private fun checkIfShouldCancelSummaryNotification(
        cancelStatusBarNotificationManually: Boolean,
        userId: UserId
    ): Boolean =
        notificationManager.activeNotifications
            .count { userId.id in it.groupKey } <= 2 && cancelStatusBarNotificationManually

}
