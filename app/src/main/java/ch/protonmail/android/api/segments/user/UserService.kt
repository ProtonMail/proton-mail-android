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
package ch.protonmail.android.api.segments.user

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.HumanVerifyOptionsResponse
import ch.protonmail.android.api.models.KeySalts
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.api.models.requests.PostHumanVerificationBody
import retrofit2.Call

import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Tag

interface UserService {

    // TODO: 2/26/18 token missing ???
    @GET("users/human")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchHumanVerificationOptions(): Call<HumanVerifyOptionsResponse>

    @GET("users")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchUserInfoCall(): Call<UserInfo>

    @GET("users")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun fetchUserInfo(): UserInfo

    @GET("users")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchUserInfoCall(@Tag retrofitTag: RetrofitTag): Call<UserInfo>

    @GET("keys/salts")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchKeySalts(): Call<KeySalts>

    @POST("users/human")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun postHumanVerification(@Body body: PostHumanVerificationBody): Call<ResponseBody>

}
