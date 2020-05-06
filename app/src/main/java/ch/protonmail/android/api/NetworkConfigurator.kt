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
import ch.protonmail.android.api.models.doh.ProxyItem
import ch.protonmail.android.api.models.doh.ProxyList
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.utils.INetworkConfiguratorCallback
import ch.protonmail.android.utils.Logger
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

// region constants
const val DOH_PROVIDER_TIMEOUT = 10000L
private const val TAG = "NetworkConfigurator"
// endregion

/**
 * Created by dinokadrikj on 3/3/20.
 */
class NetworkConfigurator(
        private val dohProviders: Array<DnsOverHttpsProviderRFC8484>,
        private val prefs: SharedPreferences
) {

    lateinit var networkSwitcher: INetworkSwitcher
    private var isRunning = false

    fun refreshDomainsAsync() = GlobalScope.async {
        if (!isRunning) {
            Logger.doLog(TAG, "Setting running to true")
            isRunning = true
            callback?.startDohSignal()
            queryDomains()
        }
    }

    private suspend fun queryDomains() {
        val freshAlternativeUrls = mutableListOf<String>()
        val user = ProtonMailApplication.getApplication().userManager.user
        if (user.allowSecureConnectionsViaThirdParties && !user.usingDefaultApi) {
            networkSwitcher.reconfigureProxy(null) // force switch to old proxy
            user.usingDefaultApi = true
            isRunning = false
            callback?.stopDohSignal()
            return
        }
        for (provider in dohProviders) {
            Logger.doLog(TAG, "Querying provider: " + dohProviders.indexOf(provider)) // 0 is quad9, 1 is google, 2 is cloudflare
            val success = withTimeoutOrNull(DOH_PROVIDER_TIMEOUT) {
                val result = try {
                    provider.getAlternativeBaseUrls()
                } catch (e: Exception) {
                    null
                }
                if (result != null) {
                    Logger.doLog(TAG, "Result is: $result")
                    freshAlternativeUrls.addAll(result)
                }
                result != null
            }
            // TODO: think if this is needed, rethink about using break at all
            if (success == true) {
                Logger.doLog(TAG, "Success")
                break
            }
        }

        val currentTimestamp = System.currentTimeMillis()
        val alternativeProxyList = freshAlternativeUrls.map {
            ProxyItem(it, 0L, null, false)
        }

        val proxies =
        if (alternativeProxyList.isEmpty()) {
            // if the new list is empty, try with the old list perhaps there will be an api url available there
            Logger.doLog(TAG, "New DoH list is empty, trying with cached alternative proxy urls")
            Proxies.getInstance(null, prefs) // or just try the last used proxy
        } else {
            Proxies.getInstance(ProxyList(alternativeProxyList), prefs)
        }
        // val proxies = Proxies.getInstance(ProxyList(alternativeProxyList), prefs) // supplying proxy list means that the savings will be invalidated
        findWorkingDomain(proxies, currentTimestamp)
    }

    fun stopAutoRetry() {
        callback?.stopAutoRetry()
    }

    fun startAutoRetry() {
        callback?.startAutoRetry()
    }

    private fun findWorkingDomain(proxies: Proxies, timestamp: Long) {
        val proxyListReference = proxies.proxyList.proxies
        GlobalScope.launch {
            proxyListReference.forEach {
                Logger.doLog(TAG, "ProxyItem baseUrl is: " + it.baseUrl)
                proxies.updateActive(it.baseUrl, timestamp)
                networkSwitcher.reconfigureProxy(proxies)
                val success = withTimeoutOrNull(DOH_PROVIDER_TIMEOUT) {
                    val result = try {
                        Logger.doLog(TAG, "Before ping async")
                        networkSwitcher.tryRequest { service ->
                            service.pingAsync()
                        }
                        Logger.doLog(TAG, "Pinged async")
                    } catch (e: Exception) {
                        Logger.doLog(TAG, "Pinged async: exception")
                        null
                    }
                    result != null // && result.code == 1000
                }
                if (success == true) {
                    Logger.doLog(TAG, "Working alternative api url found")
                    // now we have working api
                    proxies.updateSuccess(it.baseUrl, timestamp)
                    // update api url that we are currently using
                    proxies.saveCurrentWorkingProxyDomain(proxies.getCurrentActiveProxy().baseUrl)
                    proxies.save()
                    Logger.doLog(TAG, "Success: setting DOH running to: false")
                    isRunning = false
                    Logger.doLog(TAG, "Success: setting usingOldApi to: false")
                    ProtonMailApplication.getApplication().userManager.user.usingDefaultApi = false
                    callback?.startAutoRetry()
                    callback?.stopDohSignal()
                    return@launch
                } else {
                    Logger.doLog(TAG, "Alternative api url NOT working")
                    // try with other
                    proxies.updateFailed(it.baseUrl, timestamp)
                }
            }
            callback?.stopAutoRetry()
            //
            networkSwitcher.reconfigureProxy(null) // TODO: DoH, if all the new proxies failed, revert back to using the old api
            Logger.doLog(TAG, "Failure: setting usingOldApi to: false") //
            ProtonMailApplication.getApplication().userManager.user.usingDefaultApi = true // TODO: DoH, should we use this here? maybe to true, since we can't use the new api anyways...
            Logger.doLog(TAG, "Failure: setting DOH running to: false")
            isRunning = false
            callback?.stopDohSignal()
        }
    }

    companion object {
        var callback : INetworkConfiguratorCallback? = null

        fun setNetworkConfiguratorCallback(callback: INetworkConfiguratorCallback) {
            this.callback = callback
        }
    }
}