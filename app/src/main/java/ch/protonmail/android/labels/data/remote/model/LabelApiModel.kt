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

package ch.protonmail.android.labels.data.remote.model

import ch.protonmail.android.labels.domain.model.LabelType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val ID = "ID"
internal const val NAME = "Name"
internal const val PATH = "Path"
internal const val COLOR = "Color"
internal const val TYPE = "Type"
internal const val NOTIFY = "Notify"
internal const val ORDER = "Order"
internal const val EXPANDED = "Expanded"
internal const val STICKY = "Sticky"
internal const val PARENT_ID = "ParentID"

/**
 * Label model as received from the backend.
 *
 * @property name required, cannot be same as an existing label of this Type. Max length is 100 characters
 * @property path required, relative folder path e.g. "Folder/Event Label!",
 * @property color required, must match default colors
 * @property type required, 1 => Message Labels (default), 2 => Contact Groups, 3 => Message Folders
 * @property notify optional, 0 => no desktop/email notifications, 1 => notifications, folders only, default is 1 for folders
 * @property parentId optional, encrypted label id of parent folder, default is root level
 * @property expanded optional, 0 => collapse and hide sub-folders, 1 => expanded and show sub-folders
 * @property sticky optional, 0 => not sticky, 1 => stick to the page in the sidebar
 */
@Serializable
data class LabelApiModel(
    @SerialName(ID)
    val id: String,

    @SerialName(NAME)
    val name: String,

    @SerialName(PATH)
    val path: String,

    @SerialName(COLOR)
    val color: String,

    @SerialName(TYPE)
    val type: LabelType,

    @SerialName(NOTIFY)
    val notify: Int,

    @SerialName(ORDER)
    val order: Int?,

    @SerialName(PARENT_ID)
    val parentId: String? = null,

    @SerialName(EXPANDED)
    val expanded: Int?,

    @SerialName(STICKY)
    val sticky: Int?,
)
