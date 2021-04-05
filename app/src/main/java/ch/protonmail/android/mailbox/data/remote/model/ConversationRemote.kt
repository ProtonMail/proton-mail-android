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

package ch.protonmail.android.mailbox.data.remote.model

import ch.protonmail.android.api.utils.Fields
import ch.protonmail.android.mailbox.data.local.model.ConversationEntity
import com.google.gson.annotations.SerializedName
import me.proton.core.util.kotlin.EMPTY_STRING

data class ConversationRemote(
    @SerializedName(Fields.Conversation.ID)
    val id: String,

    @SerializedName(Fields.Conversation.ORDER)
    val order: Long = 0,

    @SerializedName(Fields.Conversation.SUBJECT)
    val subject: String = EMPTY_STRING,

//    @SerialName(Fields.Conversation.SENDERS)
//    val senders: List<String> = mutableListOf(),
//
//    @SerialName(Fields.Conversation.RECIPIENTS)
//    val recipients: List<String> = mutableListOf(),

    @SerializedName(Fields.Conversation.NUM_MESSAGES)
    val numMessages: Int = 0,

    @SerializedName(Fields.Conversation.NUM_UNREAD)
    val numUnread: Int = 0,

    @SerializedName(Fields.Conversation.NUM_ATTACHMENTS)
    val numAttachments: Int = 0,

    @SerializedName(Fields.Conversation.EXPIRATION_TIME)
    val expirationTime: Long = 0,

//    @SerialName(Fields.Conversation.ADDRESS_ID)
//    val addressID: String,

    @SerializedName(Fields.Conversation.SIZE)
    val size: Long = 0L,

    @SerializedName(Fields.Conversation.LABEL_IDS)
    val labelIds: List<String> = mutableListOf(),

//    @SerialName(Fields.Conversation.LABELS)
//    val labels: List<String> = mutableListOf()
)
