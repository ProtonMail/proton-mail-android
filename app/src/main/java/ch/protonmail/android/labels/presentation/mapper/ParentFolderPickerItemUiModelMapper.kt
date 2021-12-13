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

package ch.protonmail.android.labels.presentation.mapper

import android.content.Context
import androidx.core.graphics.toColorInt
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren.Folder
import ch.protonmail.android.labels.presentation.model.LabelIcon
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerItemUiModel
import me.proton.core.domain.arch.Mapper
import timber.log.Timber
import javax.inject.Inject

/**
 * A Mapper of [ParentFolderPickerItemUiModel]
 *
 * @property useFolderColor whether the user enabled the settings for use Colors for Folders.
 *  TODO to be implemented in MAILAND-1818, ideally inject its use case. Currently defaults to `true`
 */
class ParentFolderPickerItemUiModelMapper @Inject constructor(
    context: Context
) : Mapper<Collection<Folder>, List<ParentFolderPickerItemUiModel>> {

    private val useFolderColor: Boolean = true

    private val defaultIconColor = context.getColor(R.color.icon_norm)

    /**
     * @param currentFolder the [LabelId] of the folder which we're picking a parent for
     * @param selectedParentFolder the [LabelId] of the folder that is currently selected as a parent
     * @param includeNoneUiModel whether [ParentFolderPickerItemUiModel.None] should be in the list ( at the first
     *  position )
     */
    fun toUiModels(
        folders: Collection<Folder>,
        currentFolder: LabelId?,
        selectedParentFolder: LabelId?,
        includeNoneUiModel: Boolean
    ): List<ParentFolderPickerItemUiModel> {
        val noneUiModel = if (includeNoneUiModel) {
            listOf(ParentFolderPickerItemUiModel.None(isSelected = selectedParentFolder == null))
        } else {
            emptyList()
        }

        return noneUiModel + folders.flatMap { label ->
            labelToUiModels(
                folder = label,
                currentFolder = currentFolder,
                isEnabled = label.id != currentFolder,
                selectedParentFolder = selectedParentFolder,
                folderLevel = 0,
                parentColor = null
            )
        }
    }

    private fun labelToUiModels(
        folder: Folder,
        currentFolder: LabelId?,
        isEnabled: Boolean,
        selectedParentFolder: LabelId?,
        folderLevel: Int,
        parentColor: Int?
    ): List<ParentFolderPickerItemUiModel.Folder> {

        val parent = ParentFolderPickerItemUiModel.Folder(
            id = folder.id,
            name = folder.name,
            icon = buildIcon(folder, parentColor),
            folderLevel = folderLevel,
            isSelected = folder.id == selectedParentFolder,
            isEnabled = isEnabled && folder.id != currentFolder,
        )
        val children = folder.children.flatMap {
            labelToUiModels(
                folder = it,
                currentFolder = currentFolder,
                isEnabled = parent.isEnabled,
                selectedParentFolder = selectedParentFolder,
                folderLevel = folderLevel + 1,
                parentColor = parent.icon.colorInt
            )
        }
        return listOf(parent) + children
    }

    private fun buildIcon(
        folder: Folder,
        parentColor: Int?
    ): LabelIcon.Folder {
        val folderColorInt = folder.color.toColorIntOrNull() ?: parentColor ?: defaultIconColor
        return if (folder.children.isNotEmpty()) {
            if (useFolderColor) {
                LabelIcon.Folder.WithChildren.Colored(folderColorInt)
            } else {
                LabelIcon.Folder.WithChildren.BlackWhite
            }
        } else {
            if (useFolderColor) {
                LabelIcon.Folder.WithoutChildren.Colored(folderColorInt)
            } else {
                LabelIcon.Folder.WithoutChildren.BlackWhite
            }
        }
    }

    private fun String.toColorIntOrNull(): Int? = try {
        toColorInt()
    } catch (exc: IllegalArgumentException) {
        Timber.e(exc, "Unknown label color: $this")
        null
    }
}
