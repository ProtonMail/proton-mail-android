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
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ch.protonmail.android.core.Constants
import kotlinx.coroutines.flow.Flow

@Dao
interface LabelDao {

    @Query("SELECT * FROM $TABLE_LABELS ORDER BY $COLUMN_LABEL_ORDER")
    fun observeAllLabels(): Flow<List<LabelEntity>>

    @Query("SELECT * FROM $TABLE_LABELS ORDER BY $COLUMN_LABEL_ORDER")
    suspend fun findAllLabels(): List<LabelEntity>

    @Query(
        """
        SELECT * FROM $TABLE_LABELS 
        WHERE $COLUMN_LABEL_TYPE = ${Constants.LABEL_TYPE_MESSAGE_LABEL} 
        ORDER BY $COLUMN_LABEL_ORDER
        """
    )
    fun findAllLabelsPaged(): DataSource.Factory<Int, LabelEntity>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID IN (:labelIds) ORDER BY $COLUMN_LABEL_ORDER")
    fun observeLabelsById(labelIds: List<String>): Flow<List<LabelEntity>>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID IN (:labelIds)")
    suspend fun findLabelsById(labelIds: List<String>): List<LabelEntity>

    @Query("SELECT * FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID=:labelId")
    suspend fun findLabelById(labelId: String): LabelEntity?

    @Query("DELETE FROM $TABLE_LABELS")
    fun clearLabelsCache()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveLabel(label: LabelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAllLabels(labels: List<LabelEntity>): List<Long>

    @Query("DELETE FROM $TABLE_LABELS WHERE $COLUMN_LABEL_ID=:labelId")
    suspend fun deleteLabelById(labelId: String)

}
