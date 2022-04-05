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

package ch.protonmail.android.labels.utils

import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren.Folder
import me.proton.core.util.kotlin.EMPTY_STRING

/**
 * Create an hierarchy of [LabelOrFolderWithChildren.Folder] in DSL style
 * Example
 * ```kotlin
 * buildFolders {
 *     +"first"
 *     +"second" {
 *         +"child" {
 *             +"grandchild"
 *             +"second grandchild"
 *         }
 *         +"another child"
 *     }
 * }
 * ```
 */
fun buildFolders(block: FolderBuilder.() -> Unit): List<Folder> {
    val folderBuilder = FolderBuilder(EMPTY_STRING)
    folderBuilder.block()
    return folderBuilder.result
}

class FolderBuilder(private val parent: String) {

    val result: MutableList<Folder> = mutableListOf()

    fun folder(name: String, block: FolderBuilder.() -> Unit = {}) {
        val folderBuilder = FolderBuilder(name)
        folderBuilder.block()
        result += buildFolder(name, parent, folderBuilder.result)
    }
}

private fun buildFolder(
    name: String,
    parent: String,
    children: Collection<Folder>
) = Folder(
    id = LabelId(name),
    name = name,
    color = EMPTY_STRING,
    path = EMPTY_STRING,
    parentId = LabelId(parent),
    children = children
)