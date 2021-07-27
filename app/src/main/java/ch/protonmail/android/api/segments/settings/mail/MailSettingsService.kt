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
package ch.protonmail.android.api.segments.settings.mail

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.requests.DisplayName
import ch.protonmail.android.api.models.requests.ShowImages
import ch.protonmail.android.api.models.requests.Signature
import ch.protonmail.android.api.models.requests.SwipeLeft
import ch.protonmail.android.api.models.requests.SwipeRight
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Tag

// TODO: 8/20/18 Better error handling, in a single place
interface MailSettingsService {

    @GET("mail/v4/settings")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchMailSettingsCall(): Call<MailSettingsResponse>

    @GET("mail/v4/settings")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun fetchMailSettings(@Tag userIdTag: UserIdTag): MailSettingsResponse

    @GET("mail/v4/settings")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchMailSettingsCall(@Tag userIdTag: UserIdTag): Call<MailSettingsResponse>

    @PUT("mail/v4/settings/display")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateDisplay(@Body displayName: DisplayName): Call<MailSettingsResponse>

    @PUT("mail/v4/settings/signature")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateSignature(@Body signature: Signature): Call<ResponseBody>

    @PUT("mail/v4/settings/swipeleft")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateLeftSwipe(@Body swipeLeft: SwipeLeft): Call<ResponseBody>

    @PUT("mail/v4/settings/swiperight")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateRightSwipe(@Body swipeRight: SwipeRight): Call<ResponseBody>

    @PUT("mail/v4/settings/images")
    fun updateAutoShowImages(@Body showImages: ShowImages): Call<ResponseBody>
}
