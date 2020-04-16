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
package ch.protonmail.android.api.segments.reset

import ch.protonmail.android.api.models.MailboxResetBody
import ch.protonmail.android.api.models.ResetTokenResponse
import ch.protonmail.android.api.models.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE

interface ResetService {

    @POST("reset/mailbox")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchResetToken(): Call<ResetTokenResponse>

    @POST("reset/mailbox/{token}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun resetMailbox(@Path("token") token: String, @Body mailboxResetBody: MailboxResetBody): Call<ResponseBody>

}
