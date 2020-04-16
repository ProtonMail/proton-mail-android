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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager

import com.birbit.android.jobqueue.network.NetworkEventProvider
import com.birbit.android.jobqueue.network.NetworkUtil

import ch.protonmail.android.events.ConnectivityEvent
import ch.protonmail.android.utils.AppUtil

/**
 * Created by dkadrikj on 11/2/15.
 */
class QueueNetworkUtil(context: Context) : NetworkUtil, NetworkEventProvider {

    private var listener: NetworkEventProvider.Listener? = null
    private var isInternetAccessible: Boolean = false

    init {
        isInternetAccessible = true
        context.applicationContext.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (listener == null) {//shall not be but just be safe
                    return
                }
                // so in this moment, our hardware connectivity has changed
                if (hasConn(context, false)) {
                    // if we really have connectivity, then we are informing the queue to try to
                    // execute itself
                    listener!!.onNetworkChange(getNetworkStatus(context))
                    ProtonMailApplication.getApplication().startJobManager()
                }
            }
        }, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
    }

    fun isConnected(context: Context): Boolean {
        return hasConn(context, false)
    }

    fun isConnected(): Boolean {
        return hasConn(ProtonMailApplication.getApplication(), false)
    }

    fun isConnectedAndHasConnectivity(context: Context): Boolean {
        return hasConn(context, true)
    }

    fun setCurrentlyHasConnectivity(currentlyHasConnectivity: Boolean) {
        this.isInternetAccessible = currentlyHasConnectivity
    }

    override fun setListener(listener: NetworkEventProvider.Listener) {
        this.listener = listener
    }

    private fun hasConn(context: Context, checkReal: Boolean): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        var hasConnection = netInfo != null && netInfo.isConnectedOrConnecting
        if (checkReal) {
            hasConnection = hasConnection && isInternetAccessible
        }
        AppUtil.postEventOnUi(ConnectivityEvent(hasConnection))
        return hasConnection
    }

    override fun getNetworkStatus(context: Context): Int {
        val status = if (hasConn(context, true)) NetworkUtil.METERED else NetworkUtil.DISCONNECTED
        return status
    }
}
