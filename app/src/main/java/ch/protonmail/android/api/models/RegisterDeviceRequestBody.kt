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

package ch.protonmail.android.api.models

import com.google.gson.annotations.SerializedName

// region constants
private const val FIELD_DEVICE_TOKEN = "DeviceToken"
private const val FIELD_ENVIRONMENT = "Environment"

const val DEVICE_ENVIRONMENT_ANDROID = 4
// endregion

/**
 * A request body for registering a device on PM servers.
 */

// GSON doesn't serialize the class correctly when parameters are val
data class RegisterDeviceRequestBody(
    @SerializedName(FIELD_DEVICE_TOKEN)
    var deviceToken: String,
    @SerializedName(FIELD_ENVIRONMENT)
    var environment: Int
)
