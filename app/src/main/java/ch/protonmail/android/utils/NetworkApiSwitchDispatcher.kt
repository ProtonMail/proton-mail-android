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
package ch.protonmail.android.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import ch.protonmail.android.BuildConfig
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.libs.core.utils.EMPTY_STRING
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit

// region constants
private const val TWO_MINUTES_IN_MILLIS = 2 * 60 * 1000L
private const val TAG = "NetworkApiSwitchDispatcher"
private const val WORKER_TAG = "NetworkApiSwitchWorkerTag"
private const val ARBITRARY_NON_IMMEDIATE_DELAY = 500L
private const val PREF_DNS_OVER_HTTPS_API_SWITCH_WORKER_ID = "pref_dns_over_https_api_switch_worker_id"
private const val PREF_DNS_OVER_HTTPS_API_SWITCH_USER_CANCELED = "pref_24_hrs_switch_cancelled_by_user"
private const val TWENTY_FOUR_HOURS_IN_MILLIS = 24 * 60 * 60 * 1000L
// endregion

class NetworkApiSwitchDispatcher(context : Context) {

    private val mContext = context
    private val appContext = ProtonMailApplication.getApplication().applicationContext
    private val switchInterval = if(BuildConfig.DEBUG) TWO_MINUTES_IN_MILLIS else TWENTY_FOUR_HOURS_IN_MILLIS

    val prefs = ProtonMailApplication.getApplication().defaultSharedPreferences
    private val workManager = WorkManager.getInstance(mContext)

    @SuppressLint("LogNotTimber")
    fun scheduleApiSwitchWorker(forceStart : Boolean) {

        // TODO: perhaps add a 500ms - 1s arbitrary delay so timeDiff is not just several milliseconds between calls?

        // TODO: set logic to acquire api
        val apiTimeStamp = 0L // Proxies.getLastApiAttemptTimeStamp(prefs)
        val timeDiff = System.currentTimeMillis() - apiTimeStamp
        val interval = switchInterval - timeDiff

        if(BuildConfig.DEBUG) {
            Timber.d("apiTimeStamp is: $apiTimeStamp")
            Timber.d("timeDiff is: $timeDiff")
            Timber.d("interval is: $interval")
        }

        if (forceStart) { // means that we get started after positive DoH
            forceStartSwitchWorker(interval)
            return
        }

        val oldWorkerUUID = prefs.getString(PREF_DNS_OVER_HTTPS_API_SWITCH_WORKER_ID, EMPTY_STRING)
        Timber.d("old worker ID is: $oldWorkerUUID")
        if(EMPTY_STRING == oldWorkerUUID) {
            // no DoH occurred yet or we have just installed or reinstalled the app
            Timber.d("no Doh yet or fresh install/reinstall")
            return
        }
    }

    private fun forceStartSwitchWorker(interval: Long) {
        Timber.d("forceStartSwitchWorker")
        // cancel previous workers if any
        val uuidStringOfWorker = prefs.getString(PREF_DNS_OVER_HTTPS_API_SWITCH_WORKER_ID, EMPTY_STRING)
        Timber.d("uuid of worker is: " + uuidStringOfWorker!!)
        if(uuidStringOfWorker != EMPTY_STRING) {
            val uuidOfWorker = UUID.fromString(uuidStringOfWorker)
            if(uuidOfWorker != null) {
                Timber.d("uuid not null, cancelling the worker")
                val operation = WorkManager.getInstance(appContext).cancelWorkById(uuidOfWorker)
                // .cancelAllWorkByTag("NetworkApiSwitchWorkerTag")
            } else {
                Timber.d("uuid of worker is NULL")
            }
        }

        // dispatch new worker
        dispatchSwitchWorker(interval)
    }

    @SuppressLint("LogNotTimber")
    private fun dispatchSwitchWorker(interval: Long) {
        Timber.d("dispatch worker")

        val apiSwitcherWork = OneTimeWorkRequest.Builder(NetworkApiSwitchWorker::class.java)
                .setInitialDelay(interval, TimeUnit.MILLISECONDS)
                .build()

        // store the ID of the worker
        val apiSwitcherWorkID = apiSwitcherWork.id
        val apiSwitchWorkIDString = apiSwitcherWorkID.toString()
        Timber.d("worker ID is: $apiSwitchWorkIDString")
        prefs.edit().putString(PREF_DNS_OVER_HTTPS_API_SWITCH_WORKER_ID, apiSwitchWorkIDString).apply()
        prefs.edit().putBoolean(PREF_DNS_OVER_HTTPS_API_SWITCH_USER_CANCELED, false).apply()

        // val operation = workManager.enqueueUniqueWork(WORKER_TAG, ExistingWorkPolicy.REPLACE, apiSwitcherWork)
        val operation = workManager.enqueue(apiSwitcherWork)
    }
}
