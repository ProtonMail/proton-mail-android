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

import com.google.gson.annotations.SerializedName
import me.proton.core.util.kotlin.EMPTY_STRING

const val LABEL_ID = "ID"
const val CONTEXT_NUM_UNREAD = "ContextNumUnread"
const val CONTEXT_NUM_MESSAGES = "ContextNumMessages"
const val CONTEXT_TIME = "ContextTime"
const val CONTEXT_SIZE = "ContextSize"
const val CONTEXT_NUM_ATTACHMENTS = "ContextNumAttachments"

data class LabelContextApiModel(
    @SerializedName(LABEL_ID)
    val id: String = EMPTY_STRING,
    @SerializedName(CONTEXT_NUM_UNREAD)
    val contextNumUnread: Int = 0,
    @SerializedName(CONTEXT_NUM_MESSAGES)
    val contextNumMessages: Int = 0,
    @SerializedName(CONTEXT_TIME)
    val contextTime: Long = 0L,
    @SerializedName(CONTEXT_SIZE)
    val contextSize: Int = 0,
    @SerializedName(CONTEXT_NUM_ATTACHMENTS)
    val contextNumAttachments: Int = 0
)
