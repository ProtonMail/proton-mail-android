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
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel.Companion.COLUMN_ID
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel.Companion.COLUMN_LABELS
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel.Companion.COLUMN_NUM_UNREAD
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel.Companion.COLUMN_USER_ID
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel.Companion.TABLE_CONVERSATIONS
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.db.BaseDao

@Dao
abstract class ConversationDao : BaseDao<ConversationDatabaseModel>() {

    @Query(
        """
        SELECT * FROM $TABLE_CONVERSATIONS 
        WHERE $COLUMN_USER_ID = :userId
        """
    )
    abstract fun getConversations(userId: String): Flow<List<ConversationDatabaseModel>>

    @Query(
        """
        SELECT * FROM $TABLE_CONVERSATIONS
        WHERE $COLUMN_ID = :conversationId AND $COLUMN_USER_ID = :userId
        """
    )
    abstract fun getConversation(conversationId: String, userId: String): Flow<ConversationDatabaseModel>

    @Query(
        """
            DELETE FROM $TABLE_CONVERSATIONS
            WHERE $COLUMN_ID = :conversationId 
            AND $COLUMN_USER_ID = :userId
            """
    )
    abstract fun deleteConversation(conversationId: String, userId: String)

    @Query("DELETE FROM $TABLE_CONVERSATIONS")
    abstract fun clear()

    @Query(
        """
            UPDATE $TABLE_CONVERSATIONS
            SET $COLUMN_NUM_UNREAD = :numUnreadMessages
            WHERE $COLUMN_ID = :conversationId
            """
    )
    abstract suspend fun updateNumUnreadMessages(numUnreadMessages: Int, conversationId: String)

    @Query(
        """
            UPDATE $TABLE_CONVERSATIONS
            SET $COLUMN_LABELS = :labels
            WHERE $COLUMN_ID = :conversationId
        """
    )
    abstract suspend fun updateLabels(
        labels: List<LabelContextDatabaseModel>,
        conversationId: String
    )
}
