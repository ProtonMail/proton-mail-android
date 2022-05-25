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

package ch.protonmail.android.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.protonmail.android.data.local.model.MessagePreferenceEntity
import ch.protonmail.android.data.local.model.MessagePreferenceEntity.Companion.COLUMN_MESSAGE_ID
import ch.protonmail.android.data.local.model.MessagePreferenceEntity.Companion.TABLE_MESSAGE_PREFERENCE
import me.proton.core.data.room.db.BaseDao

@Dao
abstract class MessagePreferenceDao : BaseDao<MessagePreferenceEntity>() {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun saveMessagePreference(messagePreferenceEntity: MessagePreferenceEntity): Long

    @Query(
        """
        SELECT *
        FROM $TABLE_MESSAGE_PREFERENCE
        WHERE $COLUMN_MESSAGE_ID = :messageId
    """
    )
    abstract suspend fun findMessagePreference(messageId: String): MessagePreferenceEntity?
}
