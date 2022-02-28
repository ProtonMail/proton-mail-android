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
import me.proton.core.util.kotlin.EMPTY_STRING

data class ConversationApiModel(
    @SerializedName(ID)
    val id: String,

    @SerializedName(ORDER)
    val order: Long,

    @SerializedName(SUBJECT)
    val subject: String = EMPTY_STRING,

    @SerializedName(SENDERS)
    val senders: List<CorrespondentApiModel>,

    @SerializedName(RECIPIENTS)
    val recipients: List<CorrespondentApiModel>,

    @SerializedName(NUM_MESSAGES)
    val numMessages: Int,

    @SerializedName(NUM_UNREAD)
    val numUnread: Int,

    @SerializedName(NUM_ATTACHMENTS)
    val numAttachments: Int,

    @SerializedName(EXPIRATION_TIME)
    val expirationTime: Long,

    @SerializedName(SIZE)
    val size: Long,

    @SerializedName(LABELS)
    val labels: List<LabelContextApiModel>,

    @SerializedName(CONTEXT_TIME)
    val contextTime: Long
) {
    companion object {

        private const val ID = "ID"
        private const val ORDER = "Order"
        private const val SUBJECT = "Subject"
        private const val SENDERS = "Senders"
        private const val RECIPIENTS = "Recipients"
        private const val NUM_MESSAGES = "NumMessages"
        private const val NUM_UNREAD = "NumUnread"
        private const val NUM_ATTACHMENTS = "NumAttachments"
        private const val EXPIRATION_TIME = "ExpirationTime"
        private const val SIZE = "Size"
        private const val LABELS = "Labels"
        private const val CONTEXT_TIME = "ContextTime"
    }
}
