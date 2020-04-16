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
package ch.protonmail.android.gcm.models

import com.google.gson.annotations.SerializedName

data class NotificationEncryptedData(
        @SerializedName("title") val title: String? = null,
        @SerializedName("subtitle") val subtitle: String? = null,
        @SerializedName("body") val body: String? = null,
        @SerializedName("vibrate") val vibrate: Int = 0,
        @SerializedName("sound") val sound: Int = 0,
        @SerializedName("largeIcon") val largeIcon: String? = null,
        @SerializedName("smallIcon") val smallIcon: String? = null,
        @SerializedName("badge") val badge: Int = 0,
        @SerializedName("messageId") val messageId: String? = null,
        @SerializedName("customId") val customId: String? = null,
        @SerializedName("sender") val sender: NotificationSender? = null
)