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
import ch.protonmail.android.labels.domain.model.Label
import me.proton.core.domain.arch.Mapper
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class LabelEntityDomainMapper @Inject constructor() : Mapper<LabelEntity, Label> {

    fun toLabel(model: LabelEntity, contactEmailsCount: Int = 0) = Label(
        id = model.id,
        name = model.name,
        color = model.color,
        type = model.type,
        path = model.path,
        parentId = model.parentId,
        contactEmailsCount = contactEmailsCount
    )

    fun toEntity(model: Label, userId: UserId) = LabelEntity(
        id = model.id,
        userId = userId,
        name = model.name,
        color = model.color,
        order = 0,
        type = model.type,
        path = model.path,
        parentId = model.parentId,
        expanded = 0,
        sticky = 0,
        notify = 0
    )
}
