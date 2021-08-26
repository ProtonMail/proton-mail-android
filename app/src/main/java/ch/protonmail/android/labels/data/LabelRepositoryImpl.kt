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

import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.labels.data.db.LabelDao
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.mapper.LabelsMapper
import ch.protonmail.android.labels.data.model.LabelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

internal class LabelRepositoryImpl @Inject constructor(
    private val labelDao: LabelDao,
    private val api: ProtonMailApi,
    private val labelMapper: LabelsMapper
) : LabelRepository {

    override fun observeAllLabels(userId: UserId): Flow<List<LabelEntity>> =
        labelDao.observeAllLabels(userId)

    override suspend fun findAllLabels(userId: UserId): List<LabelEntity> =
        labelDao.findAllLabels(userId)

    override fun observeLabels(userId: UserId, labelsIds: List<LabelId>): Flow<List<LabelEntity>> =
        labelDao.observeLabelsById(userId, labelsIds)

    override suspend fun findLabels(userId: UserId, labelsIds: List<LabelId>): List<LabelEntity> =
        labelDao.findLabelsById(userId, labelsIds)

    override suspend fun findLabel(labelId: LabelId): LabelEntity? =
        labelDao.findLabelById(labelId)

    override fun findLabelBlocking(labelId: LabelId): LabelEntity? {
        return runBlocking {
            findLabel(labelId)
        }
    }

    override suspend fun saveLabels(labels: List<LabelEntity>) {
        Timber.v("Save labels: ${labels.map { it.id.id }}")
        labelDao.saveLabels(labels)
    }

    override suspend fun saveLabel(label: LabelEntity) =
        saveLabels(listOf(label))

    override suspend fun deleteLabel(labelId: LabelId) {
        labelDao.deleteLabelById(labelId)
    }

    override suspend fun deleteAllLabels(userId: UserId) {
        labelDao.deleteAllLabels(userId)
    }
}
