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

import ch.protonmail.android.labels.data.local.model.LabelType
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import javax.inject.Inject

class GetAllLabels @Inject constructor(
    private val labelsMapper: LabelEntityDomainMapper,
    private val accountManager: AccountManager,
    private val labelRepository: LabelRepository
) {

    suspend operator fun invoke(
        labelsSheetType: LabelType = LabelType.MESSAGE_LABEL
    ): List<Label> {
        val userId = accountManager.getPrimaryUserId().filterNotNull().first()
        val dbLabels = labelRepository.findAllLabels(userId, false)

        return dbLabels
            .filter { it.type.typeInt == labelsSheetType.typeInt }
            .map { labelsMapper.toLabel(it) }
    }

}
