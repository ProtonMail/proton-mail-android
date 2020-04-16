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
package ch.protonmail.android.api.models.room.notifications

import androidx.room.*

/**
 * Created by Kamil Rajtar on 14.07.18.
 */
@Dao
abstract class NotificationsDatabase {

	@Query("SELECT * FROM Notification WHERE message_id=:messageId")
	abstract fun findByMessageId(messageId: String): Notification?

	@Query("DELETE FROM Notification WHERE message_id=:messageId")
	abstract fun deleteByMessageId(messageId: String)

	@Insert(onConflict = OnConflictStrategy.REPLACE)
	abstract fun insertNotification(notification: Notification)

	@Query("DELETE FROM Notification")
	abstract fun clearNotificationCache()

	@Query("SELECT * FROM Notification")
	abstract fun findAllNotifications(): List<Notification>

	@Delete
	abstract fun deleteNotifications(notifications: List<Notification>)

	@Query("SELECT COUNT(${COLUMN_NOTIFICATION_MESSAGE_ID}) FROM $TABLE_NOTIFICATION")
	abstract fun count(): Int
}