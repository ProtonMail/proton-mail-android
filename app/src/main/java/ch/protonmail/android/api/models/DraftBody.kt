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

import ch.protonmail.android.api.utils.Fields
import com.google.gson.annotations.SerializedName

data class DraftBody(

    @SerializedName(Fields.Message.MESSAGE)
    val message: MessagePayload,

    @SerializedName(Fields.Message.PARENT_ID)
    val parentId: String? = null,

    @SerializedName(Fields.Message.ACTION)
    val action: Int = 0,

    @SerializedName(Fields.Message.UNREAD)
    val unread: Int? = message.unread,

    @SerializedName(Fields.Message.Send.ATTACHMENT_KEY_PACKETS)
    val attachmentKeyPackets: Map<String, String> = hashMapOf()
)

