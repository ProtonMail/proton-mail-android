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

package ch.protonmail.android.notifications.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val NOTIFICATION_TYPE_EMAIL = "email"
const val NOTIFICATION_TYPE_OPEN_URL = "open_url"

@Serializable
enum class NotificationType(val type: String) {

    @SerialName(NOTIFICATION_TYPE_EMAIL)
    EMAIL("email"),

    @SerialName(NOTIFICATION_TYPE_OPEN_URL)
    OPEN_URL("open_url");

    companion object {

        fun fromStringOrNull(type: String): NotificationType? {
            return values().find {
                it.type == type
            }
        }
    }
}
