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

package ch.protonmail.android.mailbox.data.local

import androidx.room.Dao
import androidx.room.Query
import ch.protonmail.android.mailbox.data.local.model.UNREAD_COUNTER_COLUMN_TYPE
import ch.protonmail.android.mailbox.data.local.model.UNREAD_COUNTER_COLUMN_USER_ID
import ch.protonmail.android.mailbox.data.local.model.UNREAD_COUNTER_TABLE_NAME
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class UnreadCounterDao : BaseDao<UnreadCounterEntity>() {

    fun observeMessagesUnreadCounters(userId: UserId): Flow<List<UnreadCounterEntity>> =
        observeUnreadCounters(userId, UnreadCounterEntity.Type.MESSAGES)

    fun observeConversationsUnreadCounters(userId: UserId): Flow<List<UnreadCounterEntity>> =
        observeUnreadCounters(userId, UnreadCounterEntity.Type.CONVERSATIONS)

    @Query(
        """
            SELECT * FROM $UNREAD_COUNTER_TABLE_NAME
            WHERE 
              $UNREAD_COUNTER_COLUMN_USER_ID = :userId AND
              $UNREAD_COUNTER_COLUMN_TYPE = :type
        """
    )
    abstract fun observeUnreadCounters(
        userId: UserId,
        type: UnreadCounterEntity.Type
    ): Flow<List<UnreadCounterEntity>>

    suspend fun insertOrUpdate(counters: Collection<UnreadCounterEntity>) {
        insertOrUpdate(*counters.toTypedArray())
    }

    @Query("DELETE FROM $UNREAD_COUNTER_TABLE_NAME")
    abstract fun clear()
}
