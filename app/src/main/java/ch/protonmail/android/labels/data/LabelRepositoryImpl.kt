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
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.labels.data.db.LabelDao
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.mapper.LabelsMapper
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.data.model.LabelType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
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
            .onStart {
                Timber.v("Fetching fresh labels")
                fetchAndSaveAllLabels(userId)
            }

    override suspend fun findAllLabels(userId: UserId): List<LabelEntity> {
        val dbData = labelDao.findAllLabels(userId)
        return if (dbData.isEmpty()) {
            Timber.v("Fetching fresh labels")
            fetchAndSaveAllLabels(userId)
        } else {
            dbData
        }
    }

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

    override fun observeContactGroups(userId: UserId): Flow<List<LabelEntity>> =
        labelDao.observeLabelsByType(userId, LabelType.CONTACT_GROUP.typeInt)

    override fun observeSimilarContactGroups(userId: UserId, labelName: String): Flow<List<LabelEntity>> =
        labelDao.observeSimilarLabelsByNameAndType(userId, labelName, LabelType.CONTACT_GROUP.typeInt)

    override suspend fun findContactGroups(userId: UserId): List<LabelEntity> =
        labelDao.findLabelsByType(userId, LabelType.CONTACT_GROUP.typeInt)

    override suspend fun findLabelByName(userId: UserId, labelName: String): LabelEntity? =
        labelDao.findLabelByName(userId, labelName)

    override fun findAllLabelsPaged(userId: UserId): DataSource.Factory<Int, LabelEntity> =
        labelDao.findAllMessageLabelsPaged(userId)

    override fun findAllFoldersPaged(userId: UserId): DataSource.Factory<Int, LabelEntity> =
        labelDao.findAllFoldersPaged(userId)

    override suspend fun saveLabels(labels: List<LabelEntity>) {
        Timber.v("Save labels: ${labels.map { it.id.id }}")
        labelDao.insertOrUpdate(*labels.toTypedArray())
    }

    override suspend fun saveLabel(label: LabelEntity) =
        saveLabels(listOf(label))

    override suspend fun deleteLabel(labelId: LabelId) {
        labelDao.deleteLabelById(labelId)
    }

    override suspend fun deleteAllLabels(userId: UserId) {
        labelDao.deleteAllLabels(userId)
    }

    override suspend fun deleteContactGroups(userId: UserId) {
        labelDao.deleteContactGroups(userId)
    }

    private suspend fun fetchAndSaveAllLabels(
        userId: UserId
    ): List<LabelEntity> = coroutineScope {
        val serverLabels = async { api.fetchLabels(userId).valueOrThrow.labels }
        val serverFolders = async { api.fetchFolders(userId).valueOrThrow.labels }
        val serverContactGroups = async { api.fetchContactGroups(userId).valueOrThrow.labels }
        val allLabels = serverLabels.await() + serverFolders.await() + serverContactGroups.await()
        val allLabelsEntities = allLabels.map { labelMapper.mapLabelToLabelEntity(it, userId) }
        saveLabels(allLabelsEntities)
        allLabelsEntities
    }
}
