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
package ch.protonmail.android.labels.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.data.model.LabelType
import me.proton.core.domain.entity.UserId
import me.proton.core.user.data.entity.UserEntity

internal const val TABLE_LABELS = "LabelEntity"
internal const val COLUMN_LABEL_ID = "id"
internal const val COLUMN_LABEL_USER_ID = "userId"
internal const val COLUMN_LABEL_NAME = "name"
internal const val COLUMN_LABEL_COLOR = "color"
internal const val COLUMN_LABEL_ORDER = "labelOrder"
internal const val COLUMN_LABEL_TYPE = "type"
internal const val COLUMN_LABEL_PATH = "path"
internal const val COLUMN_LABEL_PARENT_ID = "parentID"
internal const val COLUMN_LABEL_NOTIFY = "notify"
internal const val COLUMN_LABEL_EXPANDED = "expanded"
internal const val COLUMN_LABEL_STICKY = "sticky"

@Entity(
    tableName = TABLE_LABELS,
    primaryKeys = [COLUMN_LABEL_ID],
    indices = [
        Index(COLUMN_LABEL_ID),
        Index(COLUMN_LABEL_USER_ID),
        Index(COLUMN_LABEL_TYPE)
    ],
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = [COLUMN_LABEL_USER_ID],
            childColumns = [COLUMN_LABEL_USER_ID],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class LabelEntity(
    @ColumnInfo(name = COLUMN_LABEL_ID)
    val id: LabelId,
    @ColumnInfo(name = COLUMN_LABEL_USER_ID)
    val userId: UserId,
    @ColumnInfo(name = COLUMN_LABEL_NAME)
    val name: String,
    @ColumnInfo(name = COLUMN_LABEL_COLOR)
    val color: String,
    @ColumnInfo(name = COLUMN_LABEL_ORDER)
    val order: Int,
    @ColumnInfo(name = COLUMN_LABEL_TYPE)
    val type: LabelType,
    @ColumnInfo(name = COLUMN_LABEL_PATH)
    val path: String,
    @ColumnInfo(name = COLUMN_LABEL_PARENT_ID)
    val parentId: String,
    @ColumnInfo(name = COLUMN_LABEL_EXPANDED)
    val expanded: Int,
    @ColumnInfo(name = COLUMN_LABEL_STICKY)
    val sticky: Int,
    @ColumnInfo(name = COLUMN_LABEL_NOTIFY)
    val notify: Int,
)
