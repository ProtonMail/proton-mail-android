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
package ch.protonmail.android.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.protonmail.android.data.local.model.AttachmentMetadata
import ch.protonmail.android.data.local.model.COLUMN_ATTACHMENT_FILE_SIZE
import ch.protonmail.android.data.local.model.COLUMN_ATTACHMENT_FOLDER_LOCATION
import ch.protonmail.android.data.local.model.COLUMN_ATTACHMENT_ID
import ch.protonmail.android.data.local.model.TABLE_ATTACHMENT_METADATA
import kotlinx.coroutines.flow.Flow

@Dao
interface AttachmentMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachmentMetadata(attachmentMetadata: AttachmentMetadata)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAttachmentMetadataBlocking(attachmentMetadata: AttachmentMetadata)

    @Delete
    fun deleteAttachmentMetadata(attachmentMetadata: AttachmentMetadata)

    @Query("DELETE FROM $TABLE_ATTACHMENT_METADATA")
    fun clearAttachmentMetadataCache()

    @Query("SELECT SUM($COLUMN_ATTACHMENT_FILE_SIZE) size FROM $TABLE_ATTACHMENT_METADATA")
    fun getAllAttachmentsSizeUsed(): Flow<Long?>

    @Deprecated("Use Flow variant", ReplaceWith("getAllAttachmentsSizeUsed().first()"))
    @Query("SELECT SUM($COLUMN_ATTACHMENT_FILE_SIZE) size FROM $TABLE_ATTACHMENT_METADATA")
    fun getAllAttachmentsSizeUsedBlocking(): Long?

    @Query("SELECT * FROM $TABLE_ATTACHMENT_METADATA WHERE $COLUMN_ATTACHMENT_FOLDER_LOCATION = :messageId")
    fun getAllAttachmentsForMessage(messageId: String): List<AttachmentMetadata>

    @Query("""
        SELECT * FROM $TABLE_ATTACHMENT_METADATA 
        WHERE $COLUMN_ATTACHMENT_FOLDER_LOCATION = :messageId AND $COLUMN_ATTACHMENT_ID = :attachmentId
    """)
    fun getAttachmentMetadataForMessageAndAttachmentId(
        messageId: String,
        attachmentId: String
    ): AttachmentMetadata?

    @Query("SELECT * FROM $TABLE_ATTACHMENT_METADATA")
    fun getAllAttachmentsMetadata(): List<AttachmentMetadata>
}
