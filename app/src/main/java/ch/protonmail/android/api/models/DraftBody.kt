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
package ch.protonmail.android.api.models

import ch.protonmail.android.api.models.messages.receive.ServerMessage
import ch.protonmail.android.api.models.messages.receive.ServerMessageSender
import ch.protonmail.android.api.models.room.messages.MessageSender
import ch.protonmail.android.api.utils.Fields
import com.google.gson.annotations.SerializedName

data class DraftBody(
    val serverMessage: ServerMessage
) {
    @SerializedName(Fields.Message.MESSAGE)
    var message: MessagePayload = serverMessage.toMessagePayload()

    @SerializedName(Fields.Message.PARENT_ID)
    var parentID: String? = null

    @SerializedName(Fields.Message.ACTION)
    var action = 0

    @SerializedName(Fields.Message.UNREAD)
    var unread: Int? = message.unread

    @SerializedName(Fields.Message.Send.ATTACHMENT_KEY_PACKETS)
    var attachmentKeyPackets: MutableMap<String, String> = hashMapOf()

    fun setSender(messageSender: MessageSender) {
        message.sender = ServerMessageSender(messageSender.name, messageSender.emailAddress)
    }

    fun setMessageBody(messageBody: String) {
        message.body = messageBody
    }

    fun addAttachmentKeyPacket(key: String, value: String) {
        attachmentKeyPackets!![key] = value
    }

}

