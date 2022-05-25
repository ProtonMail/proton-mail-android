/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.api

import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.segments.event.EventManager
import ch.protonmail.android.di.BaseUrl
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkSwitcher @Inject constructor(
    private val api: ProtonMailApiManager,
    private val apiProvider: ProtonMailApiProvider,
    private val protonOkHttpProvider: OkHttpProvider,
    @BaseUrl private val baseUrl: String,
    private val eventManager: EventManager
) {

    /**
     * This method is used to reconfigure the underlying OkHttp/Retrofit instances to work with 3rd
     * party proxies.
     */
    fun reconfigureProxy(proxies: Proxies?) { // TODO: DoH this can be done without null
        val proxyItem = proxies?.getCurrentActiveProxy()?.baseUrl ?: baseUrl
        Timber.v("proxyItem url is: $proxyItem")
        val newApi: ProtonMailApi = apiProvider.rebuild(protonOkHttpProvider, proxyItem)
        api.reset(newApi)
        eventManager.reconfigure(newApi.securedServices.event)
    }

    suspend fun tryRequest() {
        api.ping()
    }
}
