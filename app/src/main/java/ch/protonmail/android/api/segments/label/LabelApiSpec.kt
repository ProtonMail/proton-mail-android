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
package ch.protonmail.android.api.segments.label

import ch.protonmail.android.api.models.contacts.receive.ContactGroupsResponse
import ch.protonmail.android.api.models.messages.receive.LabelRequestBody
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.api.models.messages.receive.LabelsResponse
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult

interface LabelApiSpec {

    suspend fun fetchLabels(userId: UserId): ApiResult<LabelsResponse>

    suspend fun fetchContactGroups(): ApiResult<ContactGroupsResponse>

    suspend fun createLabel(label: LabelRequestBody): ApiResult<LabelResponse>

    suspend fun updateLabel(labelId: String, label: LabelRequestBody): ApiResult<LabelResponse>

    suspend fun deleteLabel(labelId: String): ApiResult<Unit>
}
