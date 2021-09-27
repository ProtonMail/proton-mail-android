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
package ch.protonmail.android.api.segments.payment

import ch.protonmail.android.api.models.AvailablePlansResponse
import ch.protonmail.android.api.models.CreatePaymentTokenBody
import ch.protonmail.android.api.models.CreatePaymentTokenSuccessResponse
import ch.protonmail.android.api.models.GetPaymentTokenResponse
import ch.protonmail.android.api.models.VerifyBody
import ch.protonmail.android.api.models.VerifyResponse
import ch.protonmail.android.api.segments.RetrofitConstants
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface PaymentPubService {

    @GET("payments/plans")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun fetchAvailablePlans(
        @Query("Currency") currency: String,
        @Query("Cycle") cycle: Int
    ): Call<AvailablePlansResponse>

    @POST("payments/verify")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun verifyPayment(@Body body: VerifyBody): Call<VerifyResponse>

    @POST("payments/tokens")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun createPaymentToken(
        @Body body: CreatePaymentTokenBody,
        @Header("X-PM-Human-Verification-Token") token: String?,
        @Header("X-PM-Human-Verification-Token-Type") tokenType: String?
    ): Call<CreatePaymentTokenSuccessResponse>

    @GET("payments/tokens/{token}")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun getPaymentToken(@Path("token") token: String): Call<GetPaymentTokenResponse>

}
