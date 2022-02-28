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

package ch.protonmail.android.mailbox.data.remote.model

import com.google.gson.annotations.SerializedName

data class LabelContextApiModel(
    @SerializedName(LABEL_ID)
    val id: String,
    @SerializedName(CONTEXT_NUM_UNREAD)
    val contextNumUnread: Int,
    @SerializedName(CONTEXT_NUM_MESSAGES)
    val contextNumMessages: Int,
    @SerializedName(CONTEXT_TIME)
    val contextTime: Long,
    @SerializedName(CONTEXT_SIZE)
    val contextSize: Int,
    @SerializedName(CONTEXT_NUM_ATTACHMENTS)
    val contextNumAttachments: Int
) {

    companion object {

        private const val LABEL_ID = "ID"
        private const val CONTEXT_NUM_UNREAD = "ContextNumUnread"
        private const val CONTEXT_NUM_MESSAGES = "ContextNumMessages"
        private const val CONTEXT_TIME = "ContextTime"
        private const val CONTEXT_SIZE = "ContextSize"
        private const val CONTEXT_NUM_ATTACHMENTS = "ContextNumAttachments"
    }
}
