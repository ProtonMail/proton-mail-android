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
package ch.protonmail.android.pendingaction.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.protonmail.android.pendingaction.data.model.COLUMN_PENDING_SEND_LOCAL_DB_ID
import ch.protonmail.android.pendingaction.data.model.COLUMN_PENDING_SEND_MESSAGE_ID
import ch.protonmail.android.pendingaction.data.model.COLUMN_PENDING_SEND_OFFLINE_MESSAGE_ID
import ch.protonmail.android.pendingaction.data.model.COLUMN_PENDING_UPLOAD_MESSAGE_ID
import ch.protonmail.android.pendingaction.data.model.PendingSend
import ch.protonmail.android.pendingaction.data.model.PendingUpload
import ch.protonmail.android.pendingaction.data.model.TABLE_PENDING_SEND
import ch.protonmail.android.pendingaction.data.model.TABLE_PENDING_UPLOADS

@Dao
interface PendingActionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPendingForSend(pendingSend: PendingSend)

    @Query("SELECT * FROM $TABLE_PENDING_SEND")
    fun findAllPendingSendsAsync(): LiveData<List<PendingSend>>

    @Query("SELECT * FROM $TABLE_PENDING_SEND WHERE $COLUMN_PENDING_SEND_MESSAGE_ID = :messageId")
    fun findPendingSendByMessageIdBlocking(messageId: String): PendingSend?

    @Query("SELECT * FROM $TABLE_PENDING_SEND WHERE $COLUMN_PENDING_SEND_MESSAGE_ID = :messageId")
    suspend fun findPendingSendByMessageId(messageId: String): PendingSend?

    @Query("DELETE FROM $TABLE_PENDING_SEND WHERE $COLUMN_PENDING_SEND_MESSAGE_ID = :messageId")
    fun deletePendingSendByMessageId(messageId: String)

    @Query("DELETE FROM $TABLE_PENDING_SEND WHERE $COLUMN_PENDING_SEND_LOCAL_DB_ID = :messageDbId")
    fun deletePendingSendByDbId(messageDbId: Long)

    @Query("SELECT * FROM $TABLE_PENDING_SEND WHERE $COLUMN_PENDING_SEND_OFFLINE_MESSAGE_ID = :offlineMessageId")
    fun findPendingSendByOfflineMessageId(offlineMessageId: String): PendingSend?

    @Query("SELECT * FROM $TABLE_PENDING_SEND WHERE $COLUMN_PENDING_SEND_LOCAL_DB_ID = :dbId")
    fun findPendingSendByDbId(dbId: Long): PendingSend?

    @Query("DELETE FROM $TABLE_PENDING_SEND")
    fun clearPendingSendCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertPendingForUpload(pendingUpload: PendingUpload)

    @Query("SELECT * FROM $TABLE_PENDING_UPLOADS")
    fun findAllPendingUploadsAsync(): LiveData<List<PendingUpload>>

    @Query("SELECT * FROM $TABLE_PENDING_UPLOADS WHERE $COLUMN_PENDING_UPLOAD_MESSAGE_ID = :messageId")
    fun findPendingUploadByMessageIdBlocking(messageId: String): PendingUpload?

    @Query("SELECT * FROM $TABLE_PENDING_UPLOADS WHERE $COLUMN_PENDING_UPLOAD_MESSAGE_ID = :messageId")
    suspend fun findPendingUploadByMessageId(messageId: String): PendingUpload?

    @Query("DELETE FROM $TABLE_PENDING_UPLOADS WHERE $COLUMN_PENDING_UPLOAD_MESSAGE_ID IN (:messageId)")
    fun deletePendingUploadByMessageId(vararg messageId: String)

    @Query("DELETE FROM $TABLE_PENDING_UPLOADS")
    fun clearPendingUploadCache()
}
