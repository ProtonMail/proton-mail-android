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

import android.graphics.Color
import androidx.core.graphics.toColorInt
import ch.protonmail.android.R
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import timber.log.Timber
import javax.inject.Inject

class LabelsMapper @Inject constructor() {

    fun mapLabelToUi(
        label: Label,
        currentLabelsSelection: List<String>,
        labelsSheetType: LabelsActionSheet.Type
    ): LabelActonItemUiModel {
        val iconRes = if (labelsSheetType == LabelsActionSheet.Type.LABEL) {
            R.drawable.circle_labels_selection
        } else {
            R.drawable.ic_folder
        }

        // TODO if (labelsSheetType == LabelsActionSheet.Type.FOLDER && setting from the net MailSettings.EnableFolderColor != true)
        //  Color.BLACK else label.color
        val colorInt = try {
            label.color.toColorInt()
        } catch (exc: IllegalArgumentException) {
            Timber.e(exc, "Unknown label color: ${label.color}")
            Color.GRAY
        }

        val isChecked = if (labelsSheetType == LabelsActionSheet.Type.LABEL) {
            currentLabelsSelection.contains(label.id)
        } else {
            null
        }

        return LabelActonItemUiModel(
            labelId = label.id,
            iconRes = iconRes,
            title = label.name,
            titleRes = null,
            colorInt = colorInt,
            isChecked = isChecked,
            labelType = labelsSheetType.typeInt
        )
    }
}
