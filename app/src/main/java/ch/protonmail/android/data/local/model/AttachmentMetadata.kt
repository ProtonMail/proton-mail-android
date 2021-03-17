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

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.io.Serializable

const val TABLE_ATTACHMENT_METADATA = "attachment_metadata"
const val COLUMN_ATTACHMENT_NAME = "name"
const val COLUMN_ATTACHMENT_LOCAL_LOCATION = "location"
const val COLUMN_ATTACHMENT_FOLDER_LOCATION = "folder_location"
const val COLUMN_ATTACHMENT_DOWNLOAD_TIMESTAMP = "download_timestamp"
const val COLUMN_ATTACHMENT_UIR = "attachment_uri"

@Entity(tableName = TABLE_ATTACHMENT_METADATA)
@TypeConverters(value = [AttachmentsMetadataTypeConverter::class])
class AttachmentMetadata constructor(

    @ColumnInfo(name = COLUMN_ATTACHMENT_ID)
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = COLUMN_ATTACHMENT_NAME)
    val name: String,

    @ColumnInfo(name = COLUMN_ATTACHMENT_FILE_SIZE)
    val size: Long,

    @ColumnInfo(name = COLUMN_ATTACHMENT_LOCAL_LOCATION)
    val localLocation: String,

    @ColumnInfo(name = COLUMN_ATTACHMENT_FOLDER_LOCATION)
    val folderLocation: String,

    @ColumnInfo(name = COLUMN_ATTACHMENT_DOWNLOAD_TIMESTAMP)
    val downloadTimestamp: Long,

    @ColumnInfo(name = COLUMN_ATTACHMENT_UIR)
    val uri: Uri?

) : Serializable
