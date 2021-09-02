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

import ch.protonmail.android.core.Constants
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.local.model.LabelType
import ch.protonmail.android.labels.presentation.mapper.LabelActionItemUiModelMapper
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.labels.presentation.model.StandardFolderLocation
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

class GetAllLabels @Inject constructor(
    private val labelsMapper: LabelActionItemUiModelMapper,
    private val accountManager: AccountManager,
    private val labelRepository: LabelRepository
) {

    suspend operator fun invoke(
        currentLabelsSelection: List<String>,
        labelsSheetType: LabelType = LabelType.MESSAGE_LABEL,
        currentMessageFolder: Constants.MessageLocationType? = null // only required for Type.FOLDER
    ): List<LabelActonItemUiModel> {
        val userId = accountManager.getPrimaryUserId().filterNotNull().first()
        val dbLabels = labelRepository.findAllLabels(userId, false)

        val uiLabelsFromDb = dbLabels
            .filter { it.type.typeInt == labelsSheetType.typeInt }
            .map { label ->
                labelsMapper.mapLabelToUi(label, currentLabelsSelection, labelsSheetType)
            }
        return if (labelsSheetType == LabelType.FOLDER) {
            requireNotNull(currentMessageFolder)
            uiLabelsFromDb + getStandardFolders(currentMessageFolder)
        } else
            uiLabelsFromDb
    }

    private fun getStandardFolders(
        currentMessageFolder: Constants.MessageLocationType
    ): List<LabelActonItemUiModel> {
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
        .filter {
            it.id != currentMessageFolder.messageLocationTypeValue.toString()
        }
        .map { location ->
            LabelActonItemUiModel(
                labelId = location.id,
                iconRes = location.iconRes,
                titleRes = location.title,
                labelType = LabelType.FOLDER.typeInt
            )
        }
}
