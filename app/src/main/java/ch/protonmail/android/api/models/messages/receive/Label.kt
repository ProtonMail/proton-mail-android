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

    //required, cannot be same as an existing label of this Type. Max length is 100 characters
    @SerialName(Fields.Label.NAME)
    val name: String,

    @SerialName(Fields.Label.PATH)
    val path: String,

    // required, must match default colors
    @SerialName(Fields.Label.COLOR)
    val color: String,

    // required, 1 => Message Labels (default), 2 => Contact Groups, 3 => Message Folders
    @SerialName(Fields.Label.TYPE)
    val type: Int,

    // optional, 0 => no desktop/email notifications, 1 => notifications, folders only, default is 1 for folders
    @SerialName(Fields.Label.NOTIFY)
    val notify: Int,

    @SerialName(Fields.Label.ORDER)
    val order: Int?,

    // optional, encrypted label id of parent folder, default is root level
    @SerialName(Fields.Label.PARENT_ID)
    val parentId: String? = null,

    // v4 optional, 0 => collapse and hide sub-folders, 1 => expanded and show sub-folders
    @SerialName(Fields.Label.EXPANDED)
    val expanded: Int?,

    // v4 optional, 0 => not sticky, 1 => stick to the page in the sidebar
    @SerialName(Fields.Label.STICKY)
    val sticky: Int?,
)
