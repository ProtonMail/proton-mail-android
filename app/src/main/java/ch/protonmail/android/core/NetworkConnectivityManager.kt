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

package ch.protonmail.android.core

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Monitors active network connection using [NetworkConnectivityManager].
 */
class NetworkConnectivityManager @Inject constructor(
    private val connectivityManager: ConnectivityManager
) {

    fun isInternetConnectionPossible(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        return capabilities.hasVerifiedInternet()
    }

    /**
     * Flow of boolean connection events. True when internet connection is established, false when it is lost.
     */
    fun isConnectionAvailableFlow(): Flow<Constants.ConnectionState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Timber.v("Network $network available")
            }

            override fun onLost(network: Network) {
                launch {
                    delay(2.toDuration(DurationUnit.SECONDS))
                    Timber.d("Network $network lost isInternetPossible: ${isInternetConnectionPossible()}")
                    if (!isInternetConnectionPossible()) {
                        trySend(Constants.ConnectionState.NO_INTERNET)
                    }
                }
            }

            override fun onUnavailable() {
                Timber.v("Network Unavailable")
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                if (networkCapabilities.hasVerifiedInternet()) {
                    Timber.v("Network $network has internet capability")
                    trySend(Constants.ConnectionState.CONNECTED)
                }
            }
        }

        val networkRequestBuilder = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(networkRequestBuilder, callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }

    fun isConnectedToVpn(): Boolean {
        return connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            ?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
    }

    private fun NetworkCapabilities?.hasVerifiedInternet(): Boolean =
        this?.hasCapability(NET_CAPABILITY_INTERNET) == true && this.hasCapability(NET_CAPABILITY_VALIDATED)
}
