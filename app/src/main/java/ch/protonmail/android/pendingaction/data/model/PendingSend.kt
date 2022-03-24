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
package ch.protonmail.android.pendingaction.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val TABLE_PENDING_SEND = "pending_for_sending"
const val COLUMN_PENDING_SEND_ID = "pending_for_sending_id"
const val COLUMN_PENDING_SEND_MESSAGE_ID = "message_id"
const val COLUMN_PENDING_SEND_OFFLINE_MESSAGE_ID = "offline_message_id"
const val COLUMN_PENDING_SEND_SENT = "sent"
const val COLUMN_PENDING_SEND_LOCAL_DB_ID = "local_database_id"

@Entity(tableName = TABLE_PENDING_SEND)
data class PendingSend @JvmOverloads constructor(

    @PrimaryKey
    @ColumnInfo(name = COLUMN_PENDING_SEND_ID)
    var id: String = "",

    @ColumnInfo(name = COLUMN_PENDING_SEND_MESSAGE_ID)
    var messageId: String? = null,

    @ColumnInfo(name = COLUMN_PENDING_SEND_OFFLINE_MESSAGE_ID)
    var offlineMessageId: String? = null,

    @ColumnInfo(name = COLUMN_PENDING_SEND_SENT)
    var sent: Boolean? = null,

    @ColumnInfo(name = COLUMN_PENDING_SEND_LOCAL_DB_ID)
    var localDatabaseId: Long = 0
)
