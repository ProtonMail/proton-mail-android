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
package ch.protonmail.android.api.segments.settings.user

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.UserSettingsResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.SrpResponseBody
import ch.protonmail.android.api.models.requests.NotificationEmail
import ch.protonmail.android.api.models.requests.PasswordChange
import ch.protonmail.android.api.models.requests.UpdateNotify
import ch.protonmail.android.api.models.requests.UpgradePasswordBody
import retrofit2.Call

import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PUT
import retrofit2.http.Tag

interface UserSettingsService {

    @GET("settings")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchUserSettings() : Call<UserSettingsResponse>

    @GET("settings")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchUserSettings(@Tag userIdTag: UserIdTag) : Call<UserSettingsResponse>

    @PUT("settings/email/notify")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateNotify(@Body updateNotify: UpdateNotify): Call<ResponseBody>

    @PUT("settings/email")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateNotificationEmail(@Body email: NotificationEmail): Call<SrpResponseBody>

    @PUT("settings/password")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateLoginPassword(@Body passwordChangeBody: PasswordChange): Call<SrpResponseBody>

    @PUT("settings/password/upgrade")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun upgradeLoginPassword(@Body passwordChangeBody: UpgradePasswordBody): Call<ResponseBody>

}
