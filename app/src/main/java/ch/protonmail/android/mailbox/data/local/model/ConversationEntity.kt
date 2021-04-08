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

package ch.protonmail.android.mailbox.data.local.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.mailbox.data.local.model.ConversationEntity.Companion.COLUMN_ID
import ch.protonmail.android.mailbox.data.local.model.ConversationEntity.Companion.TABLE_CONVERSATIONS

@Entity(
    tableName = TABLE_CONVERSATIONS,
    indices = [Index(COLUMN_ID, unique = true)]
)
data class ConversationEntity constructor(
    @PrimaryKey
    @ColumnInfo(name = COLUMN_ID)
    val id: String,

    @ColumnInfo(name = COLUMN_ORDER)
    val order: Long = 0L,

    @ColumnInfo(name = COLUMN_USER_ID)
    val userId: String,

    @ColumnInfo(name = COLUMN_SUBJECT)
    val subject: String = "",

    @ColumnInfo(name = COLUMN_SENDERS)
    val senders: List<MessageSender> = mutableListOf(),

    @ColumnInfo(name = COLUMN_RECIPIENTS)
    val recipients: List<MessageRecipient> = mutableListOf(),

    @ColumnInfo(name = COLUMN_NUM_MESSAGES)
    val numMessages: Int = 0,

    @ColumnInfo(name = COLUMN_NUM_UNREAD)
    val numUnread: Int = 0,

    @ColumnInfo(name = COLUMN_NUM_ATTACHMENTS)
    val numAttachments: Int = 0,

    @ColumnInfo(name = COLUMN_EXPIRATION_TIME)
    val expirationTime: Long = 0,

//    @ColumnInfo(name = COLUMN_ADDRESS_ID)
//    val addressID: String,

    @ColumnInfo(name = COLUMN_SIZE)
    val size: Long = 0L
//
//    @ColumnInfo(name = COLUMN_LABELS)
//    val labels: List<String> = mutableListOf()
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
        const val COLUMN_ADDRESS_ID = "AddressID"
        const val COLUMN_SIZE = "Size"
        const val COLUMN_LABELS = "Labels"
    }
}

