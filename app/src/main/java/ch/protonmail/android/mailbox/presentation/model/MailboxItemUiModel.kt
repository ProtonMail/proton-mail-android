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

package ch.protonmail.android.mailbox.presentation.model

import androidx.recyclerview.widget.DiffUtil
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.ui.model.LabelChipUiModel

/**
 * @property messageLabels Collection of [LabelChipUiModel] or type [LabelType.MESSAGE_LABEL]
 * @property allLabelsIds Collection of IDs of all the Labels, including [LabelType.FOLDER]
 */
data class MailboxItemUiModel(
    val itemId: String,
    val correspondentsNames: String,
    val subject: String,
    val lastMessageTimeMs: Long,
    val hasAttachments: Boolean,
    val isStarred: Boolean,
    val isRead: Boolean,
    val expirationTime: Long,
    val messagesCount: Int?,
    val messageData: MessageData?,
    val messageLabels: List<LabelChipUiModel>,
    val allLabelsIds: List<LabelId>,
    val isDraft: Boolean,
    val isScheduled: Boolean,
    val isProton: Boolean
) {

    class DiffCallback : DiffUtil.ItemCallback<MailboxItemUiModel>() {

        override fun areItemsTheSame(oldItem: MailboxItemUiModel, newItem: MailboxItemUiModel) =
            oldItem.itemId == newItem.itemId

        override fun areContentsTheSame(oldItem: MailboxItemUiModel, newItem: MailboxItemUiModel) =
            oldItem == newItem
    }

}

data class MessageData(
    val location: Int,
    val isReplied: Boolean,
    val isRepliedAll: Boolean,
    val isForwarded: Boolean,
    val isInline: Boolean
)
