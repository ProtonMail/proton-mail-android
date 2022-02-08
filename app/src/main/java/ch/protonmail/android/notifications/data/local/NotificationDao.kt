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
package ch.protonmail.android.notifications.data.local

import androidx.room.Dao
import androidx.room.Query
import ch.protonmail.android.notifications.data.local.model.COLUMN_NOTIFICATION_MESSAGE_ID
import ch.protonmail.android.notifications.data.local.model.NotificationEntity
import ch.protonmail.android.notifications.data.local.model.TABLE_NOTIFICATION
import me.proton.core.data.room.db.BaseDao

@Dao
internal abstract class NotificationDao : BaseDao<NotificationEntity>() {

    @Query("SELECT * FROM NotificationEntity WHERE message_id=:messageId")
    abstract fun findByMessageId(messageId: String): NotificationEntity?

    @Query("DELETE FROM NotificationEntity WHERE message_id=:messageId")
    abstract suspend fun deleteByMessageId(messageId: String)

    @Query("DELETE FROM NotificationEntity WHERE userId=:userId")
    abstract suspend fun clearNotificationsByUserId(userId: String)

    @Query("DELETE FROM NotificationEntity")
    abstract fun clearNotifications()

    @Query("SELECT COUNT($COLUMN_NOTIFICATION_MESSAGE_ID) FROM $TABLE_NOTIFICATION")
    abstract fun count(): Int

    @Query("SELECT * FROM NotificationEntity")
    abstract fun findAllNotifications(): List<NotificationEntity>
}
