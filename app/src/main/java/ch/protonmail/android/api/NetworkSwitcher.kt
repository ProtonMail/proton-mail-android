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

import android.content.SharedPreferences
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.segments.connectivity.ConnectivityApi
import ch.protonmail.android.api.segments.connectivity.PingService
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.utils.Logger
import kotlinx.coroutines.runBlocking
import retrofit2.Response

interface INetworkSwitcher {
    fun reconfigureProxy(proxies: Proxies?)
    fun <T> tryRequest(callFun: suspend (PingService) -> Response<T>)
}

/**
 * Created by dinokadrikj on 3/6/20.
 */
class NetworkSwitcher(
        private val api: ProtonMailApiManager,
        private val apiProvider: ProtonMailApiProvider,
        private val protonOkHttpProvider: OkHttpProvider,
        private val preferences: SharedPreferences): INetworkSwitcher {

    /**
     * This method is used to reconfigure the underlying OkHttp/Retrofit instances to work with 3rd
     * party proxies.
     */
    override fun reconfigureProxy(proxies: Proxies?) { // TODO: DoH this can be done without null
        val proxyItem = proxies?.getCurrentActiveProxy()?.baseUrl ?: Constants.ENDPOINT_URI
        Logger.doLog("NetworkSwitcher", "proxyItem url is: $proxyItem")
        val newApi: ProtonMailApi = apiProvider.rebuild(protonOkHttpProvider, proxyItem)
        api.reset(newApi)
        ProtonMailApplication.getApplication().eventManager.reconfigure(newApi.securedServices.event)
    }

    override fun <T> tryRequest(callFun: suspend (PingService) -> Response<T>) {
        runBlocking {
            // this is a bit awkward, but it is fine for now
            callFun.invoke((api.api.connectivityApi as ConnectivityApi).pingService)
        }
    }
}