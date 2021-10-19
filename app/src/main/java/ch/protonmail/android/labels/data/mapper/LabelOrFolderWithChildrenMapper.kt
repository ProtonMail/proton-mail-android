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

package ch.protonmail.android.labels.data.mapper

import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
import kotlinx.coroutines.withContext
import me.proton.core.domain.arch.Mapper
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import javax.inject.Inject

class LabelOrFolderWithChildrenMapper @Inject constructor(
    private val dispatchers: DispatcherProvider
) : Mapper<Collection<LabelEntity>, Collection<LabelOrFolderWithChildren>> {

    suspend fun toLabelsAndFoldersWithChildren(labels: Collection<LabelEntity>): List<LabelOrFolderWithChildren> {
        return withContext(dispatchers.Comp) {
            val mutableList = labels
                .toMutableList()
            toLabels(mutableList) + toFoldersWithChildren(mutableList, parentId = EMPTY_STRING)
        }
    }

    private fun toLabels(mutableList: MutableList<LabelEntity>): List<LabelOrFolderWithChildren.Label> {
        val messageLabels = mutableList.pullIf { it.type == LabelType.MESSAGE_LABEL }
        return messageLabels.map { labelEntity ->
            LabelOrFolderWithChildren.Label(
                id = labelEntity.id,
                name = labelEntity.name,
                color = labelEntity.color
            )
        }
    }

    private fun toFoldersWithChildren(
        mutableList: MutableList<LabelEntity>,
        parentId: String
    ): List<LabelOrFolderWithChildren.Folder> {
        val rootFolders = mutableList.pullIf { it.parentId == parentId }
        return rootFolders.map { label ->
            val children = toFoldersWithChildren(mutableList, label.id.id)
            toFolderWithoutChildren(label).copy(children = children)
        }
    }

    private fun toFolderWithoutChildren(label: LabelEntity) = LabelOrFolderWithChildren.Folder(
        id = label.id,
        name = label.name,
        color = label.color,
        path = label.path,
        parentId = LabelId(label.parentId),
        children = emptyList()
    )

    private fun <T> MutableList<T>.pullIf(predicate: (T) -> Boolean): List<T> {
        val toPull = this.filter(predicate)
        this.removeAll(toPull)
        return toPull
    }
}
