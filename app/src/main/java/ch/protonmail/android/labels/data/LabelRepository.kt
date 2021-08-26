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

package ch.protonmail.android.labels.data

import androidx.paging.DataSource
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface LabelRepository {

    fun observeAllLabels(userId: UserId): Flow<List<LabelEntity>>

    suspend fun findAllLabels(userId: UserId): List<LabelEntity>

    fun observeLabels(userId: UserId, labelsIds: List<LabelId>): Flow<List<LabelEntity>>

    suspend fun findLabels(userId: UserId, labelsIds: List<LabelId>): List<LabelEntity>

    suspend fun findLabel(labelId: LabelId): LabelEntity?

    fun findLabelBlocking(labelId: LabelId): LabelEntity?

    suspend fun saveLabel(label: LabelEntity)

    suspend fun saveLabels(labels: List<LabelEntity>)

    suspend fun deleteLabel(labelId: LabelId)

    suspend fun deleteAllLabels(userId: UserId)

    fun findAllLabelsPaged(userId: UserId): DataSource.Factory<Int, LabelEntity>

    fun findAllFoldersPaged(userId: UserId): DataSource.Factory<Int, LabelEntity>
}
