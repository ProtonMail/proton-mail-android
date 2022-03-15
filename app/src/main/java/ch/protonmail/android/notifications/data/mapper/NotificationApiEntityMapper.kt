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

package ch.protonmail.android.notifications.data.mapper

import ch.protonmail.android.notifications.data.local.model.NotificationEntity
import ch.protonmail.android.notifications.data.remote.model.PushNotification
import ch.protonmail.android.notifications.domain.model.NotificationType
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class NotificationApiEntityMapper @Inject constructor() : Mapper<PushNotification, NotificationEntity> {

    fun toEntity(model: PushNotification, userId: UserId): NotificationEntity {
        val data = checkNotNull(model.data)
        return NotificationEntity(
            userId = userId,
            messageId = data.messageId,
            notificationTitle = data.sender?.let {
                it.senderName.ifEmpty { it.senderAddress }
            } ?: EMPTY_STRING,
            notificationBody = data.body,
            url = data.url,
            type = requireNotNull(NotificationType.fromStringOrNull(model.type)) { "Notification type is null" }
        )
    }
}
