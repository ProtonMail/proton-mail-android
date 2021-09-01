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
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.labels.data.db.LabelDao
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.mapper.LabelsMapper
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.labels.data.model.LabelType
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

internal class LabelRepositoryImpl @Inject constructor(
    private val labelDao: LabelDao,
    private val api: ProtonMailApi,
    private val labelMapper: LabelsMapper,
    private val networkConnectivityManager: NetworkConnectivityManager
) : LabelRepository {

    override fun observeAllLabels(userId: UserId, shallRefresh: Boolean): Flow<List<LabelEntity>> =
        labelDao.observeAllLabels(userId)
            .onStart {
                if (shallRefresh && networkConnectivityManager.isInternetConnectionPossible()) {
                    Timber.v("Fetching fresh labels")
                    fetchAndSaveAllLabels(userId)
                }
            }
            .onEach {
                Timber.v("Emitting new labels size: ${it.size} user: $userId")
            }

    override suspend fun findAllLabels(userId: UserId, shallRefresh: Boolean): List<LabelEntity> =
        observeAllLabels(userId, shallRefresh).first()

    override fun observeLabels(userId: UserId, labelsIds: List<LabelId>): Flow<List<LabelEntity>> =
        labelDao.observeLabelsById(userId, labelsIds)

    override suspend fun findLabels(userId: UserId, labelsIds: List<LabelId>): List<LabelEntity> =
        observeLabels(userId, labelsIds).first()

    override suspend fun findLabel(labelId: LabelId): LabelEntity? =
        labelDao.findLabelById(labelId)

    override fun findLabelBlocking(labelId: LabelId): LabelEntity? {
        return runBlocking {
            findLabel(labelId)
        }
    }

    override fun observeContactGroups(userId: UserId): Flow<List<LabelEntity>> =
        labelDao.observeLabelsByType(userId, LabelType.CONTACT_GROUP)

    override suspend fun findContactGroups(userId: UserId): List<LabelEntity> =
        labelDao.findLabelsByType(userId, LabelType.CONTACT_GROUP)

    override fun observeSearchContactGroups(userId: UserId, labelName: String): Flow<List<LabelEntity>> =
        labelDao.observeSearchLabelsByNameAndType(userId, labelName, LabelType.CONTACT_GROUP)

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
        Timber.v("fetchAndSaveAllLabels size: ${allLabelsEntities.size} user: $userId")
        saveLabels(allLabelsEntities)
        allLabelsEntities
    }
}
