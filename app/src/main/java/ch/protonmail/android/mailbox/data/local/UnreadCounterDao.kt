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
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterDatabaseModel.Companion.COLUMN_TYPE
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterDatabaseModel.Companion.COLUMN_USER_ID
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterDatabaseModel.Companion.TABLE_NAME
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
internal abstract class UnreadCounterDao : BaseDao<UnreadCounterDatabaseModel>() {

    fun observeMessagesUnreadCounters(userId: UserId): Flow<List<UnreadCounterDatabaseModel>> =
        observeUnreadCounters(userId, UnreadCounterDatabaseModel.Type.MESSAGES)

    fun observeConversationsUnreadCounters(userId: UserId): Flow<List<UnreadCounterDatabaseModel>> =
        observeUnreadCounters(userId, UnreadCounterDatabaseModel.Type.CONVERSATIONS)

    @Query(
        """
            SELECT * FROM $TABLE_NAME
            WHERE 
              $COLUMN_USER_ID = :userId AND
              $COLUMN_TYPE = :type
        """
    )
    abstract fun observeUnreadCounters(
        userId: UserId,
        type: UnreadCounterDatabaseModel.Type
    ): Flow<List<UnreadCounterDatabaseModel>>

    suspend fun insertOrUpdate(counters: Collection<UnreadCounterDatabaseModel>) {
        insertOrUpdate(*counters.toTypedArray())
    }
}
