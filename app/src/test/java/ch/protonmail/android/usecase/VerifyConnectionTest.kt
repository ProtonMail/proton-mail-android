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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.utils.captureValues
import ch.protonmail.android.worker.PingWorker
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

class VerifyConnectionTest : CoroutinesTest {

    @get:Rule
    val archRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var workEnqueuer: PingWorker.Enqueuer

    @MockK
    private lateinit var connectionManager: NetworkConnectivityManager

    private lateinit var verifyConnectionUseCase: VerifyConnection

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        verifyConnectionUseCase = VerifyConnection(
            workEnqueuer,
            connectionManager
        )
    }

    @Test
    fun verifyThatTrueIsReturnedWhenOperationSucceeds() {
        // given
        val isInternetAvailable = false
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.SUCCEEDED
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flow<Boolean> {}
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns isInternetAvailable
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        val observer = mockk<Observer<Boolean>>(relaxed = true)
        val expected = true

        // when
        val response = verifyConnectionUseCase()
        response.observeForever(observer)

        // then
        verify { observer.onChanged(isInternetAvailable) }
        verify { observer.onChanged(expected) }
        assertEquals(expected, response.value)
        verify(exactly = 1) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatFalseIsReturnedWhenOperationFails() {
        // given
        val isInternetAvailable = true
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.FAILED
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flow<Boolean> {}
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns isInternetAvailable
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow

        val observer = mockk<Observer<Boolean>>(relaxed = true)
        val expected = false

        // when
        val response = verifyConnectionUseCase()
        response.observeForever(observer)

        // then
        verify { observer.onChanged(isInternetAvailable) }
        verify { observer.onChanged(expected) }
        assertEquals(expected, response.value)
        verify(exactly = 1) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatOnlyInitialNetworkStatusIsReturnedWhenOperationIsOngoingAndNothingLater() {
        // given
        val isInternetAvailable = true
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.RUNNING
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flow<Boolean> {}
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns isInternetAvailable
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow

        val observer = mockk<Observer<Boolean>>(relaxed = true)

        // when
        val response = verifyConnectionUseCase()
        response.observeForever(observer)

        // then
        verify(exactly = 1) { observer.onChanged(isInternetAvailable) }
        assertEquals(isInternetAvailable, response.value)
        verify(exactly = 1) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatTrueAndThanFalseIsReturnedWhenOperationSucceedsAndThenConnectionDrops() {
        // given
        val isInternetAvailable = false
        val newConnectionEvent = false
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.SUCCEEDED
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flowOf(newConnectionEvent).take(1)
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns isInternetAvailable
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        val expectedWorkerState = true

        // when
        val response = verifyConnectionUseCase()

        // then
        response.captureValues {
            assertEquals(listOf(isInternetAvailable, expectedWorkerState, newConnectionEvent), values)
        }
        verify(exactly = 2) { workEnqueuer.enqueue() }
    }

    @Test
    fun verifyThatTrueAndThanFalseIsReturnedWhenOperationSucceedsAndThenConnectionEventIsNotTakenIntoAccount() {
        // given
        val isInternetAvailable = false
        val newConnectionEvent = true
        val workInfoLiveData = MutableLiveData<List<WorkInfo>>()
        val workInfo = mockk<WorkInfo> {
            every { state } returns WorkInfo.State.SUCCEEDED
        }
        workInfoLiveData.value = listOf(workInfo)
        val connectionsFlow = flowOf(newConnectionEvent).take(1)
        every { workEnqueuer.enqueue() } returns Unit
        every { workEnqueuer.getWorkInfoState() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns isInternetAvailable
        every { connectionManager.isConnectionAvailableFlow() } returns connectionsFlow
        val expectedWorkerState = true

        // when
        val response = verifyConnectionUseCase()

        // then
        response.captureValues {
            assertEquals(listOf(isInternetAvailable, expectedWorkerState), values)
        }
        verify(exactly = 1) { workEnqueuer.enqueue() }
    }
}
