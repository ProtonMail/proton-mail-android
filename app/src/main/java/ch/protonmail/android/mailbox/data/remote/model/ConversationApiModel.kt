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

const val ID = "ID"
const val ORDER = "Order"
const val SUBJECT = "Subject"
const val SENDERS = "Senders"
const val RECIPIENTS = "Recipients"
const val NUM_MESSAGES = "NumMessages"
const val NUM_UNREAD = "NumUnread"
const val NUM_ATTACHMENTS = "NumAttachments"
const val EXPIRATION_TIME = "ExpirationTime"
const val SIZE = "Size"
const val LABELS = "Labels"

data class ConversationApiModel(
    @SerializedName(ID)
    val id: String,

    @SerializedName(ORDER)
    val order: Long = 0,

    @SerializedName(SUBJECT)
    val subject: String = EMPTY_STRING,

    @SerializedName(SENDERS)
    val senders: List<CorrespondentApiModel> = mutableListOf(),

    @SerializedName(RECIPIENTS)
    val recipients: List<CorrespondentApiModel> = mutableListOf(),

    @SerializedName(NUM_MESSAGES)
    val numMessages: Int = 0,

    @SerializedName(NUM_UNREAD)
    val numUnread: Int = 0,

    @SerializedName(NUM_ATTACHMENTS)
    val numAttachments: Int = 0,

    @SerializedName(EXPIRATION_TIME)
    val expirationTime: Long = 0,

    @SerializedName(SIZE)
    val size: Long = 0L,

    @SerializedName(LABELS)
    val labels: List<LabelContextApiModel> = mutableListOf()
)
