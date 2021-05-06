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
import ch.protonmail.android.labels.presentation.model.ManageLabelItemUiModel
import ch.protonmail.android.labels.presentation.ui.ManageLabelsActionSheet
import javax.inject.Inject

class LabelsMapper @Inject constructor() {

    fun mapLabelToUi(
        label: Label,
        currentLabelsSelection: List<String>,
        labelsSheetType: ManageLabelsActionSheet.Type
    ): ManageLabelItemUiModel {
        val iconRes = if (labelsSheetType == ManageLabelsActionSheet.Type.LABEL) {
            R.drawable.circle_labels_selection
        } else {
            R.drawable.ic_folder
        }

        val colorInt = if (labelsSheetType == ManageLabelsActionSheet.Type.LABEL) {
            label.color.toColorInt()
        } else {
            Color.BLACK
        }

        val isChecked = if (labelsSheetType == ManageLabelsActionSheet.Type.LABEL) {
            currentLabelsSelection.contains(label.id)
        } else {
            null
        }

        return ManageLabelItemUiModel(
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
