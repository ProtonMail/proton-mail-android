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
package ch.protonmail.android.api.segments

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Created by dinokadrikj on 3/5/20.
 *
 * Used to define the DoH Retrofit request and response.
 */
interface DnsOverHttpsRetrofitApi {

    @Headers("Accept: application/dns-message")
    @GET
    suspend fun getServers(@Url url: String, @Query("dns") base64DnsMessage: String): Response<ResponseBody>
}
