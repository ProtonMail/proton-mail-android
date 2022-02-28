/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.labels.domain.usecase

import androidx.work.WorkInfo
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case responsible for removing labels.
 */
class DeleteLabels @Inject constructor(
    private val labelRepository: LabelRepository
) {

    suspend operator fun invoke(labelIds: List<LabelId>): Flow<Boolean> =
        labelRepository.scheduleDeleteLabels(labelIds)
            .filter { it.state.isFinished }
            .map { workInfo ->
                Timber.v("Finished worker State ${workInfo.state}")
                workInfo.state == WorkInfo.State.SUCCEEDED
            }
}
