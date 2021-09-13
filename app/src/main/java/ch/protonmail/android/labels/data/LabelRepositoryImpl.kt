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
import ch.protonmail.android.labels.data.local.LabelDao
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.data.mapper.LabelEntityApiMapper
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
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
    private val labelApiMapper: LabelEntityApiMapper,
    private val labelDomainMapper: LabelEntityDomainMapper,
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

    override fun observeLabels(labelsIds: List<LabelId>, userId: UserId): Flow<List<LabelEntity>> =
        labelDao.observeLabelsById(userId, labelsIds)

    override suspend fun findLabels(labelsIds: List<LabelId>, userId: UserId): List<LabelEntity> =
        observeLabels(labelsIds, userId).first()

    override suspend fun findLabel(labelId: LabelId): LabelEntity? =
        labelDao.findLabelById(labelId)

    override fun observeLabel(labelId: LabelId): Flow<LabelEntity?> =
        labelDao.observeLabelById(labelId)

    override fun findLabelBlocking(labelId: LabelId): LabelEntity? {
        return runBlocking {
            findLabel(labelId)
        }
    }

    override fun observeContactGroups(userId: UserId): Flow<List<LabelEntity>> =
        labelDao.observeLabelsByType(userId, LabelType.CONTACT_GROUP)

    override suspend fun findContactGroups(userId: UserId): List<LabelEntity> =
        labelDao.findLabelsByType(userId, LabelType.CONTACT_GROUP)

    override fun observeSearchContactGroups(labelName: String, userId: UserId): Flow<List<LabelEntity>> =
        labelDao.observeSearchLabelsByNameAndType(userId, labelName, LabelType.CONTACT_GROUP)

    override suspend fun findLabelByName(labelName: String, userId: UserId): LabelEntity? =
        labelDao.findLabelByName(userId, labelName)

    override fun findAllLabelsPaged(userId: UserId): DataSource.Factory<Int, LabelEntity> =
        labelDao.findAllLabelsPaged(userId, LabelType.MESSAGE_LABEL)

    override fun findAllFoldersPaged(userId: UserId): DataSource.Factory<Int, LabelEntity> =
        labelDao.findAllLabelsPaged(userId, LabelType.FOLDER)

    override suspend fun saveLabels(labels: List<Label>, userId: UserId) {
        saveLabels(
            labels.map {
                labelDomainMapper.toEntity(it, userId)
            }
        )
    }

    private suspend fun saveLabels(labels: List<LabelEntity>) {
        Timber.v("Save labels: ${labels.map { it.id.id }}")
        labelDao.insertOrUpdate(*labels.toTypedArray())
    }

    override suspend fun saveLabel(label: Label, userId: UserId) {
        saveLabels(
            listOf(labelDomainMapper.toEntity(label, userId))
        )
    }

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
        val serverLabels = async { api.getLabels(userId).valueOrThrow.labels }
        val serverFolders = async { api.getFolders(userId).valueOrThrow.labels }
        val serverContactGroups = async { api.getContactGroups(userId).valueOrThrow.labels }
        val allLabels = serverLabels.await() + serverFolders.await() + serverContactGroups.await()
        val allLabelsEntities = allLabels.map { labelApiMapper.toEntity(it, userId) }
        Timber.v("fetchAndSaveAllLabels size: ${allLabelsEntities.size} user: $userId")
        saveLabels(allLabelsEntities)
        allLabelsEntities
    }
}
