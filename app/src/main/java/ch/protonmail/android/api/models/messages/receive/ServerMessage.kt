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
package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.messages.ParsedHeaders
import com.google.gson.annotations.SerializedName

private const val FIELD_FLAGS = "Flags"
private const val FIELD_ID = "ID"
private const val FIELD_LABEL_IDS_ADDED = "LabelIDsAdded"
private const val FIELD_LABEL_IDS_REMOVED = "LabelIDsRemoved"
private const val FIELD_PARSED_HEADERS = "ParsedHeaders"
private const val FIELD_TIME = "Time"

data class ServerMessage(
    @SerializedName(FIELD_ID)
    val id: String? = null,
    val ConversationID: String,
    val Subject: String? = null,
    val Order: Long? = null,
    val Unread: Int = -1,
    val Type: Int = -1, // 0 = INBOX, 1 = DRAFT, 2 = SENT, 3 = INBOX_AND_SENT
    val Sender: ServerMessageSender? = null,
    @SerializedName(FIELD_FLAGS)
    val flags: Long = 0,
    @SerializedName(FIELD_TIME)
    val time: Long = -1,
    val Size: Long = -1,
    val FolderLocation: String? = null,
    val Starred: Int = -1,
    val NumAttachments: Int = -1,
    val ExpirationTime: Long = -1,
    val SpamScore: Int = -1,
    val AddressID: String? = null,
    val Body: String? = null,
    val MIMEType: String? = null,
    val LabelIDs: List<String>? = null,
    @SerializedName(FIELD_LABEL_IDS_ADDED)
    val LabelIDsAdded: List<String>? = null,
    @SerializedName(FIELD_LABEL_IDS_REMOVED)
    val LabelIDsRemoved: List<String>? = null,
    val ToList: List<MessageRecipient>? = null,
    val CCList: List<MessageRecipient>? = null,
    val BCCList: List<MessageRecipient>? = null,
    val ReplyTos: List<MessageRecipient>? = null,
    val Header: String? = null,
    @SerializedName(FIELD_PARSED_HEADERS)
    val parsedHeaders: ParsedHeaders? = null,
    val Attachments: List<ServerAttachment>? = listOf(),
    val embeddedImagesArray: List<String>? = listOf()
)
