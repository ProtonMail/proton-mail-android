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

import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.segments.event.EventManager
import ch.protonmail.android.di.BaseUrl
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface INetworkSwitcher {
    fun reconfigureProxy(proxies: Proxies?)
    suspend fun tryRequest()
}

@Singleton
class NetworkSwitcher @Inject constructor(
    private val api: ProtonMailApiManager,
    private val apiProvider: ProtonMailApiProvider,
    private val protonOkHttpProvider: OkHttpProvider,
    @BaseUrl private val baseUrl: String,
    networkConfigurator: NetworkConfigurator,
    private val eventManager: EventManager
) : INetworkSwitcher {

    init {
        networkConfigurator.networkSwitcher = this
    }

    /**
     * This method is used to reconfigure the underlying OkHttp/Retrofit instances to work with 3rd
     * party proxies.
     */
    override fun reconfigureProxy(proxies: Proxies?) { // TODO: DoH this can be done without null
        val proxyItem = proxies?.getCurrentActiveProxy()?.baseUrl ?: baseUrl
        Timber.v("proxyItem url is: $proxyItem")
        val newApi: ProtonMailApi = apiProvider.rebuild(protonOkHttpProvider, proxyItem)
        api.reset(newApi)
        eventManager.reconfigure(newApi.securedServices.event)
    }

    override suspend fun tryRequest() {
        api.ping()
    }
}
