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
package ch.protonmail.android.api.segments.address

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.address.AddressOrder
import ch.protonmail.android.api.models.address.AddressSetupBody
import ch.protonmail.android.api.models.address.AddressSetupResponse
import ch.protonmail.android.api.models.address.AddressesResponse
import ch.protonmail.android.api.models.address.CondensedAddress
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Tag

interface AddressService {

    @GET("addresses")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchAddressesCall(): Call<AddressesResponse>

    @GET("addresses")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun fetchAddresses(): AddressesResponse

    @GET("addresses")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchAddressesCall(@Tag retrofitTag: RetrofitTag): Call<AddressesResponse>

    @POST("addresses/setup")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun setupAddress(@Body addressSetupBody: AddressSetupBody): Call<AddressSetupResponse>

    @PUT("addresses/{addressid}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun editAddress(@Path("addressid") addressId: String, @Body condensedAddress: CondensedAddress): Call<ResponseBody>

    @PUT("addresses/order")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateAddressOrder(@Body order: AddressOrder): Call<ResponseBody>

}
