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

import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * A use case that handles labeling/unlabeling conversations
 */
class UpdateConversationsLabels @Inject constructor(
    private val conversationsRepository: ConversationsRepository
) {

    suspend operator fun invoke(
        conversationIds: List<String>,
        userId: UserId,
        selectedLabels: List<String>,
        unselectedLabels: List<String>
    ) {
        selectedLabels.forEach {
            conversationsRepository.label(conversationIds, userId, it)
        }
        unselectedLabels.forEach {
            conversationsRepository.unlabel(conversationIds, userId, it)
        }
    }
}
