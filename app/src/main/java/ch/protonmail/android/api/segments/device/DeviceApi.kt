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
import ch.protonmail.android.api.models.RegisterDeviceBody
import ch.protonmail.android.api.segments.BaseApi
import java.io.IOException

class DeviceApi(private val service: DeviceService) : BaseApi(), DeviceApiSpec {

    @Throws(IOException::class)
    override fun registerDevice(registerDeviceBody: RegisterDeviceBody, username: String) {
        service.registerDevice(registerDeviceBody, RetrofitTag(username)).execute()
    }

    @Throws(IOException::class)
    override fun unregisterDeviceBlocking(deviceToken: String) {
        service.unregisterDeviceBlocking(deviceToken).execute()
    }

    override suspend fun unregisterDevice(deviceToken: String) =
        service.unregisterDevice(deviceToken)
}
