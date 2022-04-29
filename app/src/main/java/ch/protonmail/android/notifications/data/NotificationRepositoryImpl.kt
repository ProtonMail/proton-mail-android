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

package ch.protonmail.android.notifications.data

import ch.protonmail.android.notifications.data.local.NotificationDao
import ch.protonmail.android.notifications.data.mapper.NotificationApiEntityMapper
import ch.protonmail.android.notifications.data.mapper.NotificationEntityDomainMapper
import ch.protonmail.android.notifications.data.remote.model.PushNotification
import ch.protonmail.android.notifications.domain.NotificationRepository
import ch.protonmail.android.notifications.domain.model.Notification
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

internal class NotificationRepositoryImpl @Inject constructor(
    private val notificationDao: NotificationDao,
    private val notificationApiEntityMapper: NotificationApiEntityMapper,
    private val notificationEntityDomainMapper: NotificationEntityDomainMapper
) : NotificationRepository {

    override suspend fun saveNotification(notification: PushNotification, userId: UserId): Notification? {
        notificationDao.insertOrUpdate(notificationApiEntityMapper.toEntity(notification, userId))
        return getNotificationByIdBlocking(notificationApiEntityMapper.toEntity(notification, userId).messageId)
    }

    override suspend fun deleteNotification(userId: UserId, notificationId: String) {
        notificationDao.deleteByMessageId(notificationId)
    }

    override suspend fun deleteAllNotificationsByUserId(userId: UserId) {
        notificationDao.deleteAllNotificationsByUserId(userId = userId.id)
    }

    override fun deleteAllNotificationsBlocking() {
        notificationDao.deleteAllNotificationsBlocking()
    }

    override fun getNotificationByIdBlocking(messageId: String): Notification? =
        notificationDao.findByMessageIdBlocking(messageId)?.let {
            notificationEntityDomainMapper.toNotification(it)
        }
}
