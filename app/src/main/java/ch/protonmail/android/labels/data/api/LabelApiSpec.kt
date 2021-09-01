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
package ch.protonmail.android.labels.data.api

import ch.protonmail.android.labels.data.model.LabelRequestBody
import ch.protonmail.android.labels.data.model.LabelResponse
import ch.protonmail.android.labels.data.model.LabelsResponse
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult

interface LabelApiSpec {

    suspend fun fetchLabels(userId: UserId): ApiResult<LabelsResponse>

    suspend fun fetchContactGroups(userId: UserId): ApiResult<LabelsResponse>

    suspend fun fetchFolders(userId: UserId): ApiResult<LabelsResponse>

    suspend fun createLabel(userId: UserId, label: LabelRequestBody): ApiResult<LabelResponse>

    suspend fun updateLabel(userId: UserId, labelId: String, labelRequestBody: LabelRequestBody):
        ApiResult<LabelResponse>

    suspend fun deleteLabel(userId: UserId, labelId: String): ApiResult<Unit>
}
