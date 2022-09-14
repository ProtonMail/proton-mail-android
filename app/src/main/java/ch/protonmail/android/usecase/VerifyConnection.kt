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

package ch.protonmail.android.usecase

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.work.WorkInfo
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.worker.PingWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case responsible for scheduling Worker that sends a ping message through [PingWorker], processing the result
 * and listening to system network disconnections events. It emits LiveData<Constants.ConnectionState> with CONNECTED
 * corresponding to network connection being available and CAN'T_REACH_SERVER otherwise.
 *
 * The idea followed here is that when we get a disconnection event from the system and we no longer have access to the
 * server we will emit CAN'T_REACH_SERVER from this use case and the UI should display a error state snackbar.
 *
 * In order to ensure that we have the connection back we schedule a ping message on a unique worker
 * (because we can go through somehow restricted network and
 * just a system connectivity event might be not enough to check if we can really reach our servers).
 * When the ping is successful we emit CONNECTED from the use case, the UI should hide the snack bar etc.
 */
class VerifyConnection @Inject constructor(
    private val pingWorkerEnqueuer: PingWorker.Enqueuer,
    private val connectivityManager: NetworkConnectivityManager,
    private val queueNetworkUtil: QueueNetworkUtil
) {

    operator fun invoke(): Flow<Constants.ConnectionState> {

        val connectivityManagerFlow = flowOf(
            connectivityManager.isConnectionAvailableFlow(),
            queueNetworkUtil.connectionStateFlow
        )
            .flattenMerge()
            .filter { it != Constants.ConnectionState.CONNECTED } // observe only disconnections
            .onEach {
                Timber.v("connectivityManagerFlow value: ${it.name}")
                pingWorkerEnqueuer.enqueue() // re-schedule ping
            }

        return flowOf(
            getPingStateList(pingWorkerEnqueuer.getWorkInfoState()),
            connectivityManagerFlow
        )
            .flattenMerge()
            .filter { it != Constants.ConnectionState.PING_NEEDED }
            .onStart {
                pingWorkerEnqueuer.enqueue()
                emit(
                    if (connectivityManager.isInternetConnectionPossible()) {
                        Constants.ConnectionState.CONNECTED
                    } else {
                        Constants.ConnectionState.NO_INTERNET
                    }
                ) // start with current net state
            }
    }

    private fun getPingStateList(workInfoLiveData: LiveData<List<WorkInfo>?>): Flow<Constants.ConnectionState> {
        return workInfoLiveData
            .map { workInfoList ->
                val hasSucceededEvents = mutableListOf<Constants.ConnectionState>()
                workInfoList?.forEach { info ->
                    if (info.state.isFinished) {
                        if (info.state == WorkInfo.State.FAILED) {
                            hasSucceededEvents.add(Constants.ConnectionState.CANT_REACH_SERVER)
                        } else {
                            hasSucceededEvents.add(Constants.ConnectionState.CONNECTED)
                        }
                    }
                }
                Timber.v(
                    "SendPing State: $hasSucceededEvents Net: ${connectivityManager.isInternetConnectionPossible()}"
                )
                hasSucceededEvents
            }
            .asFlow()
            .filter { it.isNotEmpty() }
            .map { it[0] }
    }
}
