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
package ch.protonmail.android.api.segments.event

import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.EventResponse
import ch.protonmail.android.api.models.LatestEventResponse
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Tag

interface EventService {

    @GET("events/{eventId}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun check(@Path("eventId") eventId: String, @Tag idTag: UserIdTag): EventResponse

    @GET("events/latest")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun latestId(@Tag idTag: UserIdTag): LatestEventResponse
}
