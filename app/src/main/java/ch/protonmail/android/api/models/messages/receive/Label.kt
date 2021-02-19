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

import ch.protonmail.android.api.utils.Fields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Label(
    @SerialName(Fields.Label.ID)
    val id: String,
    @SerialName(Fields.Label.NAME)
    val name: String,
    @SerialName(Fields.Label.PATH)
    val path: String,
    @SerialName(Fields.Label.COLOR)
    val color: String,
    @SerialName(Fields.Label.TYPE)
    val type: Int,
    @SerialName(Fields.Label.NOTIFY)
    val notify: Int,
    @SerialName(Fields.Label.ORDER)
    val order: Int,
    @SerialName(Fields.Label.EXPANDED) // v4
    val expanded: Int,
    @SerialName(Fields.Label.STICKY) // v4
    val sticky: Int,
    @SerialName(Fields.Label.DISPLAY) // v3
    val display: Int,
    @SerialName(Fields.Label.EXCLUSIVE) // v3
    val exclusive: Int
)
