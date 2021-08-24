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

package ch.protonmail.android.mailbox.presentation.model

import androidx.recyclerview.widget.DiffUtil
import ch.protonmail.android.ui.model.LabelChipUiModel

data class MailboxUiItem(
    val itemId: String,
    val senderName: String,
    val subject: String,
    val lastMessageTimeMs: Long,
    val hasAttachments: Boolean,
    val isStarred: Boolean,
    val isRead: Boolean,
    val expirationTime: Long,
    val messagesCount: Int?,
    val messageData: MessageData?,
    val isDeleted: Boolean,
    val labels: List<LabelChipUiModel>,
    val recipients: String,
    val isDraft: Boolean
) {

    companion object {

        val DiffCallback = object : DiffUtil.ItemCallback<MailboxUiItem>() {

            override fun areItemsTheSame(oldItem: MailboxUiItem, newItem: MailboxUiItem) =
                oldItem.itemId == newItem.itemId

            override fun areContentsTheSame(oldItem: MailboxUiItem, newItem: MailboxUiItem) =
                oldItem == newItem
        }
    }
}

data class MessageData(
    val location: Int,
    val isReplied: Boolean,
    val isRepliedAll: Boolean,
    val isForwarded: Boolean,
    val isInline: Boolean
)
