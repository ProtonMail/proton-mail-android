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
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.di.AppCoroutineScope
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.di.DohProviders
import ch.protonmail.android.utils.INetworkConfiguratorCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

// region constants
const val DOH_PROVIDER_TIMEOUT = 30_000L
// endregion

/**
 * NetworkConfigurator - used to retrieve and switch to alternative routing domains
 */
@Singleton
class NetworkConfigurator @Inject constructor(
    @DohProviders private val dohProviders: Array<DnsOverHttpsProviderRFC8484>,
    @DefaultSharedPreferences private val prefs: SharedPreferences,
    @AppCoroutineScope private val scope: CoroutineScope,
    private val userManager: UserManager,
    private val connectivityManager: NetworkConnectivityManager
) {

    lateinit var networkSwitcher: INetworkSwitcher
    private var isRunning = false
    private var callback: INetworkConfiguratorCallback? = null

    fun refreshDomainsAsync() = scope.launch {
        if (!isRunning) {
            isRunning = true
            callback?.startDohSignal()
            queryDomains()
        }
    }

    fun tryRetryWithDoh() {
        if (connectivityManager.isInternetConnectionPossible()) {
            val isThirdPartyConnectionsEnabled = userManager.currentLegacyUser?.allowSecureConnectionsViaThirdParties
            if (isThirdPartyConnectionsEnabled == true) {
                Timber.i("Third party connections enabled, attempting DoH...")
                refreshDomainsAsync()
            }
        }
    }

    private suspend fun queryDomains() {
        val freshAlternativeUrls = mutableListOf<String>()
        val user = userManager.requireCurrentLegacyUser()
        if (!user.allowSecureConnectionsViaThirdParties) {
            networkSwitcher.reconfigureProxy(null) // force switch to old proxy
            user.usingDefaultApi = true
            isRunning = false
            callback?.stopDohSignal()
            return
        }
        for (provider in dohProviders) {
            // 0 is quad9, 1 is google, 2 is cloudflare
            val success = withTimeoutOrNull(DOH_PROVIDER_TIMEOUT) {
                val result: List<String>? = try {
                    provider.getAlternativeBaseUrls()
                } catch (ioException: IOException) {
                    Timber.w(ioException, "DoH getAlternativeBaseUrls error!")
                    null
                }
                if (result != null) {
                    freshAlternativeUrls.addAll(result)
                }
                result != null
            }

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
        scope.launch {
            val user = userManager.requireCurrentLegacyUser()
            // double-check if normal API call works before resorting to use alternative routing url
            if (user.usingDefaultApi) {
                val success = withTimeoutOrNull(DOH_PROVIDER_TIMEOUT) {
                    val result = try {
                        networkSwitcher.tryRequest()
                    } catch (e: Exception) {
                        Timber.i(e, "Exception while pinging API before using alternative routing")
                        null
                    }
                    result != null
                }
                if (success == true) {
                    callback?.stopAutoRetry()
                    networkSwitcher.reconfigureProxy(null)
                    isRunning = false
                    callback?.stopDohSignal()
                    return@launch
                }
            }

            proxyListReference.forEach {
                proxies.updateActive(it.baseUrl, timestamp)
                networkSwitcher.reconfigureProxy(proxies)
                val success = withTimeoutOrNull(DOH_PROVIDER_TIMEOUT) {
                    val result = try {
                        networkSwitcher.tryRequest()
                    } catch (e: Exception) {
                        Timber.i(e, "Exception while pinging alternative routing URL")
                        null
                    }
                    result != null // && result.code == 1000
                }
                if (success == true) {
                    proxies.updateSuccess(it.baseUrl, timestamp)
                    proxies.saveCurrentWorkingProxyDomain(proxies.getCurrentActiveProxy().baseUrl)
                    proxies.save()
                    isRunning = false
                    user.usingDefaultApi = false
                    callback?.startAutoRetry()
                    callback?.stopDohSignal()
                    return@launch
                } else {
                    proxies.updateFailed(it.baseUrl, timestamp)
                }
            }
            callback?.stopAutoRetry()
            networkSwitcher.reconfigureProxy(null)
            user.usingDefaultApi = true
            isRunning = false
            callback?.stopDohSignal()
            callback?.onDohFailed()
        }
    }

    fun setNetworkConfiguratorCallback(callback: INetworkConfiguratorCallback) {
        this.callback = callback
    }

    fun removeNetworkConfiguratorCallback() {
        callback = null
    }

}
