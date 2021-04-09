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
package ch.protonmail.android.api.models.doh

import android.content.SharedPreferences
import ch.protonmail.android.core.Constants
import com.google.gson.Gson

// region constants
const val PREF_DNS_OVER_HTTPS_API_URL_LIST = "pref_dns_over_https_api_url_list"
const val PREF_DNS_OVER_HTTPS_API_URL = "pref_dns_over_https_api_url"
// endregion


class Proxies constructor(
    val proxyList: ProxyList,
    val prefs: SharedPreferences
) {

    var isDohActive: Boolean = false

    // init {
    //     save()
    // }

    fun getNextProxy(): ProxyItem {
        return proxyList.proxies.first {
            !it.active && it.success == null && it.lastTrialTimestamp == 0L
        }
    }

    /**
     * Returns the currently active proxy (active proxy doesn't mean that it works successfully, but
     * only that the network is set to work with it).
     */
    fun getCurrentActiveProxy(): ProxyItem {
        return proxyList.proxies.first {
            it.active
        }
    }

    fun updateFailed(baseUrl: String, timestamp: Long) {
        val proxyBaseUrl = proxyList.proxies.first {
            it.baseUrl == baseUrl
        }
        val index = proxyList.proxies.indexOf(proxyBaseUrl)
        (proxyList.proxies as MutableList)[index] = proxyBaseUrl.copy(active = false, lastTrialTimestamp = timestamp, success = false)
        save()
    }

    fun updateActive(baseUrl: String, timestamp: Long) {
        val proxyBaseUrl = proxyList.proxies.first {
            it.baseUrl == baseUrl
        }
        val index = proxyList.proxies.indexOf(proxyBaseUrl)
        (proxyList.proxies as MutableList)[index] = proxyBaseUrl.copy(active = true, lastTrialTimestamp = timestamp)
        save()
    }

    fun updateSuccess(baseUrl: String, timestamp: Long) {
        val proxyBaseUrl = proxyList.proxies.first {
            it.baseUrl == baseUrl
        }
        val index = proxyList.proxies.indexOf(proxyBaseUrl)
        (proxyList.proxies as MutableList)[index] = proxyBaseUrl.copy(active = true, lastTrialTimestamp = timestamp, success = true)
        save()
    }

    fun save() {
        prefs.edit().putString(PREF_DNS_OVER_HTTPS_API_URL_LIST, Gson().toJson(proxyList)).apply()
    }

    fun saveCurrentWorkingProxyDomain(domain: String) {
        prefs.edit().putString(PREF_DNS_OVER_HTTPS_API_URL, domain).apply()
    }

    fun getCurrentWorkingProxyDomain(): String {
        return prefs.getString(PREF_DNS_OVER_HTTPS_API_URL, Constants.ENDPOINT_URI)!!
    }

    companion object {
        @Volatile private var INSTANCE: Proxies? = null

        /**
         * If you supply proxyList, this mean that it will overwrite the current saved proxies completely.
         * If you want to only get what has already been saved, just do not use that parameter.
         */
        fun getInstance(proxyList: ProxyList? = null, prefs: SharedPreferences): Proxies {
            return INSTANCE ?: synchronized(this) {
                INSTANCE
                    ?: if (proxyList != null) {
                        return Proxies(proxyList, prefs)
                    }
                val storedValue = prefs.getString(PREF_DNS_OVER_HTTPS_API_URL_LIST, null)
                val proxyItems = if (storedValue != null) {
                    Gson().fromJson(storedValue, ProxyList::class.java)
                } else {
                    ProxyList(emptyList())
                }
                Proxies(proxyItems, prefs)
            }
        }
    }
}

data class ProxyList(val proxies: List<ProxyItem>) {
    constructor() : this(emptyList<ProxyItem>())
}
