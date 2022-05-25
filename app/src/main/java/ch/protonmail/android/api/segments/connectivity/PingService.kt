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
package ch.protonmail.android.api.segments.connectivity

import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers

/**
 * ProtonMailServices
 * Provides APIs which have naming policy for parameters as UPPER_CAMEL_CASE
 */
interface PingService {

    @GET("tests/ping")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun ping(): Response<ResponseBody>
}
