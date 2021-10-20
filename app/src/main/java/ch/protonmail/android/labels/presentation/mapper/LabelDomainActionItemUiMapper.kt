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
import ch.protonmail.android.labels.domain.model.LabelOrFolderWithChildren
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import me.proton.core.domain.arch.Mapper
import timber.log.Timber
import javax.inject.Inject

/**
 * A Mapper of [LabelActonItemUiModel]
 *
 * @property useFolderColor whether the user enabled the settings for use Colors for Folders.
 *  TODO to be implemented in MAILAND-1818, ideally inject its use case. Currently defaults to `true`
 */
class LabelDomainActionItemUiMapper @Inject constructor(
    context: Context
) : Mapper<Collection<LabelOrFolderWithChildren>, List<LabelActonItemUiModel>> {

    private val useFolderColor: Boolean = true

    private val defaultIconColor = context.getColor(R.color.icon_norm)

    fun toUiModels(
        labels: Collection<LabelOrFolderWithChildren>,
        currentLabelsSelection: List<String>
    ): List<LabelActonItemUiModel> = labels.flatMap { labelToUiModels(it, currentLabelsSelection, 0) }

    private fun labelToUiModels(
        label: LabelOrFolderWithChildren,
        currentLabelsSelection: List<String>,
        folderLevel: Int
    ): List<LabelActonItemUiModel> {

        val parent = labelToUiModel(
            label = label,
            currentLabelsSelection = currentLabelsSelection,
            folderLevel = folderLevel
        )
        val children = if (label is LabelOrFolderWithChildren.Folder) {
            label.children.flatMap { labelToUiModels(it, currentLabelsSelection, folderLevel + 1) }
        } else {
            emptyList()
        }
        return listOf(parent) + children
    }

    private fun labelToUiModel(
        label: LabelOrFolderWithChildren,
        currentLabelsSelection: List<String>,
        folderLevel: Int,
    ): LabelActonItemUiModel {

        val labelType = when (label) {
            is LabelOrFolderWithChildren.Label -> LabelType.MESSAGE_LABEL
            is LabelOrFolderWithChildren.Folder -> LabelType.FOLDER
        }
        val iconRes = when (labelType) {
            LabelType.MESSAGE_LABEL -> R.drawable.circle_labels_selection
            LabelType.FOLDER ->
                if (useFolderColor) R.drawable.ic_folder_filled else R.drawable.ic_folder
            LabelType.CONTACT_GROUP -> throw IllegalArgumentException("Contacts are currently unsupported")
        }

        val colorInt = if (useFolderColor) label.color.toColorIntOrDefault() else defaultIconColor

        val isChecked = if (labelType == LabelType.MESSAGE_LABEL) {
            currentLabelsSelection.contains(label.id.id)
        } else {
            null
        }

        return LabelActonItemUiModel(
            labelId = label.id,
            iconRes = iconRes,
            title = label.name,
            titleRes = null,
            colorInt = colorInt,
            folderLevel = folderLevel,
            isChecked = isChecked,
            labelType = labelType
        )
    }

    private fun String.toColorIntOrDefault(): Int = try {
        toColorInt()
    } catch (exc: IllegalArgumentException) {
        Timber.e(exc, "Unknown label color: $this")
        defaultIconColor
    }
}
