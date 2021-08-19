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
import ch.protonmail.android.core.Constants

// region constants
const val TABLE_CONTACT_LABEL = "ContactLabel"
// endregion

// TODO: Investigate if it can be replaced with [LabelEntity] and removed
@Entity(
    tableName = TABLE_CONTACT_LABEL,
    indices = [Index(COLUMN_LABEL_ID, unique = true)]
)
data class ContactLabelEntity @JvmOverloads constructor(

    @ColumnInfo(name = COLUMN_LABEL_ID)
    @PrimaryKey
    val id: String = "",

    @ColumnInfo(name = COLUMN_LABEL_NAME)
    val name: String = "",

    @ColumnInfo(name = COLUMN_LABEL_COLOR)
    val color: String = "",

    @ColumnInfo(name = COLUMN_LABEL_ORDER)
    val order: Int = 0,

    @ColumnInfo(name = COLUMN_LABEL_TYPE)
    val type: Int = Constants.LABEL_TYPE_MESSAGE_LABEL,

    @ColumnInfo(name = COLUMN_LABEL_PATH)
    val path: String = "",

    @ColumnInfo(name = COLUMN_LABEL_NOTIFY)
    val notify: Int = 0,

    @ColumnInfo(name = COLUMN_LABEL_PARENT_ID)
    val parentId: String = "",

    @ColumnInfo(name = COLUMN_LABEL_EXPANDED)
    val expanded: Int = 0, // v4

    @ColumnInfo(name = COLUMN_LABEL_STICKY)
    val sticky: Int = 0, // v4

    // TODO: Remove these two in the new DB
    @Deprecated("This value has been removed in the v4 of Labels API, please do not use")
    @ColumnInfo(name = COLUMN_LABEL_DISPLAY)
    val display: Int = 0,

    @Deprecated("This value has been removed in the v4 of Labels API, please do not use")
    @ColumnInfo(name = COLUMN_LABEL_EXCLUSIVE)
    val exclusive: Boolean = false
)
