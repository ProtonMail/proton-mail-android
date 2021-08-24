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
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import timber.log.Timber
import javax.inject.Inject

/**
 * A Mapper of [LabelActonItemUiModel]
 *
 * @property useFolderColor whether the user enabled the settings for use Colors for Folders.
 *  TODO to be implemented in MAILAND-1818, ideally inject its use case. Currently defaults to `true`
 */
class LabelActionItemUiModelMapper @Inject constructor(
    context: Context
) {
    private val useFolderColor: Boolean = true

    private val defaultIconColor = context.getColor(R.color.icon_norm)

    fun mapLabelToUi(
        label: LabelEntity,
        currentLabelsSelection: List<String>,
        labelsSheetType: LabelsActionSheet.Type
    ): LabelActonItemUiModel {

        val iconRes = when (labelsSheetType) {
            LabelsActionSheet.Type.LABEL -> R.drawable.circle_labels_selection
            LabelsActionSheet.Type.FOLDER ->
                if (useFolderColor) R.drawable.ic_folder_filled else R.drawable.ic_folder
        }

        val colorInt = if (useFolderColor) label.color.toColorIntOrDefault() else defaultIconColor

        val isChecked = if (labelsSheetType == LabelsActionSheet.Type.LABEL) {
            currentLabelsSelection.contains(label.id)
        } else {
            null
        }

        return LabelActonItemUiModel(
            labelId = label.id.id,
            iconRes = iconRes,
            title = label.name,
            titleRes = null,
            colorInt = colorInt,
            isChecked = isChecked,
            labelType = labelsSheetType.typeInt
        )
    }

    private fun String.toColorIntOrDefault(): Int =try {
        toColorInt()
    } catch (exc: IllegalArgumentException) {
        Timber.e(exc, "Unknown label color: $this")
        defaultIconColor
    }
}
