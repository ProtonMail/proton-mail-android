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
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.QueueNetworkUtil
import ch.protonmail.android.utils.AppUtil
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.network.domain.ApiResult
import me.proton.core.test.kotlin.TestDispatcherProvider
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
    fun verifySuccessIsReturnedWhenPingRespondedWithSuccess() {
        runBlockingTest {
            // given
            val expected = Result.success()
            val pingResponse = ApiResult.Success(Unit)
            coEvery { api.ping() } returns pingResponse

            // when
            val operationResult = worker.doWork()

            // then
            verify { queueNetworkUtil.setCurrentlyHasConnectivity() }
            assertEquals(expected, operationResult)
        }
    }

    @Test
    fun verifySuccessIsReturnedWhenPingRespondedWithNoInternetError() {
        runBlockingTest {
            // given
            val pingResponse = ApiResult.Error.NoInternet
            val expected = Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${pingResponse.cause}")
            )
            coEvery { api.ping() } returns pingResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(expected, operationResult)
        }
    }

    @Test
    fun verifyFailureIsReturnedWhenNetworkConnectionFails() {
        runBlockingTest {
            // given
            val exceptionMessage = "NetworkError!"
            val ioException = IOException(exceptionMessage)
            val pingResponse = ApiResult.Error.Connection(false, ioException)
            val expected = Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${pingResponse.cause}")
            )
            mockkStatic(AppUtil::class)
            justRun { AppUtil.postEventOnUi(any()) }
            coEvery { api.ping() } returns pingResponse

            // when
            val operationResult = worker.doWork()

            // then
            verify { queueNetworkUtil.setConnectivityHasFailed(ioException) }
            assertEquals(operationResult, expected)
        }
    }
}
