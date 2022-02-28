/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.labels.data.remote.model

import com.google.gson.annotations.SerializedName

private const val ID = "ID"
private const val NAME = "Name"
private const val PATH = "Path"
private const val COLOR = "Color"
private const val TYPE = "Type"
private const val NOTIFY = "Notify"
private const val ORDER = "Order"
private const val EXPANDED = "Expanded"
private const val STICKY = "Sticky"
private const val PARENT_ID = "ParentID"

/**
 * Label model as received from the backend in the Events system.
 *
 * @property name required, cannot be same as an existing label of this Type. Max length is 100 characters
 * @property path required, relative folder path e.g. "Folder/Event Label!",
 * @property color required, must match default colors
 * @property type required, 1 => Message Labels (default), 2 => Contact Groups, 3 => Message Folders
 * @property notify optional, 0 => no desktop/email notifications, 1 => notifications, folders only, default is 1 for
 *  folders
 * @property parentId optional, encrypted label id of parent folder, default is root level
 * @property expanded optional, 0 => collapse and hide sub-folders, 1 => expanded and show sub-folders
 * @property sticky optional, 0 => not sticky, 1 => stick to the page in the sidebar
 */
data class LabelEventModel(
    @SerializedName(ID)
    val id: String,

    @SerializedName(NAME)
    val name: String,

    @SerializedName(PATH)
    val path: String,

    @SerializedName(TYPE)
    val type: Int,

    @SerializedName(COLOR)
    val color: String,

    @SerializedName(ORDER)
    val order: Int?,

    @SerializedName(NOTIFY)
    val notify: Int?,

    @SerializedName(EXPANDED)
    val expanded: Int?,

    @SerializedName(STICKY)
    val sticky: Int?,

    @SerializedName(PARENT_ID)
    val parentId: String? = null
)
