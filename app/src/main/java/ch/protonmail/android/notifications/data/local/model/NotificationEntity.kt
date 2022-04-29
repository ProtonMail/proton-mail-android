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
package ch.protonmail.android.notifications.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.notifications.domain.model.NotificationType
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity

const val TABLE_NOTIFICATION = "NotificationEntity"
internal const val COLUMN_NOTIFICATION_MESSAGE_ID = "messageId"
internal const val COLUMN_NOTIFICATION_USER_ID = "userId"
internal const val COLUMN_NOTIFICATION_TITLE = "notificationTitle"
internal const val COLUMN_NOTIFICATION_BODY = "notificationBody"
internal const val COLUMN_NOTIFICATION_URL = "url"
internal const val COLUMN_NOTIFICATION_TYPE = "type"

@Entity(
    tableName = TABLE_NOTIFICATION,
    primaryKeys = [COLUMN_NOTIFICATION_MESSAGE_ID],
    indices = [
        Index(COLUMN_NOTIFICATION_MESSAGE_ID),
        Index(COLUMN_NOTIFICATION_USER_ID)
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = [COLUMN_NOTIFICATION_USER_ID],
            childColumns = [COLUMN_NOTIFICATION_USER_ID],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NotificationEntity(

    @ColumnInfo(name = COLUMN_NOTIFICATION_USER_ID)
    val userId: UserId,

    @ColumnInfo(name = COLUMN_NOTIFICATION_MESSAGE_ID)
    val messageId: String,

    @ColumnInfo(name = COLUMN_NOTIFICATION_TITLE)
    val notificationTitle: String,

    @ColumnInfo(name = COLUMN_NOTIFICATION_BODY)
    val notificationBody: String,

    @ColumnInfo(name = COLUMN_NOTIFICATION_URL)
    val url: String?,

    @ColumnInfo(name = COLUMN_NOTIFICATION_TYPE)
    val type: NotificationType
)
