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

import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.data.local.model.LabelId
import ch.protonmail.android.labels.data.remote.model.LabelApiModel
import ch.protonmail.android.labels.data.remote.model.LabelRequestBody
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class LabelsMapper @Inject constructor() {

    fun mapLabelToLabelEntity(serverLabel: LabelApiModel, userId: UserId) = LabelEntity(
        id = LabelId(serverLabel.id),
        userId = userId,
        name = serverLabel.name,
        color = serverLabel.color,
        order = serverLabel.order ?: 0,
        type = serverLabel.type,
        path = serverLabel.path,
        parentId = serverLabel.parentId ?: EMPTY_STRING,
        expanded = serverLabel.expanded ?: 0,
        sticky = serverLabel.sticky ?: 0,
        notify = serverLabel.notify
    )

    fun mapLabelEntityToServerLabel(labelEntity: LabelEntity) = LabelApiModel(
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

    fun mapLabelEntityToRequestLabel(labelEntity: LabelEntity) = LabelRequestBody(
        name = labelEntity.name,
        color = labelEntity.color,
        type = labelEntity.type.typeInt,
        parentId = if (labelEntity.parentId.isBlank()) null else labelEntity.parentId,
        notify = null,
        expanded = null,
        sticky = null
    )

    fun mapLabelToContactLabelUiModel(serverLabel: LabelApiModel, contactEmailsCount: Int) = ContactLabelUiModel(
        id = LabelId(serverLabel.id),
        name = serverLabel.name,
        color = serverLabel.color,
        type = serverLabel.type,
        path = serverLabel.path,
        parentId = serverLabel.parentId ?: EMPTY_STRING,
        expanded = serverLabel.expanded ?: 0,
        sticky = serverLabel.sticky ?: 0,
        contactEmailsCount = contactEmailsCount
    )

    fun mapLabelEntityToContactLabelUiModel(entity: LabelEntity, contactEmailsCount: Int) =
        ContactLabelUiModel(
            id = entity.id,
            name = entity.name,
            color = entity.color,
            type = entity.type,
            path = entity.path,
            parentId = entity.parentId,
            expanded = entity.expanded,
            sticky = entity.sticky,
            contactEmailsCount = contactEmailsCount
        )
}
