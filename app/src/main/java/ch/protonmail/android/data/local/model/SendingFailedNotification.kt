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
package ch.protonmail.android.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val TABLE_SENDING_FAILED_NOTIFICATION = "SendingFailedNotification"
const val COLUMN_SENDING_FAILED_NOTIFICATION_MESSAGE_ID = "message_id"
const val COLUMN_SENDING_FAILED_NOTIFICATION_MESSAGE_SUBJECT = "message_subject"
const val COLUMN_SENDING_FAILED_NOTIFICATION_ERROR_MESSAGE = "error_message"

@Entity(
    tableName = TABLE_SENDING_FAILED_NOTIFICATION,
    indices = [Index(COLUMN_SENDING_FAILED_NOTIFICATION_MESSAGE_ID, unique = true)]
)
class SendingFailedNotification constructor(

    @ColumnInfo(name = COLUMN_SENDING_FAILED_NOTIFICATION_MESSAGE_ID)
    var messageId: String,

    @ColumnInfo(name = COLUMN_SENDING_FAILED_NOTIFICATION_MESSAGE_SUBJECT)
    var messageSubject: String?,

    @ColumnInfo(name = COLUMN_SENDING_FAILED_NOTIFICATION_ERROR_MESSAGE)
    val errorMessage: String
) {

    @PrimaryKey
    var dbId: Long? = null
}
