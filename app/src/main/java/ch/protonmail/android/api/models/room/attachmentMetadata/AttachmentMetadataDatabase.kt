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
package ch.protonmail.android.api.models.room.attachmentMetadata

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface AttachmentMetadataDatabase {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachmentMetadata(attachmentMetadata: AttachmentMetadata)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAttachmentMetadataBlocking(attachmentMetadata: AttachmentMetadata)

    @Delete
    fun deleteAttachmentMetadata(attachmentMetadata: AttachmentMetadata)

    @Query("DELETE FROM $TABLE_ATTACHMENT_METADATA")
    fun clearAttachmentMetadataCache()

    @Query("SELECT SUM($COLUMN_ATTACHMENT_FILE_SIZE) size FROM $TABLE_ATTACHMENT_METADATA")
    fun getAllAttachmentsSizeUsed(): Long

    @Query("SELECT * FROM $TABLE_ATTACHMENT_METADATA WHERE $COLUMN_ATTACHMENT_FOLDER_LOCATION=:messageId")
    fun getAllAttachmentsForMessage(messageId: String): List<AttachmentMetadata>

    @Query("SELECT * FROM $TABLE_ATTACHMENT_METADATA WHERE $COLUMN_ATTACHMENT_FOLDER_LOCATION=:messageId AND $COLUMN_ATTACHMENT_ID=:attachmentId")
    suspend fun getAttachmentMetadataForMessageAndAttachmentId(
        messageId: String,
        attachmentId: String
    ): AttachmentMetadata?

    @Query("SELECT * FROM $TABLE_ATTACHMENT_METADATA")
    fun getAllAttachmentsMetadata(): List<AttachmentMetadata>
}
