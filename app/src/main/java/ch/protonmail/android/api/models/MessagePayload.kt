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

import ch.protonmail.android.api.models.messages.receive.ServerMessageSender
import ch.protonmail.android.api.utils.Fields
import com.google.gson.annotations.SerializedName

data class MessagePayload(

    @SerializedName(Fields.Message.SENDER)
    val sender: ServerMessageSender,

    @SerializedName(Fields.Message.MESSAGE_BODY)
    val body: String,

    @SerializedName(Fields.Message.ID)
    val id: String? = null,

    @SerializedName(Fields.Message.SUBJECT)
    val subject: String? = null,

    @SerializedName(Fields.Message.TO_LIST)
    val toList: List<MessageRecipient>? = null,

    @SerializedName(Fields.Message.CC_LIST)
    val ccList: List<MessageRecipient>? = null,

    @SerializedName(Fields.Message.BCC_LIST)
    val bccList: List<MessageRecipient>? = null,

    @SerializedName(Fields.Message.UNREAD)
    val unread: Int? = null
)
