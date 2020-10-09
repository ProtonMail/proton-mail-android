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

package ch.protonmail.android.core

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Monitors active network connection using [NetworkConnectivityManager].
 */
class NetworkConnectivityManager @Inject constructor(
    private val connectivityManager: ConnectivityManager
) {

    fun isInternetConnectionPossible(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            isActiveNetworkInternetCapable()
        else
            isCurrentlyConnected()
    }

    private fun isCurrentlyConnected(): Boolean =
        connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true

    @RequiresApi(Build.VERSION_CODES.M)
    private fun isActiveNetworkInternetCapable(): Boolean {
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        return capabilities?.hasCapability(NET_CAPABILITY_INTERNET) == true
    }

    /**
     * Flow of boolean connection events. True when internet connection is established, false when it is lost.
     */
    fun isConnectionAvailableFlow(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network?) {
                Timber.v("Network $network available")
            }

            override fun onLost(network: Network?) {
                Timber.d("Network $network lost isInternetPossible: ${isInternetConnectionPossible()}")
                if (!isInternetConnectionPossible()) {
                    offer(false)
                }
            }

            override fun onUnavailable() {
                Timber.v("Network Unavailable")
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                if (networkCapabilities.hasCapability(NET_CAPABILITY_INTERNET)) {
                    Timber.v("Network $network has internet capability")
                    offer(true)
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
    }
}
