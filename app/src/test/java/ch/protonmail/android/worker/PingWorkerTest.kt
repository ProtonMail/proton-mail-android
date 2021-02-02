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

package ch.protonmail.android.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.QueueNetworkUtil
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.android.workmanager.toWorkData
import me.proton.core.util.kotlin.EMPTY_STRING
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PingWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var queueNetworkUtil: QueueNetworkUtil

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var worker: PingWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        worker = PingWorker(context, parameters, api, queueNetworkUtil, TestDispatcherProvider)
    }

    @Test
    fun verifySuccessIsReturnedWhenPingRespondedWithOk() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.success()
            val pingResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.pingAsync() } returns pingResponse

            // when
            val operationResult = worker.doWork()

            // then
            verify { queueNetworkUtil.setCurrentlyHasConnectivity() }
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifySuccessIsReturnedWhenPingRespondedWithApiOfflineResponse() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.success()
            val pingResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_API_OFFLINE
            }
            coEvery { api.pingAsync() } returns pingResponse

            // when
            val operationResult = worker.doWork()

            // then
            verify { queueNetworkUtil.setCurrentlyHasConnectivity() }
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifyFailureIsReturnedWhenPingRespondedWithUnrecognizedApiResponse() {
        runBlockingTest {
            // given
            val unknownResponseCode = 12313
            val expected = ListenableWorker.Result.failure()
            val pingResponse = mockk<ResponseBody> {
                every { code } returns unknownResponseCode
            }
            coEvery { api.pingAsync() } returns pingResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifyFailureIsReturnedWhenNetworkConnectionFails() {
        runBlockingTest {
            // given
            val ioException = IOException("NetworkError!")
            val expected = ListenableWorker.Result.failure(WorkerError(ioException.message ?: EMPTY_STRING).toWorkData())
            coEvery { api.pingAsync() } throws ioException

            // when
            val operationResult = worker.doWork()

            // then
            verify { queueNetworkUtil.setConnectivityHasFailed(ioException) }
            assertEquals(operationResult, expected)
        }
    }
}
