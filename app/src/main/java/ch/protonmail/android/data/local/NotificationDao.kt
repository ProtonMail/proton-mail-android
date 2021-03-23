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
package ch.protonmail.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ch.protonmail.android.data.local.model.COLUMN_NOTIFICATION_MESSAGE_ID
import ch.protonmail.android.data.local.model.Notification
import ch.protonmail.android.data.local.model.TABLE_NOTIFICATION

@Dao
interface NotificationDao {

    @Query("SELECT * FROM Notification WHERE message_id=:messageId")
    fun findByMessageId(messageId: String): Notification?

    @Query("DELETE FROM Notification WHERE message_id=:messageId")
    fun deleteByMessageId(messageId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertNotification(notification: Notification)

    @Query("DELETE FROM Notification")
    fun clearNotificationCache()

    @Query("SELECT * FROM Notification")
    fun findAllNotifications(): List<Notification>

    @Delete
    fun deleteNotifications(notifications: List<Notification>)

    @Query("SELECT COUNT($COLUMN_NOTIFICATION_MESSAGE_ID) FROM $TABLE_NOTIFICATION")
    fun count(): Int

    @Transaction
    fun insertNewNotificationAndReturnAll(notification: Notification): List<Notification> {
        insertNotification(notification)
        return findAllNotifications()
    }
}
