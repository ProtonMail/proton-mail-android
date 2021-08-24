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

import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelId
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId

interface LabelRepository {

    fun observeLabels(userId: UserId, labelsIds: List<LabelId>): Flow<List<LabelEntity>>

    suspend fun findLabels(userId: UserId, labelsIds: List<LabelId>): List<LabelEntity>

    fun observeAllLabels(userId: UserId): Flow<List<LabelEntity>>

    suspend fun findAllLabels(userId: UserId): List<LabelEntity>

    suspend fun saveLabel(userId: UserId, label: LabelEntity)

    @Deprecated("Save with userId", ReplaceWith("saveLabel(userId, label)"))
    suspend fun saveLabel(label: LabelEntity)

}
