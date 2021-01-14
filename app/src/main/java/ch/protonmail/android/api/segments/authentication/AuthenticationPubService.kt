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
package ch.protonmail.android.api.segments.authentication

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.*
import ch.protonmail.android.api.segments.RetrofitConstants
import retrofit2.Call
import retrofit2.http.*

interface AuthenticationPubService {

    @POST("auth")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun login(@Body loginBody: LoginBody): Call<LoginResponse>

    @GET("auth/modulus")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun randomModulus(): Call<ModulusResponse>

    @POST("auth/info")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun loginInfo(@Body infoBody: LoginInfoBody, @Tag retrofitTag: RetrofitTag? = null): Call<LoginInfoResponse>

    @POST("auth/2fa")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun post2fa(@Body twofaBody: TwoFABody, @Tag retrofitTag: RetrofitTag? = null): Call<TwoFAResponse>

    @POST("auth/refresh")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun refreshAuth(
        @Body refreshBody: RefreshBody,
        @Tag retrofitTag: RetrofitTag? = null
    ): RefreshResponse
}
