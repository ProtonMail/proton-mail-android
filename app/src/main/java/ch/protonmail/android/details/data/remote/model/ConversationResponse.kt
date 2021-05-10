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

package ch.protonmail.android.details.data.remote.model

import ch.protonmail.android.api.models.messages.receive.ServerMessage
import ch.protonmail.android.mailbox.data.remote.model.ConversationApiModel
import com.google.gson.annotations.SerializedName

data class ConversationResponse(
    @SerializedName(TOTAL)
    val total: Int,
    @SerializedName(CONVERSATION)
    val conversation: ConversationApiModel,
    @SerializedName(MESSAGES)
    val messages: List<ServerMessage>
) {
    companion object {

        private const val TOTAL = "Total"
        private const val CONVERSATION = "Conversation"
        private const val MESSAGES = "Messages"
    }
}
