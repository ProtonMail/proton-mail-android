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
package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.api.models.MessagePayload
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.messages.ParsedHeaders
import com.google.gson.annotations.SerializedName

// region constants
private const val FIELD_LABEL_IDS_REMOVED = "LabelIDsRemoved"
private const val FIELD_LABEL_IDS_ADDED = "LabelIDsAdded"
private const val FIELD_PARSED_HEADERS = "ParsedHeaders"
// endregion

data class ServerMessage(
    var ID: String? = null,
    var ConversationID: String? = null,
    var Subject: String? = null,
    var Order: Long? = null,
    var Unread: Int = -1,//todo new
    var Type: Int = -1, // 0 = INBOX, 1 = DRAFT, 2 = SENT, 3 = INBOX_AND_SENT
    var Sender: ServerMessageSender? = null,
    var Flags: Long = 0,
    var Time: Long = -1,
    var Size: Long = -1,
    var FolderLocation: String? = null,
    var Starred: Int = -1,
    var NumAttachments: Int = -1,
    var ExpirationTime: Long = -1,
    var SpamScore: Int = -1,
    var AddressID: String? = null,
    var Body: String? = null,
    var MIMEType: String? = null,
    var LabelIDs: List<String>? = null,
    @SerializedName(FIELD_LABEL_IDS_ADDED)
    var LabelIDsAdded: List<String>? = null,
    @SerializedName(FIELD_LABEL_IDS_REMOVED)
    var LabelIDsRemoved: List<String>? = null,
    var ToList: List<MessageRecipient>? = null,
    var CCList: List<MessageRecipient>? = null,
    var BCCList: List<MessageRecipient>? = null,
    var ReplyTos: List<MessageRecipient>? = null,
    var Header: String? = null,
    @SerializedName(FIELD_PARSED_HEADERS)
    var parsedHeaders: ParsedHeaders? = null,
    var Attachments: List<ServerAttachment> = listOf(),
    var embeddedImagesArray: List<String> = listOf()
) {
    /**
     * Converts the message to a more compact payload used
     * for create and update draft.
     */
    fun toMessagePayload() = MessagePayload(
        ID,
        Subject,
        Sender,
        Body,
        ToList,
        CCList,
        BCCList,
        Unread
    )
}
