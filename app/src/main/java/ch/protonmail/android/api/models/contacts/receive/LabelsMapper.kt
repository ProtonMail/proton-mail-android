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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.ContactLabelEntity
import ch.protonmail.android.data.local.model.LabelEntity
import me.proton.core.util.kotlin.toBooleanOrFalse
import me.proton.core.util.kotlin.toInt
import javax.inject.Inject

class LabelsMapper @Inject constructor() {

    fun mapLabelToLabelEntity(serverLabel: Label) =
        LabelEntity(
            id = serverLabel.id,
            name = serverLabel.name,
            color = serverLabel.color,
            display = serverLabel.display ?: 0,
            order = serverLabel.order ?: 0,
            exclusive = serverLabel.exclusive?.toBooleanOrFalse() ?: false,
            type = serverLabel.type ?: 0
        )

    fun mapLabelEntityToServerLabel(dbObject: ContactLabelEntity): Label {
        val id = dbObject.ID
        val name = dbObject.name
        val color = dbObject.color
        val display = dbObject.display
        val order = dbObject.order
        val exclusive = dbObject.exclusive.toInt()
        val type = Constants.LABEL_TYPE_CONTACT_GROUPS
        return Label(
            id = id,
            name = name,
            path = "",
            color = color,
            display = display,
            order = order,
            exclusive = exclusive,
            type = type,
            notify = 0,
            expanded = null,
            sticky = null
        )
    }

    fun mapLabelToContactLabelEntity(label: Label) = ContactLabelEntity(
        ID = label.id,
        name = label.name,
        color = label.color,
        display = label.display ?: 0,
        order = label.order ?: 0,
        exclusive = label.exclusive?.toBooleanOrFalse() ?: false,
        type = label.type ?: 0
    )

    fun mapContactLabelToRequestLabel(contactLabel: ContactLabelEntity): LabelRequestBody {
        return LabelRequestBody(
            name = contactLabel.name,
            color = contactLabel.color,
            type = contactLabel.type,
            parentId = null,
            notify = 0,
            exclusive = contactLabel.exclusive.toInt(),
            display = contactLabel.display
        )
    }
}
