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
package ch.protonmail.android.api

import ch.protonmail.android.di.AlternativeApiPins
import ch.protonmail.android.di.BaseUrl
import ch.protonmail.android.utils.crypto.ServerTimeInterceptor
import me.proton.core.network.data.ProtonCookieStore
import okhttp3.ConnectionSpec
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OkHttpProvider @Inject constructor(
    @AlternativeApiPins private val pinnedKeyHashes: List<String>,
    @BaseUrl private val baseUrl: String
) {
    // cache the clients, this way we can have separate client for every Uri/Url
    private val okHttpClients = HashMap<String, ProtonOkHttpClient>()

    /**
     * Decide in runtime which okhttp client to be returned
     */
    fun provideOkHttpClient(
        endpointUri: String,
        id: String = endpointUri,
        timeout: Long,
        interceptor: Interceptor,
        loggingLevel: HttpLoggingInterceptor.Level,
        connectionSpecs: List<ConnectionSpec>,
        serverTimeInterceptor: ServerTimeInterceptor?,
        cookieStore: ProtonCookieStore?
    ): ProtonOkHttpClient {
        if (okHttpClients.containsKey(id)) {
            return okHttpClients[id]!! // we can safely enforce here because we are sure it exists
        }
        okHttpClients[id] = if (endpointUri == baseUrl) {
            DefaultOkHttpClient(
                timeout,
                interceptor,
                loggingLevel,
                connectionSpecs,
                serverTimeInterceptor,
                baseUrl,
                cookieStore
            )
        } else {
            ProxyOkHttpClient(
                timeout,
                interceptor,
                loggingLevel,
                connectionSpecs,
                serverTimeInterceptor,
                endpointUri,
                pinnedKeyHashes,
                cookieStore
            )
        }
        return okHttpClients[id]!!
    }
}
