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

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val TABLE_LABELS = "label"
const val COLUMN_LABEL_ID = "ID"
const val COLUMN_LABEL_NAME = "Name"
const val COLUMN_LABEL_COLOR = "Color"
const val COLUMN_LABEL_DISPLAY = "Display"
const val COLUMN_LABEL_ORDER = "LabelOrder"
const val COLUMN_LABEL_EXCLUSIVE = "Exclusive"
const val COLUMN_LABEL_TYPE = "Type"
const val COLUMN_LABEL_PATH = "Path"
const val COLUMN_LABEL_NOTIFY = "Notify"
const val COLUMN_LABEL_PARENT_ID = "ParentID"
const val COLUMN_LABEL_EXPANDED = "Expanded"
const val COLUMN_LABEL_STICKY = "Sticky"

@Entity(
    tableName = TABLE_LABELS,
    indices = [Index(COLUMN_LABEL_ID, unique = true)]
)
data class LabelEntity constructor(

    @PrimaryKey
    @ColumnInfo(name = COLUMN_LABEL_ID)
    val id: String,

    @ColumnInfo(name = COLUMN_LABEL_NAME, index = true)
    val name: String,

    @ColumnInfo(name = COLUMN_LABEL_COLOR, index = true)
    val color: String,

    @ColumnInfo(name = COLUMN_LABEL_ORDER)
    val order: Int,

    @ColumnInfo(name = COLUMN_LABEL_TYPE)
    val type: Int,

    @ColumnInfo(name = COLUMN_LABEL_PATH)
    val path: String,

    @ColumnInfo(name = COLUMN_LABEL_PARENT_ID)
    val parentId: String,

    @ColumnInfo(name = COLUMN_LABEL_EXPANDED)
    val expanded: Int, // v4

    @ColumnInfo(name = COLUMN_LABEL_STICKY)
    val sticky: Int, // v4

    @ColumnInfo(name = COLUMN_LABEL_NOTIFY)
    val notify: Int = 0,

    // TODO: Remove these two in the new DB
    @Deprecated("This value has been removed in the v4 of Labels API, please do not use")
    @ColumnInfo(name = COLUMN_LABEL_EXCLUSIVE)
    val exclusive: Boolean = false,

    @Deprecated("This value has been removed in the v4 of Labels API, please do not use")
    @ColumnInfo(name = COLUMN_LABEL_DISPLAY)
    val display: Int = -1

)
