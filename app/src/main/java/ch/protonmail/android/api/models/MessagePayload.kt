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
    var ID: String? = null,
    @SerializedName(Fields.Message.SUBJECT)
    var subject: String? = null,
    @SerializedName(Fields.Message.SENDER)
    var sender: ServerMessageSender? = null,
    @SerializedName(Fields.Message.MESSAGE_BODY)
    var body: String? = null,
    @SerializedName(Fields.Message.TO_LIST)
    var toList: List<MessageRecipient>? = null,
    @SerializedName(Fields.Message.CC_LIST)
    var ccList: List<MessageRecipient>? = null,
    @SerializedName(Fields.Message.BCC_LIST)
    var bccList: List<MessageRecipient>? = null,
    @SerializedName(Fields.Message.UNREAD)
    var unread: Int? = null
)
