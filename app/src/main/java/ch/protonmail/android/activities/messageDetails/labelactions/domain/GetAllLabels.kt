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

package ch.protonmail.android.activities.messageDetails.labelactions.domain

import ch.protonmail.android.activities.messageDetails.labelactions.ManageLabelItemUiModel
import ch.protonmail.android.activities.messageDetails.labelactions.ManageLabelsActionSheet
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import javax.inject.Inject

class GetAllLabels @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val labelsMapper: LabelsMapper
) {

    suspend operator fun invoke(
        currentLabelsSelection: List<String>,
        labelsSheetType: ManageLabelsActionSheet.Type = ManageLabelsActionSheet.Type.LABEL
    ): List<ManageLabelItemUiModel> {
        val dbLabels = messageDetailsRepository.getAllLabels()

        val uiLabelsFromDb = dbLabels
            .filter { it.type == labelsSheetType.typeInt }
            .map { label ->
                labelsMapper.mapLabelToUi(label, currentLabelsSelection, labelsSheetType)
            }
        return if (labelsSheetType == ManageLabelsActionSheet.Type.FOLDER)
            uiLabelsFromDb + getStandardFolders()
        else
            uiLabelsFromDb
    }

    private fun getStandardFolders(): List<ManageLabelItemUiModel> {
        return listOf(
            ManageLabelItemUiModel(
                labelId = NewFolderLocation.Inbox.id,
                titleRes = NewFolderLocation.Inbox.title,
                iconRes = NewFolderLocation.Inbox.iconRes,
            ),
            ManageLabelItemUiModel(
                labelId = NewFolderLocation.Archive.id,
                titleRes = NewFolderLocation.Archive.title,
                iconRes = NewFolderLocation.Archive.iconRes,
            ),
            ManageLabelItemUiModel(
                labelId = NewFolderLocation.Spam.id,
                titleRes = NewFolderLocation.Spam.title,
                iconRes = NewFolderLocation.Spam.iconRes,
            ),
            ManageLabelItemUiModel(
                labelId = NewFolderLocation.Trash.id,
                titleRes = NewFolderLocation.Trash.title,
                iconRes = NewFolderLocation.Trash.iconRes,
            )
        )
    }
}
