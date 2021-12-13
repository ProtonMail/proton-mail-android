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
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.presentation.model.LabelIcon
import ch.protonmail.android.labels.presentation.model.LabelsManagerItemUiModel
import me.proton.core.domain.arch.Mapper
import timber.log.Timber
import javax.inject.Inject

/**
 * A Mapper of [LabelsManagerItemUiModel]
 *
 * @property useFolderColor whether the user enabled the settings for use Colors for Folders.
 *  TODO to be implemented in MAILAND-1818, ideally inject its use case. Currently defaults to `true`
 */
class LabelsManagerItemUiModelMapper @Inject constructor(
    context: Context
) : Mapper<Collection<LabelOrFolderWithChildren>, List<LabelsManagerItemUiModel>> {

    private val useFolderColor: Boolean = true

    private val defaultIconColor = context.getColor(R.color.icon_norm)

    fun toUiModels(
        labels: Collection<LabelOrFolderWithChildren>,
        checkedLabels: Collection<LabelId>
    ): List<LabelsManagerItemUiModel> =
        labels.flatMap { labelOrFolder ->
            labelOrFolderToUiModels(labelOrFolder, checkedLabels)
        }

    private fun labelOrFolderToUiModels(
        label: LabelOrFolderWithChildren,
        checkedLabels: Collection<LabelId>
    ): List<LabelsManagerItemUiModel> =
        when (label) {
            is LabelOrFolderWithChildren.Label -> labelToUiModel(
                label = label,
                checkedLabels = checkedLabels
            )
            is LabelOrFolderWithChildren.Folder -> folderToUiModels(
                folder = label,
                checkedLabels = checkedLabels,
                folderLevel = 0,
                parentId = null,
                parentColor = null
            )
        }

    private fun labelToUiModel(
        label: LabelOrFolderWithChildren.Label,
        checkedLabels: Collection<LabelId>
    ): List<LabelsManagerItemUiModel.Label> {
        val uiModel = LabelsManagerItemUiModel.Label(
            id = label.id,
            name = label.name,
            icon = LabelIcon.Label(label.color.toColorIntOrDefault()),
            isChecked = label.id in checkedLabels
        )
        return listOf(uiModel)
    }

    private fun folderToUiModels(
        folder: LabelOrFolderWithChildren.Folder,
        checkedLabels: Collection<LabelId>,
        folderLevel: Int,
        parentId: LabelId?,
        parentColor: Int?
    ): List<LabelsManagerItemUiModel.Folder> {

        val parent = LabelsManagerItemUiModel.Folder(
            id = folder.id,
            name = folder.name,
            icon = buildFolderIcon(folder, parentColor),
            folderLevel = folderLevel,
            parentId = parentId,
            isChecked = folder.id in checkedLabels,
        )
        val children = folder.children.flatMap {
            folderToUiModels(
                folder = it,
                checkedLabels = checkedLabels,
                folderLevel = folderLevel + 1,
                parentId = parent.id,
                parentColor = parent.icon.colorInt
            )
        }
        return listOf(parent) + children
    }

    private fun buildFolderIcon(
        folder: LabelOrFolderWithChildren.Folder,
        parentColor: Int?
    ): LabelIcon.Folder {
        val folderColorInt = folder.color.toColorIntOrNull() ?: parentColor ?: defaultIconColor
        return if (folder.children.isNotEmpty()) {
            if (useFolderColor) {
                LabelIcon.Folder.WithChildren.Colored(folderColorInt)
            } else {
                LabelIcon.Folder.WithChildren.BlackWhite(defaultIconColor)
            }
        } else {
            if (useFolderColor) {
                LabelIcon.Folder.WithoutChildren.Colored(folderColorInt)
            } else {
                LabelIcon.Folder.WithoutChildren.BlackWhite(defaultIconColor)
            }
        }
    }

    private fun String.toColorIntOrNull(): Int? = try {
        toColorInt()
    } catch (exc: IllegalArgumentException) {
        Timber.e(exc, "Unknown label color: $this")
        null
    }

    private fun String.toColorIntOrDefault(): Int =
        toColorIntOrNull() ?: defaultIconColor
}
