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
import ch.protonmail.android.notifications.domain.model.Notification
import ch.protonmail.android.notifications.domain.model.NotificationId
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class NotificationEntityDomainMapper @Inject constructor() : Mapper<NotificationEntity, Notification> {

    fun toNotification(model: NotificationEntity) = Notification(
        id = NotificationId(model.messageId),
        notificationTitle = model.notificationTitle,
        notificationBody = model.notificationBody,
        url = model.url ?: EMPTY_STRING,
        type = model.type
    )

    fun toEntity(model: Notification, userId: UserId) = NotificationEntity(
        messageId = model.id.value,
        userId = userId,
        notificationTitle = model.notificationTitle,
        notificationBody = model.notificationBody,
        url = model.url,
        type = model.type
    )
}
