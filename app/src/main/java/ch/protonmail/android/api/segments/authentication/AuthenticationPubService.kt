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

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.interceptors.UsernameTag
import ch.protonmail.android.api.models.LoginBody
import ch.protonmail.android.api.models.LoginInfoBody
import ch.protonmail.android.api.models.LoginInfoResponse
import ch.protonmail.android.api.models.LoginResponse
import ch.protonmail.android.api.models.ModulusResponse
import ch.protonmail.android.api.models.RefreshBody
import ch.protonmail.android.api.models.RefreshResponse
import ch.protonmail.android.api.models.TwoFABody
import ch.protonmail.android.api.models.TwoFAResponse
import ch.protonmail.android.api.segments.RetrofitConstants
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Tag

interface AuthenticationPubService {

    @POST("auth")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun login(@Body loginBody: LoginBody): Call<LoginResponse>

    @GET("auth/modulus")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun randomModulus(): Call<ModulusResponse>

    @POST("auth/info")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun loginInfo(@Body infoBody: LoginInfoBody, @Tag usernameTag: UsernameTag): Call<LoginInfoResponse>

    @POST("auth/2fa")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun post2fa(@Body twoFaBody: TwoFABody, @Tag userIdTag: UserIdTag? = null): Call<TwoFAResponse>

    @POST("auth/refresh")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    suspend fun refreshAuth(
        @Body refreshBody: RefreshBody,
        @Tag userIdTag: UserIdTag? = null
    ): RefreshResponse

    @POST("auth/refresh")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun refreshAuthBlocking(
        @Body refreshBody: RefreshBody,
        @Tag userIdTag: UserIdTag
    ): Call<RefreshResponse>
}
