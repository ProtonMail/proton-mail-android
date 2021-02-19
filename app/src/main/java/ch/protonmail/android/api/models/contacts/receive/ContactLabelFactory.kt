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

import ch.protonmail.android.api.models.factories.IConverterFactory
import ch.protonmail.android.api.models.factories.parseBoolean
import ch.protonmail.android.api.models.messages.receive.Label
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.api.utils.Fields
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.ContactLabel
import ch.protonmail.android.domain.util.requireNotBlank
import me.proton.core.util.kotlin.toInt

// TODO: Maybe merge with [LabelFactory]
class ContactLabelFactory : IConverterFactory<ServerLabel, ContactLabel> {

    override fun createServerObjectFromDBObject(dbObject: ContactLabel) = ServerLabel(
        ID = dbObject.ID,
        name = requireNotBlank(dbObject.name) { "name is empty" },
        color = requireNotBlank(dbObject.color) { "color is empty" },
        display = dbObject.display,
        order = dbObject.order,
        exclusive = dbObject.exclusive.toInt(),
        type = Constants.LABEL_TYPE_CONTACT_GROUPS
    )

    override fun createDBObjectFromServerObject(serverObject: ServerLabel) = ContactLabel(
        ID = requireNotBlank(serverObject.ID) { "id is empty" },
        name = requireNotBlank(serverObject.name) { "name is empty" },
        color = requireNotBlank(serverObject.color) { "color is empty" },
        display = requireNotNull(serverObject.display) { "display is null" },
        order = requireNotNull(serverObject.order) { "order is null" },
        exclusive = requireNotNull(serverObject.exclusive) { "exclusive is null" }
            .parseBoolean(Fields.Label.EXCLUSIVE),
        type = requireNotNull(serverObject.type) { "type is null" }
    )

    fun createDBObjectFromServerLabelObject(serverObject: Label) = ContactLabel(
        ID = requireNotBlank(serverObject.id) { "id is empty" },
        name = requireNotBlank(serverObject.name) { "name is empty" },
        color = requireNotBlank(serverObject.color) { "color is empty" },
        display = requireNotNull(serverObject.display) { "display is null" },
        order = requireNotNull(serverObject.order) { "order is null" },
        exclusive = requireNotNull(serverObject.exclusive) { "exclusive is null" }
            .parseBoolean(Fields.Label.EXCLUSIVE),
        type = requireNotNull(serverObject.type) { "type is null" }
    )

}
