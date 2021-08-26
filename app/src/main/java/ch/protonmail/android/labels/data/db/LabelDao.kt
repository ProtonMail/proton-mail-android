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

import androidx.paging.DataSource
import androidx.room.Dao
import androidx.room.Query
import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.data.model.LabelId
import kotlinx.coroutines.flow.Flow
import me.proton.core.data.room.db.BaseDao
import me.proton.core.domain.entity.UserId

@Dao
abstract class LabelDao : BaseDao<LabelEntity>(){

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_USER_ID=:userId ORDER BY $COLUMN_LABEL_ORDER")
    abstract fun observeAllLabels(userId: UserId): Flow<List<LabelEntity>>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_USER_ID=:userId ORDER BY $COLUMN_LABEL_ORDER ")
    abstract suspend fun findAllLabels(userId: UserId): List<LabelEntity>

    @Query(
        """
        SELECT * FROM $TABLE_LABELS 
        WHERE $COLUMN_LABEL_ID IN (:labelIds) 
        AND $COLUMN_LABEL_USER_ID=:userId  
        ORDER BY $COLUMN_LABEL_ORDER
        """
    )
    abstract fun observeLabelsById(userId: UserId, labelIds: List<LabelId>): Flow<List<LabelEntity>>

    @Query(
        """
        SELECT * FROM $TABLE_LABELS 
        WHERE $COLUMN_LABEL_ID IN (:labelIds) 
        AND $COLUMN_LABEL_USER_ID=:userId
        ORDER BY $COLUMN_LABEL_ORDER
        """
    )
    abstract suspend fun findLabelsById(userId: UserId, labelIds: List<LabelId>): List<LabelEntity>

    @Query(
        """
        SELECT * FROM $TABLE_LABELS 
        WHERE $COLUMN_LABEL_ID=:labelId 
        ORDER BY $COLUMN_LABEL_ORDER
        """
    )
    abstract suspend fun findLabelById(labelId: LabelId): LabelEntity?

    @Query(
        """
        SELECT * FROM $TABLE_LABELS 
        WHERE $COLUMN_LABEL_TYPE = ${Constants.LABEL_TYPE_MESSAGE_LABEL} 
        AND $COLUMN_LABEL_USER_ID=:userId 
        ORDER BY $COLUMN_LABEL_ORDER
        """
    )
    abstract fun findAllLabelsPaged(userId: UserId): DataSource.Factory<Int, LabelEntity>

    @Query(
        """
        SELECT * FROM $TABLE_LABELS 
        WHERE $COLUMN_LABEL_TYPE = ${Constants.LABEL_TYPE_MESSAGE_FOLDERS} 
        AND $COLUMN_LABEL_USER_ID=:userId 
        ORDER BY $COLUMN_LABEL_ORDER
        """
    )
    abstract fun findAllFoldersPaged(userId: UserId): DataSource.Factory<Int, LabelEntity>

    @Query("DELETE FROM $TABLE_LABELS")
    abstract fun clearLabelsCache()

    @Query("DELETE FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID=:labelId")
    abstract suspend fun deleteLabelById(labelId: LabelId)

    @Query("DELETE FROM $TABLE_LABELS WHERE $COLUMN_LABEL_USER_ID=:userId ")
    abstract suspend fun deleteAllLabels(userId: UserId)
}
