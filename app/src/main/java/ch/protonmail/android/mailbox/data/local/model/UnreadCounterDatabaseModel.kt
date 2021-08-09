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

package ch.protonmail.android.mailbox.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterDatabaseModel.Companion.COLUMN_LABEL_ID
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterDatabaseModel.Companion.COLUMN_TYPE
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterDatabaseModel.Companion.COLUMN_USER_ID
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import me.proton.core.domain.entity.UserId

/**
 * Database model for [UnreadCounter]
 */
@Entity(
    tableName = UnreadCounterDatabaseModel.TABLE_NAME,
    primaryKeys = [COLUMN_USER_ID, COLUMN_LABEL_ID, COLUMN_TYPE]
)
internal data class UnreadCounterDatabaseModel(

    @ColumnInfo(name = COLUMN_USER_ID)
    val userId: UserId,

    @ColumnInfo(name = COLUMN_TYPE)
    val type: Type,

    @ColumnInfo(name = COLUMN_LABEL_ID)
    val labelId: String,

    @ColumnInfo(name = COLUMN_UNREAD_COUNT)
    val unreadCount: Int
) {

    enum class Type {
        MESSAGES,
        CONVERSATIONS
    }

    companion object {

        const val TABLE_NAME = "UnreadCounter"
        const val COLUMN_USER_ID = "user_id"
        const val COLUMN_TYPE = "type"
        const val COLUMN_LABEL_ID = "label_id"
        const val COLUMN_UNREAD_COUNT = "unread_count"
    }
}
