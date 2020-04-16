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
package ch.protonmail.android.api.segments.report

import ch.protonmail.android.api.models.BugsBody
import ch.protonmail.android.api.models.PostPhishingReportBody
import ch.protonmail.android.api.models.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE

interface ReportService {

    @POST("reports/bug")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun bugs(@Body bugsBody: BugsBody): Call<ResponseBody>

    @POST("reports/phishing")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun postPhishingReport(@Body postPhishingReportBody: PostPhishingReportBody): Call<ResponseBody>

}
