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

import androidx.lifecycle.MutableLiveData
import androidx.work.WorkInfo
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.worker.PingWorker
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class VerifyConnectionTest : CoroutinesTest by CoroutinesTest(), ArchTest by ArchTest() {

    @MockK
    private lateinit var workEnqueuer: PingWorker.Enqueuer

    @MockK
    private lateinit var connectionManager: NetworkConnectivityManager

    @MockK
    private lateinit var queueNetworkUtil: QueueNetworkUtil

    private lateinit var verifyConnectionUseCase: VerifyConnection

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        verifyConnectionUseCase = VerifyConnection(
            workEnqueuer,
            connectionManager,
            queueNetworkUtil
        )
    }

    @Test
    fun verifyThatConnectedIsReturnedWhenOperationSucceeds() = runTest {
        // given
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.SUCCEEDED
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flow<Constants.ConnectionState> {}
        val backendConnectionsFlow = MutableStateFlow(Constants.ConnectionState.CONNECTED)
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns true
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        every { queueNetworkUtil.connectionStateFlow } returns backendConnectionsFlow
        val expected = listOf(Constants.ConnectionState.CONNECTED, Constants.ConnectionState.CONNECTED)

        // when
        val resultList = verifyConnectionUseCase().take(2).toList()

        // then
        assertEquals(expected, resultList)
        verify(exactly = 1) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatCantReachServerIsReturnedWhenOperationFails() = runTest {
        // given
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.FAILED
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flow<Constants.ConnectionState> {}
        val backendConnectionsFlow = MutableStateFlow(Constants.ConnectionState.CONNECTED)
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns true
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        every { queueNetworkUtil.connectionStateFlow } returns backendConnectionsFlow

        val expected = listOf(Constants.ConnectionState.CONNECTED, Constants.ConnectionState.CANT_REACH_SERVER)

        // when
        val resultList = verifyConnectionUseCase().take(2).toList()

        // then
        assertEquals(expected, resultList)
        verify(exactly = 1) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatOnlyInitialNetworkStatusIsReturnedWhenOperationIsOngoingAndNothingLater() = runTest {
        // given
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.RUNNING
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flow<Constants.ConnectionState> {}
        val backendConnectionsFlow = MutableStateFlow(Constants.ConnectionState.CONNECTED)
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns true
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        every { queueNetworkUtil.connectionStateFlow } returns backendConnectionsFlow

        val expected = Constants.ConnectionState.CONNECTED

        // when
        val resultsList = verifyConnectionUseCase().take(1).toList()

        // then
        assertEquals(expected, resultsList[0])
        verify(exactly = 1) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatConnectedAndThanNoInternetIsReturnedWhenOperationSucceedsAndThenConnectionDrops() = runTest {
        // given
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.SUCCEEDED
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flowOf(Constants.ConnectionState.NO_INTERNET).take(1)
        val backendConnectionsFlow = MutableStateFlow(Constants.ConnectionState.CONNECTED)
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns false
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        every { queueNetworkUtil.connectionStateFlow } returns backendConnectionsFlow

        val expected = listOf(Constants.ConnectionState.CONNECTED, Constants.ConnectionState.NO_INTERNET)

        // when
        val response = verifyConnectionUseCase().take(3).drop(1).toList()

        // then
        assertEquals(expected, response)
        verify(exactly = 2) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatCantReachServerAndThanNoInternetIsReturnedWhenOperationFailsAndThenConnectionDrops() = runTest {
        // given
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.FAILED
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flowOf(Constants.ConnectionState.NO_INTERNET).take(1)
        val backendConnectionsFlow = MutableStateFlow(Constants.ConnectionState.CANT_REACH_SERVER)
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns false
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        every { queueNetworkUtil.connectionStateFlow } returns backendConnectionsFlow

        val expected = listOf(Constants.ConnectionState.CANT_REACH_SERVER, Constants.ConnectionState.NO_INTERNET)

        // when
        val response = verifyConnectionUseCase().take(3).drop(1).toList()

        // then
        assertEquals(expected, response)
        verify(exactly = 3) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatNoInternetAndThanConnectedIsReturnedWhenOperationSucceedsAfterInternetHasSuccessfullyReconnected() =
        runTest {
            // given
            val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
            val workInfo = mockk<WorkInfo> {
                every { state } returns WorkInfo.State.SUCCEEDED
            }
            workInfoLiveData.value = listOf(workInfo)
            val connectionsFlow = flow<Constants.ConnectionState> {}
            val backendConnectionsFlow = MutableStateFlow(Constants.ConnectionState.CONNECTED)
            every { workEnqueuer.enqueue() } returns Unit
            every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns false
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        every { queueNetworkUtil.connectionStateFlow } returns backendConnectionsFlow

        val expected = listOf(Constants.ConnectionState.NO_INTERNET, Constants.ConnectionState.CONNECTED)

        // when
        val response = verifyConnectionUseCase().take(2).toList()

        // then
        assertEquals(expected, response)
        verify(exactly = 1) { workEnqueuer.enqueue() }
    }
}
