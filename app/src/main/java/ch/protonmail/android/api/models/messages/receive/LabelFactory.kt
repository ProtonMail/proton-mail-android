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
package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.api.models.factories.IConverterFactory
import ch.protonmail.android.api.models.factories.parseBoolean
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.utils.extensions.notNull
import ch.protonmail.android.utils.extensions.notNullOrEmpty

// TODO: Maybe merge with [ContactLabelFactory]
class LabelFactory : IConverterFactory<ServerLabel, Label> {

    override fun createServerObjectFromDBObject(dbObject: Label): ServerLabel {
        TODO("not implemented") // To change body of created functions use File | Settings | File Templates.
    }

    override fun createDBObjectFromServerObject(serverLabel: ServerLabel): Label {
        val id = serverLabel.ID.notNullOrEmpty("ID")
        val name = serverLabel.name.notNullOrEmpty("name")
        val color = serverLabel.color.notNullOrEmpty("color")
        val display = serverLabel.display.notNull("display")
        val order = serverLabel.order.notNull("order")
        val exclusive = serverLabel.exclusive.notNull("exclusive").parseBoolean("exclusive")
        val type = serverLabel.exclusive.notNull("type")
        return Label(
            id = id,
            name = name,
            color = color,
            display = display,
            order = order,
            exclusive = exclusive,
            type = type
        )
    }

    fun createDBObjectFromServerLabelObject(serverLabel: ch.protonmail.android.api.models.messages.receive.Label): Label {
        val id = serverLabel.id.notNullOrEmpty("ID")
        val name = serverLabel.name.notNullOrEmpty("name")
        val color = serverLabel.color.notNullOrEmpty("color")
        val display = serverLabel.display.notNull("display")
        val order = serverLabel.order.notNull("order")
        val exclusive = serverLabel.exclusive.notNull("exclusive").parseBoolean("exclusive")
        val type = serverLabel.exclusive.notNull("type")
        return Label(
            id = id,
            name = name,
            color = color,
            display = display,
            order = order,
            exclusive = exclusive,
            type = type
        )
    }
}
