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

package ch.protonmail.android.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.SwitchToMainBackendIfOnProxy
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.utils.AppUtil
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
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

    private val switchToMainBackendIfOnProxy = mockk<SwitchToMainBackendIfOnProxy> {
        coEvery { this@mockk() } returns SwitchToMainBackendIfOnProxy.AlreadyUsingMainBackend
    }

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        worker = PingWorker(context, parameters, api, queueNetworkUtil, switchToMainBackendIfOnProxy)
    }

    @Test
    fun verifySuccessIsReturnedWhenPingRespondedWithOk() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.success()
            val pingResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.ping() } returns pingResponse

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
            coEvery { api.ping() } returns pingResponse

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
            coEvery { api.ping() } returns pingResponse

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
            val exceptionMessage = "NetworkError!"
            val ioException = IOException(exceptionMessage)
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code $exceptionMessage")
            )
            mockkStatic(AppUtil::class)
            justRun { AppUtil.postEventOnUi(any()) }
            coEvery { api.ping() } throws ioException

            // when
            val operationResult = worker.doWork()

            // then
            verify { queueNetworkUtil.setConnectivityHasFailed(ioException) }
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun `should return a success and not call api when switching to main BE succeeded`() = runBlockingTest {
        // given
        coEvery { switchToMainBackendIfOnProxy() } returns SwitchToMainBackendIfOnProxy.SwitchSuccess
        val expectedResult = ListenableWorker.Result.success()

        // when
        val actualResult = worker.doWork()

        // then
        assertEquals(expectedResult, actualResult)
        verify { queueNetworkUtil.setCurrentlyHasConnectivity() }
        coVerify(exactly = 0) { api.ping() }
    }

    @Test
    fun `should call the api when already switched to the main BE`() = runBlockingTest {
        // given
        coEvery { switchToMainBackendIfOnProxy() } returns SwitchToMainBackendIfOnProxy.AlreadyUsingMainBackend
        coEvery { api.ping() } returns ResponseBody()

        // when
        worker.doWork()

        // then
        coVerify { api.ping() }
    }

    @Test
    fun `should call the api when switching to main BE failed`() = runBlockingTest {
        // given
        coEvery { switchToMainBackendIfOnProxy() } returns SwitchToMainBackendIfOnProxy.SwitchFailure
        coEvery { api.ping() } returns ResponseBody()

        // when
        worker.doWork()

        // then
        coVerify { api.ping() }
    }
}
