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
import androidx.room.PrimaryKey

const val TABLE_PENDING_DRAFT = "pending_draft"
const val COLUMN_PENDING_DRAFT_MESSAGE_ID = "message_db_id"

@Entity(tableName = TABLE_PENDING_DRAFT)
data class PendingDraft(


    @PrimaryKey
    @ColumnInfo(name = COLUMN_PENDING_DRAFT_MESSAGE_ID)
    var messageDbId: Long
)
