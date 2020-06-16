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
import kotlinx.coroutines.*
import timber.log.Timber

// region constants
const val DOH_PROVIDER_TIMEOUT = 20_000L
private const val TAG = "NetworkConfigurator"
// endregion

/**
 * Created by dinokadrikj on 3/3/20.
 */

/**
 * NetworkConfigurator - used to retrieve and switch to alternative routing domains
 */
class NetworkConfigurator(
    private val dohProviders: Array<DnsOverHttpsProviderRFC8484>,
    private val prefs: SharedPreferences,
    private val scope: CoroutineScope = GlobalScope
) {

    lateinit var networkSwitcher: INetworkSwitcher
    private var isRunning = false

    fun refreshDomainsAsync() = scope.async {
        if (!isRunning) {
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
            // 0 is quad9, 1 is google, 2 is cloudflare
            val success = withTimeoutOrNull(DOH_PROVIDER_TIMEOUT) {
                val result = try {
                    provider.getAlternativeBaseUrls()
                } catch (e: Exception) {
                    null
                }
                if (result != null) {
                    freshAlternativeUrls.addAll(result)
                }
                result != null
            }
            // TODO: think if this is needed, rethink about using break at all
            if (success == true) {
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
            Proxies.getInstance(null, prefs) // or just try the last used proxy
        } else {
            Proxies.getInstance(ProxyList(alternativeProxyList), prefs)
        }

        // supplying proxy list means that the savings will be invalidated
        // val proxies = Proxies.getInstance(ProxyList(alternativeProxyList), prefs)
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

            // double-check if normal API call works before resorting to use alternative routing url
            val success = withTimeoutOrNull(DOH_PROVIDER_TIMEOUT) {
                val result = try {
                    networkSwitcher.tryRequest { service ->
                        service.pingAsync()
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Exception while pinging normal API before using alternative routing")
                    null
                }
                result != null
            }
            if (success == true) {
                callback?.stopAutoRetry()
                networkSwitcher.reconfigureProxy(null)
                ProtonMailApplication.getApplication().userManager.user.usingDefaultApi = true
                isRunning = false
                callback?.stopDohSignal()
                return@launch
            }

            proxyListReference.forEach {
                proxies.updateActive(it.baseUrl, timestamp)
                networkSwitcher.reconfigureProxy(proxies)
                val success = withTimeoutOrNull(DOH_PROVIDER_TIMEOUT) {
                    val result = try {
                        networkSwitcher.tryRequest { service ->
                            service.pingAsync()
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Exception while pinging alternative routing URL")
                        null
                    }
                    result != null // && result.code == 1000
                }
                if (success == true) {
                    proxies.updateSuccess(it.baseUrl, timestamp)
                    proxies.saveCurrentWorkingProxyDomain(proxies.getCurrentActiveProxy().baseUrl)
                    proxies.save()
                    isRunning = false
                    ProtonMailApplication.getApplication().userManager.user.usingDefaultApi = false
                    callback?.startAutoRetry()
                    callback?.stopDohSignal()
                    return@launch
                } else {
                    proxies.updateFailed(it.baseUrl, timestamp)
                }
            }
            callback?.stopAutoRetry()
            networkSwitcher.reconfigureProxy(null)
            ProtonMailApplication.getApplication().userManager.user.usingDefaultApi = true
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
