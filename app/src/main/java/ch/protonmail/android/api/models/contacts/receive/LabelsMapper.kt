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
package ch.protonmail.android.api.models.contacts.receive

import ch.protonmail.android.api.models.messages.receive.Label
import ch.protonmail.android.api.models.messages.receive.LabelRequestBody
import ch.protonmail.android.data.local.model.ContactLabelEntity
import ch.protonmail.android.data.local.model.LabelEntity
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class LabelsMapper @Inject constructor() {

    fun mapLabelToLabelEntity(serverLabel: Label) =
        LabelEntity(
            id = serverLabel.id,
            name = serverLabel.name,
            color = serverLabel.color,
            order = serverLabel.order ?: 0,
            type = serverLabel.type,
            path = serverLabel.path,
            parentId = serverLabel.parentId ?: EMPTY_STRING,
            expanded = serverLabel.expanded ?: 0,
            sticky = serverLabel.sticky ?: 0,
        )

    fun mapLabelEntityToServerLabel(dbObject: ContactLabelEntity): Label {
        val id = dbObject.id
        val name = dbObject.name
        val color = dbObject.color
        val order = dbObject.order
        val type = dbObject.type
        val path = dbObject.path
        val parentId = dbObject.parentId
        val expanded = dbObject.expanded
        val sticky = dbObject.sticky
        val notify = dbObject.notify
        return Label(
            id = id,
            name = name,
            path = path,
            color = color,
            order = order,
            type = type,
            notify = notify,
            parentId = parentId,
            expanded = expanded,
            sticky = sticky,
        )
    }

    fun mapLabelToContactLabelEntity(label: Label) = ContactLabelEntity(
        id = label.id,
        name = label.name,
        color = label.color,
        order = label.order ?: 0,
        type = label.type,
        path = label.path,
        parentId = label.parentId ?: EMPTY_STRING,
        expanded = label.expanded ?: 0,
        sticky = label.sticky ?: 0,
        notify = label.notify
    )

    fun mapContactLabelToRequestLabel(contactLabel: ContactLabelEntity): LabelRequestBody {
        return LabelRequestBody(
            name = contactLabel.name,
            color = contactLabel.color,
            type = contactLabel.type,
            parentId = contactLabel.parentId,
            notify = 0,
            expanded = contactLabel.expanded,
            sticky = contactLabel.sticky
        )
    }
}
