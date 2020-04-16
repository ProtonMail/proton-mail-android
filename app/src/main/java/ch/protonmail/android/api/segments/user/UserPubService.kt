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

import ch.protonmail.android.api.models.*
import ch.protonmail.android.api.segments.RetrofitConstants
import retrofit2.Call
import retrofit2.http.*

// region constants
private const val QUERY_USERNAME = "Name"
// endregion

interface UserPubService {

    @GET("users/direct")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun fetchDirectEnabled(@Query("Type") type: Int): Call<DirectEnabledResponse>

    @POST("users")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun createUser(@Body createUserBody: CreateUserBody): Call<UserInfo>

    @POST("users/code")
    fun sendVerificationCode(@Body verificationCodeBody: VerificationCodeBody): Call<ResponseBody>

    @GET("users/available")
    @Headers(RetrofitConstants.CONTENT_TYPE, RetrofitConstants.ACCEPT_HEADER_V1)
    fun isUsernameAvailable(@Query(QUERY_USERNAME) username: String): Call<ResponseBody>

}
