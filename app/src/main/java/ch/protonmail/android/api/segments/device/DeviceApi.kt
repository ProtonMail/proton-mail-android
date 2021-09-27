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
package ch.protonmail.android.api.segments.device

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.RegisterDeviceRequestBody
import ch.protonmail.android.api.segments.BaseApi

class DeviceApi(private val service: DeviceService) : BaseApi(), DeviceApiSpec {

    override suspend fun registerDevice(registerDeviceRequestBody: RegisterDeviceRequestBody, username: String) =
        service.registerDevice(registerDeviceRequestBody, RetrofitTag(username))

    override suspend fun unregisterDevice(deviceToken: String) =
        service.unregisterDevice(deviceToken)
}
