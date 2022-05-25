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

package ch.protonmail.android.labels.domain.model

/**
 * Representation of a [Label] of [LabelType.FOLDER] or [LabelType.MESSAGE_LABEL], with its relative children
 */
sealed class LabelOrFolderWithChildren {

    abstract val id: LabelId
    abstract val name: String
    abstract val color: String

    data class Label(
        override val id: LabelId,
        override val name: String,
        override val color: String
    ) : LabelOrFolderWithChildren()

    data class Folder(
        override val id: LabelId,
        override val name: String,
        override val color: String,
        val path: String,
        val parentId: LabelId?,
        val children: Collection<Folder>
    ) : LabelOrFolderWithChildren()

}
