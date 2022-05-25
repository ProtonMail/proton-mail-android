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

package ch.protonmail.android.details.presentation.model

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.ui.model.LabelChipUiModel

data class ConversationUiModel(
    val isStarred: Boolean,
    val subject: String?,
    val messages: List<Message>,
    val messagesCount: Int?,
    // TODO: This should be improved- there should be just one type of message UI model that the adapter
    // is also using, for the time being we keep both a list of Message objects and a list of models
    // used by the adapter as a workaround
    val messageListItems: List<MessageDetailsListItem> = emptyList(),
    // TODO: The labels should be part of the message UI model instead
    val nonExclusiveLabels: HashMap<String, List<LabelChipUiModel>> = hashMapOf(),
    val exclusiveLabels: HashMap<String, List<Label>> = hashMapOf(),
)
