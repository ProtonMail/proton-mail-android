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
package ch.protonmail.android.api.segments.key

import ch.protonmail.android.api.models.KeysSetupBody
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.SinglePasswordChange
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.api.models.address.AddressPrivateKey
import ch.protonmail.android.api.models.address.KeyActivationBody
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import ch.protonmail.android.domain.entity.user.AddressKey
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface KeyService {

    @GET("keys")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun getPublicKeysBlocking(@Query("Email") email: String): Call<PublicKeyResponse>

    @GET("keys")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun getPublicKeys(@Query("Email") email: String): PublicKeyResponse

    @PUT("keys/private")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updatePrivateKeys(@Body privateKeyBody: SinglePasswordChange): Call<ResponseBody>

    // migrated user
    @PUT("keys/address/{addressId}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun activateKey(
        @Body keyActivationBody: KeyActivationBody,
        @Path("addressId") keyId: String
    ): Call<ResponseBody>

    // legacy user
    @PUT("/keys/{keyId}/activate")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun activateKeyLegacy(
        @Body keyActivationBody: KeyActivationBody,
        @Path("keyId") keyId: String
    ): ResponseBody

    @POST("keys/setup")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun setupKeys(@Body keysSetupBody: KeysSetupBody): Call<UserInfo>

    // TODO: not implemented yet (evaluation necessary for mail android client)
    @PUT("/keys/private/upgrade")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun upgradePrivateKeys(@Body privateKeyBody: SinglePasswordChange): Call<ResponseBody>

    @POST("/keys/reset")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun resetKeys(@Body keysSetupBody: KeysSetupBody): Call<ResponseBody>

    @POST("/keys/address")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun createAddressKey(@Body addressPrivateKey: AddressPrivateKey): Call<ResponseBody>

    @POST("/keys")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun createAddressKeyLegacy(@Body addressPrivateKey: AddressPrivateKey): Call<ResponseBody>

    @PUT("/keys/user/{userkeyid}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun reactivateAddressKeys(@Body addressPrivateKey: AddressPrivateKey): Call<ResponseBody>

    @PUT("/keys/{keyid}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun reactivateAddressKeysLegacy(@Body addressPrivateKey: AddressPrivateKey): Call<ResponseBody>

    // probably not necessary for mail android client
    @POST("/members/{memberid}/keys")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun createMembersAddressKeys(@Body addressKey: AddressKey): Call<ResponseBody>

    @POST("/members/{memberid}/keys/setup")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun setupMembersKeys(@Body keysSetupBody: KeysSetupBody): Call<UserInfo>

}
