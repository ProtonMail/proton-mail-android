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

package ch.protonmail.android.labels.domain.usecase

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.presentation.mapper.LabelsMapper
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.model.StandardFolderLocation
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import me.proton.core.util.kotlin.toBooleanOrFalse
import javax.inject.Inject

class GetAllLabels @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val labelsMapper: LabelsMapper
) {

    suspend operator fun invoke(
        currentLabelsSelection: List<String>,
        labelsSheetType: LabelsActionSheet.Type = LabelsActionSheet.Type.LABEL,
        currentMessageFolder: Constants.MessageLocationType? = null // only required for Type.FOLDER
    ): List<LabelActonItemUiModel> {
        val dbLabels = messageDetailsRepository.getAllLabels()

        val uiLabelsFromDb = dbLabels
            .filter { it.exclusive == labelsSheetType.typeInt.toBooleanOrFalse() }
            .map { label ->
                labelsMapper.mapLabelToUi(label, currentLabelsSelection, labelsSheetType)
            }
        return if (labelsSheetType == LabelsActionSheet.Type.FOLDER) {
            requireNotNull(currentMessageFolder)
            uiLabelsFromDb + getStandardFolders(currentMessageFolder)
        } else
            uiLabelsFromDb
    }

    private fun getStandardFolders(currentMessageFolder: Constants.MessageLocationType): List<LabelActonItemUiModel> {
        return when (currentMessageFolder) {
            Constants.MessageLocationType.INBOX,
            Constants.MessageLocationType.ARCHIVE,
            Constants.MessageLocationType.SPAM,
            Constants.MessageLocationType.TRASH -> getListWithoutType(currentMessageFolder)
            else -> getListWithoutType()
        }
    }

    private fun getListWithoutType(
        currentMessageFolder: Constants.MessageLocationType = Constants.MessageLocationType.INVALID
    ) = StandardFolderLocation.values()
        .filter { it.id != currentMessageFolder.name }
        .map { location ->
            LabelActonItemUiModel(
                labelId = location.id,
                iconRes = location.iconRes,
                titleRes = location.title,
                labelType = LabelsActionSheet.Type.FOLDER.typeInt
            )
        }
}
