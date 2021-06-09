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

private const val IDS = "IDs"
private const val LABEL_ID = "LabelID"

/**
 * Request body that should be used when performing an action on conversations.
 */
data class ConversationIdsRequestBody(
    @SerializedName(LABEL_ID)
    val labelId: String? = null,
    @SerializedName(IDS)
    val ids: List<String>
)
