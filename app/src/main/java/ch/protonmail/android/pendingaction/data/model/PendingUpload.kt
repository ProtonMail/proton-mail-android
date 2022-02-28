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
package ch.protonmail.android.pendingaction.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

const val TABLE_PENDING_UPLOADS = "pending_uploads"
const val COLUMN_PENDING_UPLOAD_MESSAGE_ID = "message_id"

@Entity(tableName = TABLE_PENDING_UPLOADS)
data class PendingUpload(

    @PrimaryKey
    @ColumnInfo(name = COLUMN_PENDING_UPLOAD_MESSAGE_ID)
    var messageId: String
)
