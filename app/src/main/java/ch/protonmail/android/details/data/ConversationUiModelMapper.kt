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

package ch.protonmail.android.details.data

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.details.presentation.model.ConversationUiModel
import ch.protonmail.android.mailbox.domain.model.Conversation

private const val STARRED_LABEL_ID = "10"

internal fun Conversation.toConversationUiModel() = ConversationUiModel(
    isStarred = labels.any { it.id == STARRED_LABEL_ID },
    subject = subject,
    labelIds = labels.map { it.id },
    messages = messages?.toDbModelList().orEmpty(),
    messagesCount = messagesCount
)

internal fun Message.toConversationUiModel() = ConversationUiModel(
    isStarred = isStarred ?: false,
    subject = subject,
    labelIds = labelIDsNotIncludingLocations,
    messages = listOf(this),
    messagesCount = null
)
