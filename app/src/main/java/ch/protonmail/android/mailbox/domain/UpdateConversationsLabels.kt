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

package ch.protonmail.android.mailbox.domain

import ch.protonmail.android.data.LabelRepository
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import kotlinx.coroutines.flow.first
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * A use case that handles labeling/unlabeling conversations
 */
class UpdateConversationsLabels @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val labelRepository: LabelRepository
) {

    suspend operator fun invoke(
        conversationIds: List<String>,
        userId: UserId,
        selectedLabelIds: List<String>
    ): ConversationsActionResult {

        selectedLabelIds.forEach {
            val result = conversationsRepository.label(conversationIds, userId, it)
            if (result is ConversationsActionResult.Error) {
                return result
            }
        }

        val unselectedLabelIds = labelRepository.findAllLabels(userId)
            .first()
            .map { it.id }
            .filter { it !in selectedLabelIds }

        unselectedLabelIds.forEach {
            val result = conversationsRepository.unlabel(conversationIds, userId, it)
            if (result is ConversationsActionResult.Error) {
                return result
            }
        }

        return ConversationsActionResult.Success
    }
}
