/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.labels.data.mapper

import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelEventModel
import ch.protonmail.android.labels.domain.model.LabelType
import me.proton.core.domain.arch.Mapper
import javax.inject.Inject

class LabelEventApiMapper @Inject constructor() : Mapper<LabelEventModel, LabelApiModel> {

    fun toApiModel(eventModel: LabelEventModel): LabelApiModel = LabelApiModel(
        id = eventModel.id,
        name = eventModel.name,
        path = eventModel.path,
        type = requireNotNull(LabelType.fromIntOrNull(eventModel.type)),
        color = eventModel.color,
        order = eventModel.order,
        notify = eventModel.notify,
        expanded = eventModel.expanded,
        sticky = eventModel.sticky,
        parentId = eventModel.parentId
    )
}
