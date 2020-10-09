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
import ch.protonmail.android.worker.PingWorker
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SendPingTest : CoroutinesTest {

    @get:Rule
    val archRule = InstantTaskExecutorRule()

    @MockK
    private lateinit var workEnqueuer: PingWorker.Enqueuer

    @MockK
    private lateinit var connectionManager: NetworkConnectivityManager

    private lateinit var sendPingUseCase: SendPing

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        sendPingUseCase = SendPing(
            workEnqueuer,
            connectionManager
        )
    }

    @Test
    fun verifyThatTrueIsReturnedWhenOperationSucceeds() {
        // given
        val workInfoLiveData = MutableLiveData<WorkInfo>()
        val isInternetAvailable = false
        workInfoLiveData.value = mockk {
            every { state } returns WorkInfo.State.SUCCEEDED
        }
        every { workEnqueuer.enqueue() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns isInternetAvailable
        val observer = mockk<Observer<Boolean>>(relaxed = true)
        val expected = true

        // when
        val response = sendPingUseCase()
        response.observeForever(observer)

        // then
        assertNotNull(response.value)
        verify { observer.onChanged(isInternetAvailable) }
        verify { observer.onChanged(expected) }
        assertEquals(expected, response.value)
    }

    @Test
    fun verifyThatFalseIsReturnedWhenOperationFails() {
        // given
        val workInfoLiveData = MutableLiveData<WorkInfo>()
        val isInternetAvailable = true
        workInfoLiveData.value = mockk {
            every { state } returns WorkInfo.State.FAILED
        }
        every { workEnqueuer.enqueue() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns isInternetAvailable

        val observer = mockk<Observer<Boolean>>(relaxed = true)
        val expected = false

        // when
        val response = sendPingUseCase()
        response.observeForever(observer)

        // then
        assertNotNull(response.value)
        verify { observer.onChanged(isInternetAvailable) }
        verify { observer.onChanged(expected) }
        assertEquals(expected, response.value)
    }

    @Test
    fun verifyThatOnlyInitialNetworkStatusIsReturnedWhenOperationIsOngoingAndNothingLater() {
        // given
        val workInfoLiveData = MutableLiveData<WorkInfo>()
        val isInternetAvailable = true
        workInfoLiveData.value = mockk {
            every { state } returns WorkInfo.State.RUNNING
        }
        every { workEnqueuer.enqueue() } returns workInfoLiveData
        every { connectionManager.isInternetConnectionPossible() } returns isInternetAvailable

        val observer = mockk<Observer<Boolean>>(relaxed = true)

        // when
        val response = sendPingUseCase()
        response.observeForever(observer)

        // then
        assertNotNull(response.value)
        verify(exactly = 1) { observer.onChanged(isInternetAvailable) }
        assertEquals(isInternetAvailable, response.value)
    }
}
