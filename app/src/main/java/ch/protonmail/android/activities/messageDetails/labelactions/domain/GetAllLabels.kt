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
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import javax.inject.Inject

class GetAllLabels @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository,
    private val labelsMapper: LabelsMapper
) {

    suspend operator fun invoke(currentLabelsSelection: List<String>): List<ManageLabelItemUiModel> {
        val dbLabels = messageDetailsRepository.getAllLabels()

        return dbLabels
            .filter { it.type == TYPE_NORMAL_LABEL }
            .map { label ->
                labelsMapper.mapLabelToUi(label, currentLabelsSelection)
            }
    }
    companion object {
        private const val TYPE_NORMAL_LABEL = 0
    }
}
