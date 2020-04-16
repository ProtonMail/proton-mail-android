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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// region constants
const val TABLE_NOTIFICATION = "Notification"
const val COLUMN_NOTIFICATION_MESSAGE_ID = "message_id"
const val COLUMN_NOTIFICATION_TITLE = "notification_title"
const val COLUMN_NOTIFICATION_BODY = "notification_body"
// endregion

@Entity(tableName = TABLE_NOTIFICATION, indices=[Index(value=[COLUMN_NOTIFICATION_MESSAGE_ID],unique=true)])
class Notification constructor(
		@ColumnInfo(name = COLUMN_NOTIFICATION_MESSAGE_ID) var messageId: String,
		@ColumnInfo(name = COLUMN_NOTIFICATION_TITLE) val notificationTitle: String,
		@ColumnInfo(name = COLUMN_NOTIFICATION_BODY) val notificationBody: String
) {
	@PrimaryKey
	var dbId: Long? = null
}
