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
import ch.protonmail.android.api.models.factories.makeInt
import ch.protonmail.android.api.models.factories.parseBoolean
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.utils.Fields
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.extensions.notNull
import ch.protonmail.android.utils.extensions.notNullOrEmpty
import javax.inject.Inject

class ContactLabelFactory @Inject constructor() : IConverterFactory<ServerLabel, ContactLabel> {

    override fun createServerObjectFromDBObject(dbObject: ContactLabel): ServerLabel {
        val id = dbObject.ID
        val name = dbObject.name.notNullOrEmpty(Fields.Label.NAME)
        val color = dbObject.color.notNullOrEmpty(Fields.Label.COLOR)
        val display = dbObject.display.notNull(Fields.Label.DISPLAY)
        val order = dbObject.order.notNull(Fields.Label.ORDER)
        val exclusive = dbObject.exclusive.notNull(Fields.Label.EXCLUSIVE).makeInt()
        val type = Constants.LABEL_TYPE_CONTACT_GROUPS
        return ServerLabel(
            ID = id,
            name = name,
            color = color,
            display = display,
            order = order,
            exclusive = exclusive,
            type = type)
    }

    override fun createDBObjectFromServerObject(serverObject: ServerLabel): ContactLabel {
        val id = serverObject.ID.notNullOrEmpty("ID")
        val name = serverObject.name.notNullOrEmpty(Fields.Label.NAME)
        val color = serverObject.color.notNullOrEmpty(Fields.Label.COLOR)
        val display = serverObject.display.notNull(Fields.Label.DISPLAY)
        val order = serverObject.order.notNull(Fields.Label.ORDER)
        val exclusive = serverObject.exclusive.notNull(Fields.Label.EXCLUSIVE).parseBoolean(Fields.Label.EXCLUSIVE)
        val type = serverObject.type.notNull(Fields.Label.TYPE)
        return ContactLabel(
                ID = id,
                name = name,
                color = color,
                display = display,
                order = order,
                exclusive = exclusive,
                type = type)
    }

}
