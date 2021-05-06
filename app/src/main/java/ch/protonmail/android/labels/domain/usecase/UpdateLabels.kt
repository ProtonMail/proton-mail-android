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
import timber.log.Timber
import javax.inject.Inject

class UpdateLabels @Inject constructor(
    private val messageDetailsRepository: MessageDetailsRepository
) {

    suspend operator fun invoke(
        messageId: String,
        checkedLabelIds: List<String>,
        isTransient: Boolean = false
    ) {
        val message = messageDetailsRepository.findMessageByIdOnce(messageId)
        val existingLabels = messageDetailsRepository.getAllLabels()
            .filter { it.id in message.labelIDsNotIncludingLocations }
        Timber.v("UpdateLabels checkedLabelIds: $checkedLabelIds")
        messageDetailsRepository.findAllLabelsWithIds(
            message,
            checkedLabelIds,
            existingLabels,
            isTransient
        )
    }
}
