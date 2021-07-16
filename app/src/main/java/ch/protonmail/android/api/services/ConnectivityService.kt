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
package ch.protonmail.android.api.services

import android.annotation.TargetApi
import android.app.Service
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import ch.protonmail.android.api.segments.event.AlarmReceiver
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.receivers.ConnectivityBroadcastReceiver
import ch.protonmail.android.utils.AppUtil
import android.content.Intent

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ConnectivityService : JobService(), ConnectivityBroadcastReceiver.ConnectivityReceiverListener {

    private var mConnectivityReceiver: ConnectivityBroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        mConnectivityReceiver = ConnectivityBroadcastReceiver(this)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return Service.START_NOT_STICKY
    }

    override fun onStartJob(params: JobParameters?): Boolean {
        registerReceiver(mConnectivityReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        unregisterReceiver(mConnectivityReceiver)
        return true
    }

    override fun onNetworkConnectionChanged(isOnline: Boolean) {

        if (isOnline) {
            val alarmReceiver = AlarmReceiver()
            alarmReceiver.setAlarm(this)
            ProtonMailApplication.getApplication().startJobManager()
        }
    }
}
