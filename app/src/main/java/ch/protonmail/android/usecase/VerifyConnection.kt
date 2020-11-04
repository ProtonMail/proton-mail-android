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

package ch.protonmail.android.usecase

import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.map
import androidx.work.WorkInfo
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.worker.PingWorker
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.seconds

/**
 * Use case responsible for scheduling Worker that sends a ping message through [PingWorker], processing the result
 * and listening to system network disconnections events. It emits LiveData<Boolean> with true corresponding to
 * network connection being available and false otherwise.
 *
 * The idea followed here is that when we get a disconnection event from the system and we no longer have a network
 * interface with an internet capability we will emit false from this use case and the UI should display a
 * no connection snackbar.
 *
 * In order to ensure that we have the connection back we schedule a ping message on a unique worker
 * (because we can go through somehow restricted network and
 * just a system connectivity event might be not enough to check if we can really reach our servers).
 * When the ping is successful we emit true from the use case, the UI should hide the snack bar etc.
 */
class VerifyConnection @Inject constructor(
    private val pingWorkerEnqueuer: PingWorker.Enqueuer,
    private val connectivityManager: NetworkConnectivityManager,
    private val queueNetworkUtil: QueueNetworkUtil
) {

    operator fun invoke(): Flow<Boolean> {
        Timber.v("VerifyConnection invoked")

        val connectivityManagerFlow = flowOf(
            connectivityManager.isConnectionAvailableFlow(),
            queueNetworkUtil.isBackendRespondingWithoutErrorFlow
        )
            .flattenMerge()
            .filter { !it } // observe only disconnections
            .onEach {
                delay(2.seconds) // this delay has been added in case of DoH being established in the meantime
                pingWorkerEnqueuer.enqueue() // re-schedule ping
            }

        return flowOf(
            getPingStateList(pingWorkerEnqueuer.getWorkInfoState()),
            connectivityManagerFlow
        )
            .flattenMerge()
            .onStart {
                pingWorkerEnqueuer.enqueue()
                emit(connectivityManager.isInternetConnectionPossible()) // start with current net state
            }
    }

    private fun getPingStateList(workInfoLiveData: LiveData<List<WorkInfo>?>): Flow<Boolean> {
        return workInfoLiveData
            .map { workInfoList ->
                val hasSucceededEvents = mutableListOf<Boolean>()
                workInfoList?.forEach { info ->
                    if (info.state.isFinished) {
                        hasSucceededEvents.add(info.state == WorkInfo.State.SUCCEEDED)
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
