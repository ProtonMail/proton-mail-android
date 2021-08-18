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

package ch.protonmail.android.data

import android.content.Context
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.data.local.model.LabelEntity
import ch.protonmail.android.domain.entity.LabelId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

internal class RoomLabelRepository @Inject constructor(
    private val context: Context,
    private val messageDatabaseFactory: MessageDatabase.Factory,
    private val messageDao: MessageDao
) : LabelRepository {

    override fun findLabels(userId: UserId, labelsIds: List<LabelId>): Flow<List<LabelEntity>> =
        getDao(userId).findLabelsById(labelsIds.map { it.id })

    override fun findAllLabels(userId: UserId): Flow<List<LabelEntity>> =
        getDao(userId).getAllLabels()

    override suspend fun saveLabel(userId: UserId, label: LabelEntity) {
        getDao(userId).saveLabel(label)
    }

    override suspend fun saveLabel(label: LabelEntity) {
        messageDao.saveLabel(label)
    }

    private fun getDao(userId: UserId): MessageDao =
        messageDatabaseFactory.getInstance(context, userId).getDao()
}
