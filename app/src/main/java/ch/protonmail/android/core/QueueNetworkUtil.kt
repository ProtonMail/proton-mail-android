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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.utils.extensions.isCanceledRequestException
import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import java.net.SocketTimeoutException
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton
import javax.net.ssl.SSLException

private const val DISCONNECTION_EMISSION_WINDOW_MS = 20_000

@Singleton
class QueueNetworkUtil @Inject constructor(
    private val context: Context,
    internal val networkConfigurator: NetworkConfigurator
) : NetworkUtil, NetworkEventProvider {

    private var listener: NetworkEventProvider.Listener? = null
    private var isServerAccessible: Boolean = true
    private var lastEmissionTime = 0L

    val connectionStateFlow: StateFlow<Constants.ConnectionState>
        get() = backendExceptionFlow

    private val backendExceptionFlow = MutableStateFlow(Constants.ConnectionState.CONNECTED)

    init {
        updateRealConnectivity(true) // initially we assume there is connectivity
        context.applicationContext.registerReceiver(
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (listener == null) {
                        return
                    }
                    // so in this moment, our hardware connectivity has changed
                    if (hasConn(false)) {
                        // if we really have connectivity, then we are informing the queue to try to
                        // execute itself
                        listener?.onNetworkChange(getNetworkStatus(context))
                        ProtonMailApplication.getApplication().startJobManager()
                    }
                }
            },
            IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        )
    }

    @Synchronized
    fun updateRealConnectivity(
        serverAccessible: Boolean,
        connectionState: Constants.ConnectionState = Constants.ConnectionState.CONNECTED
    ) {
        isServerAccessible = serverAccessible

        if (serverAccessible) {
            Timber.v("Server is connected")
            backendExceptionFlow.value = Constants.ConnectionState.CONNECTED
        } else {
            // to prevent consecutive series of disconnection emissions we introduce a disconnection
            // emission buffer below
            val currentTime = System.currentTimeMillis()
            val emissionTimeDelta = currentTime - lastEmissionTime
            Timber.v(
                "updateRealConnectivity isServerAccessible: $serverAccessible timeDelta: $emissionTimeDelta $connectionState"
            )
            val mayEmit = emissionTimeDelta > DISCONNECTION_EMISSION_WINDOW_MS
            if (mayEmit) {
                lastEmissionTime = currentTime
                backendExceptionFlow.value = connectionState
            }
        }

        // Lets the job manager know if the server is reachable again as soon as we update the connectivity;
        // this way, the jobs that require network will run as soon as we have decided the server is in fact reachable
        listener?.onNetworkChange(getNetworkStatus(context))
    }

    fun isConnected(): Boolean = hasConn(false)

    fun setCurrentlyHasConnectivity() = updateRealConnectivity(true)

    fun retryPingAsPreviousRequestWasInconclusive(exception: Exception? = null) {
        if (exception == null || !exception.isCanceledRequestException()) {
            updateRealConnectivity(false, Constants.ConnectionState.PING_NEEDED)
        }
    }

    fun setConnectivityHasFailed(throwable: Throwable) {
        // for valid failure types specified below
        // the connections should be declared as failure which
        // should be followed by no connection snackbar and DoH setup
        when (throwable) {
            is SocketTimeoutException,
            is GeneralSecurityException, // e.g. CertificateException
            is SSLException -> updateRealConnectivity(false, Constants.ConnectionState.CANT_REACH_SERVER)
            else -> Timber.d("connectivityHasFailed ignoring exception: $throwable")
        }
    }

    override fun setListener(netListener: NetworkEventProvider.Listener) {
        listener = netListener
    }

    @Synchronized
    private fun hasConn(checkReal: Boolean): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        var hasConnection = netInfo != null && netInfo.isConnected
        val currentStatus = isServerAccessible
        if (checkReal) {
            hasConnection = hasConnection && isServerAccessible
        }
        if (checkReal && currentStatus != hasConnection) {
            Timber.d("Network statuses differs hasConnection $hasConnection currentStatus $currentStatus")
        }
        return hasConnection
    }

    override fun getNetworkStatus(context: Context): Int =
        if (hasConn(true)) NetworkUtil.METERED else NetworkUtil.DISCONNECTED
}
