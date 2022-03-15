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

package ch.protonmail.android.notifications.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val NOTIFICATION_ACTION_MESSAGE_CREATED = "message_created"
const val NOTIFICATION_ACTION_MESSAGE_TOUCHED = "message_touched"

@Serializable
enum class NotificationAction(val action: String) {

    @SerialName(NOTIFICATION_ACTION_MESSAGE_CREATED)
    CREATED("message_created"),

    @SerialName(NOTIFICATION_ACTION_MESSAGE_TOUCHED)
    TOUCHED("message_touched");

    companion object {

        fun fromStringOrNull(action: String): NotificationAction? {
            return values().find {
                it.action == action
            }
        }
    }
}
