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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val NAME = "Name"
private const val COLOR = "Color"
private const val TYPE = "Type"
private const val NOTIFY = "Notify"
private const val EXPANDED = "Expanded"
private const val STICKY = "Sticky"
private const val PARENT_ID = "ParentID"

@Serializable
data class LabelRequestBody(
    @SerialName(NAME)
    val name: String,
    @SerialName(COLOR)
    val color: String,
    @SerialName(TYPE)
    val type: Int? = null, // only '1', '2', or '3
    @SerialName(PARENT_ID)
    val parentId: String? = null,
    @SerialName(NOTIFY)
    val notify: Int? = null,
    @SerialName(EXPANDED) // v4
    val expanded: Int? = null,
    @SerialName(STICKY) // v4
    val sticky: Int? = null
)
