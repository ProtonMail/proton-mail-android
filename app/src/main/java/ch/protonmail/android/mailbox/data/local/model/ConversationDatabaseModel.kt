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

package ch.protonmail.android.mailbox.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel.Companion.COLUMN_ID
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel.Companion.TABLE_CONVERSATIONS

@Entity(
    tableName = TABLE_CONVERSATIONS,
    indices = [Index(COLUMN_ID, unique = true)]
)
data class ConversationDatabaseModel constructor(
    @PrimaryKey
    @ColumnInfo(name = COLUMN_ID)
    val id: String,

    @ColumnInfo(name = COLUMN_ORDER)
    val order: Long,

    @ColumnInfo(name = COLUMN_USER_ID)
    val userId: String,

    @ColumnInfo(name = COLUMN_SUBJECT)
    val subject: String,

    @ColumnInfo(name = COLUMN_SENDERS)
    val senders: List<MessageSender>,

    @ColumnInfo(name = COLUMN_RECIPIENTS)
    val recipients: List<MessageRecipient>,

    @ColumnInfo(name = COLUMN_NUM_MESSAGES)
    val numMessages: Int,

    @ColumnInfo(name = COLUMN_NUM_UNREAD)
    val numUnread: Int,

    @ColumnInfo(name = COLUMN_NUM_ATTACHMENTS)
    val numAttachments: Int,

    @ColumnInfo(name = COLUMN_EXPIRATION_TIME)
    val expirationTime: Long,

    @ColumnInfo(name = COLUMN_SIZE)
    val size: Long,

    @ColumnInfo(name = COLUMN_LABELS)
    val labels: List<LabelContextDatabaseModel>
) {

    companion object {

        const val TABLE_CONVERSATIONS = "conversations"
        const val COLUMN_ID = "ID"
        const val COLUMN_USER_ID = "UserID"
        const val COLUMN_ORDER = "Order"
        const val COLUMN_SUBJECT = "Subject"
        const val COLUMN_SENDERS = "Senders"
        const val COLUMN_RECIPIENTS = "Recipients"
        const val COLUMN_NUM_MESSAGES = "NumMessages"
        const val COLUMN_NUM_UNREAD = "NumUnread"
        const val COLUMN_NUM_ATTACHMENTS = "NumAttachments"
        const val COLUMN_EXPIRATION_TIME = "ExpirationTime"
        const val COLUMN_SIZE = "Size"
        const val COLUMN_LABELS = "Labels"
    }
}

