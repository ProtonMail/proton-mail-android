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
package ch.protonmail.android.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// region constants
private const val FIELD_MESSAGE_ID = "MessageID"
private const val FIELD_BODY = "Body"
private const val FIELD_MIME_TYPE = "MIMEType"
// endregion

@Serializable
data class PostPhishingReportBody(
    @SerialName(FIELD_MESSAGE_ID)
    val messageId: String,
    @SerialName(FIELD_BODY)
    val body: String,
    @SerialName(FIELD_MIME_TYPE)
    val mimeType: String
)
