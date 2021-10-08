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

package ch.protonmail.android.labels.data.mapper

import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.domain.model.LabelId
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class LabelEntityApiMapper @Inject constructor() : Mapper<LabelApiModel, LabelEntity> {

    fun toEntity(model: LabelApiModel, userId: UserId) = LabelEntity(
        id = LabelId(model.id),
        userId = userId,
        name = model.name,
        color = model.color,
        order = model.order ?: 0,
        type = model.type,
        path = model.path,
        parentId = model.parentId ?: EMPTY_STRING,
        expanded = model.expanded ?: 0,
        sticky = model.sticky ?: 0,
        notify = model.notify ?: 0
    )

    fun toApiModel(labelEntity: LabelEntity) = LabelApiModel(
        id = labelEntity.id.id,
        name = labelEntity.name,
        path = labelEntity.path,
        color = labelEntity.color,
        order = labelEntity.order,
        type = labelEntity.type,
        notify = labelEntity.notify,
        parentId = labelEntity.parentId,
        expanded = labelEntity.expanded,
        sticky = labelEntity.sticky,
    )
}
