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

package ch.protonmail.android.fcm

import android.content.Context
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import ch.protonmail.android.utils.BuildInfo
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

/**
 * Tests the functionality of [PMRegistrationWorker].
 */

class PMRegistrationWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var workerParameters: WorkerParameters

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @RelaxedMockK
    private lateinit var buildInfo: BuildInfo

    @MockK
    private lateinit var protonMailApiManager: ProtonMailApiManager

    @MockK
    private lateinit var accountManager: AccountManager

    private lateinit var pmRegistrationWorker: PMRegistrationWorker

    private lateinit var pmRegistrationWorkerEnqueuer: PMRegistrationWorker.Enqueuer

    @Before
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic(FcmUtil::class)
        every { FcmUtil.getFirebaseToken() } returns "registrationId"
        every { FcmUtil.setTokenSent(any(), any()) } just runs

        pmRegistrationWorker = PMRegistrationWorker(
            context,
            workerParameters,
            buildInfo,
            protonMailApiManager
        )
        pmRegistrationWorkerEnqueuer = PMRegistrationWorker.Enqueuer(
            workManager,
            accountManager
        )
    }

    @After
    fun tearDown() {
        unmockkStatic(FcmUtil::class)
    }

    @Test
    fun shouldReturnFailureIfUsernameIsNotProvided() {
        runBlocking {
            // given
            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_PM_REGISTRATION_WORKER_ERROR to "Username not provided")
            )

            // when
            val workerResult = pmRegistrationWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun shouldReturnRetryIfApiCallFails() {
        runBlocking {
            // given
            val username = "username"
            val ioException = IOException()

            every { workerParameters.inputData } returns workDataOf(KEY_PM_REGISTRATION_WORKER_USERNAME to username)
            coEvery { protonMailApiManager.registerDevice(any(), any()) } throws ioException

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val workerResult = pmRegistrationWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun shouldReturnSuccessIfApiCallSucceeds() {
        runBlocking {
            // given
            val username = "username"
            val mockRegisterDeviceResponse = mockk<ResponseBody> {
                every { code } returns RESPONSE_CODE_OK
            }

            every { workerParameters.inputData } returns workDataOf(KEY_PM_REGISTRATION_WORKER_USERNAME to username)
            coEvery { protonMailApiManager.registerDevice(any(), any()) } returns mockRegisterDeviceResponse

            val expectedResult = ListenableWorker.Result.success()

            // when
            val workerResult = pmRegistrationWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun verifySetTokenSentFunctionWasCalledWhenApiCallSucceeds() {
        runBlocking {
            // given
            val username = "username"
            val mockRegisterDeviceResponse = mockk<ResponseBody> {
                every { code } returns RESPONSE_CODE_OK
            }

            every { workerParameters.inputData } returns workDataOf(KEY_PM_REGISTRATION_WORKER_USERNAME to username)
            coEvery { protonMailApiManager.registerDevice(any(), any()) } returns mockRegisterDeviceResponse

            // when
            pmRegistrationWorker.doWork()

            // then
            verify { FcmUtil.setTokenSent(username, true) }
        }
    }

    @Test
    fun verifyWorkerBeingEnqueuedForEveryUserForWhichATokenHasNotBeenSent() {
        // given
        val usernameList = listOf("username1", "username2", "username3")
        every { accountManager.getLoggedInUsers() } returns usernameList

        every { FcmUtil.isTokenSent("username1") } returns false
        every { FcmUtil.isTokenSent("username2") } returns true
        every { FcmUtil.isTokenSent("username3") } returns false

        // when
        pmRegistrationWorkerEnqueuer()

        // then
        verify(exactly = 2) { workManager.enqueue(any<WorkRequest>()) }
    }

    @Test
    fun verifyThatWorkerIsBeingEnqueuedWithTheCorrectParameters() {
        // given
        val usernameList = listOf("username1", "username2")
        every { accountManager.getLoggedInUsers() } returns usernameList

        every { FcmUtil.isTokenSent("username1") } returns false
        every { FcmUtil.isTokenSent("username2") } returns true

        val expectedInputData = workDataOf(KEY_PM_REGISTRATION_WORKER_USERNAME to "username1")
        val expectedConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // when
        pmRegistrationWorkerEnqueuer()

        // then
        val workRequestSlot = slot<WorkRequest>()
        verify { workManager.enqueue(capture(workRequestSlot)) }
        assertEquals(workRequestSlot.captured.workSpec.input, expectedInputData)
        assertEquals(workRequestSlot.captured.workSpec.constraints, expectedConstraints)
    }
}
