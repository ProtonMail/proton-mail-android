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

package ch.protonmail.android.labels.domain.usecase

import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren.Folder
import ch.protonmail.android.labels.domain.model.LabelType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Observe a [List] of all the Folder ( [Folder] ) eligible to be a parent, respecting the product rule, to allow max
 *  3 levels ( child, parent, grand-parent )
 */
class ObserveFoldersEligibleAsParent @Inject constructor(
    private val labelRepository: LabelRepository
) {

    operator fun invoke(userId: UserId): Flow<List<Folder>> =
        labelRepository.observeAllLabelsOrFoldersWithChildren(userId, LabelType.FOLDER)
            .map(::removeThirdLevelChildren)

    private fun removeThirdLevelChildren(list: List<LabelOrFolderWithChildren>): List<Folder> =
        list.filterIsInstance<Folder>().map { firstLevelFolder ->
            val secondLevelFoldersWithoutChildren = firstLevelFolder.children.map { secondLevelFolder ->
                secondLevelFolder.copy(children = emptyList())
            }
            firstLevelFolder.copy(children = secondLevelFoldersWithoutChildren)
        }

}
