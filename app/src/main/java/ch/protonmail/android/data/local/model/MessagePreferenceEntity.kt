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

package ch.protonmail.android.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import ch.protonmail.android.data.local.model.MessagePreferenceEntity.Companion.TABLE_MESSAGE_PREFERENCE

@Entity(
    tableName = TABLE_MESSAGE_PREFERENCE
)
data class MessagePreferenceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = COLUMN_ID)
    val id: Long = 0,

    @ColumnInfo(name = COLUMN_MESSAGE_ID)
    val messageId: String,

    @ColumnInfo(name = COLUMN_VIEW_IN_DARK_MODE)
    val viewInDarkMode: Boolean,
) {
    companion object {
        const val TABLE_MESSAGE_PREFERENCE = "message_preference"
        const val COLUMN_ID = "ID"
        const val COLUMN_MESSAGE_ID = "message_id"
        const val COLUMN_VIEW_IN_DARK_MODE = "view_in_dark_mode"
    }
}
