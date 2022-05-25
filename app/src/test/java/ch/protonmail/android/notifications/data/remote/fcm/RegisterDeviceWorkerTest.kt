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

package ch.protonmail.android.notifications.data.remote.fcm

import android.content.Context
import androidx.work.Constraints
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import ch.protonmail.android.notifications.data.remote.fcm.model.FirebaseToken
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.BuildInfo
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.mocks.mockSharedPreferences
import me.proton.core.test.android.mocks.newMockSharedPreferences
import java.io.IOException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the functionality of [RegisterDeviceWorker].
 */

class RegisterDeviceWorkerTest {

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

    private val fcmTokenManager: FcmTokenManager = mockk(relaxed = true) {
        coEvery { getToken() } returns FirebaseToken("registrationId")
    }

    private val fcmTokenManagerFactory: FcmTokenManager.Factory = mockk {
        every { create(any()) } returns fcmTokenManager
    }

    private lateinit var registerDeviceWorker: RegisterDeviceWorker

    private lateinit var registerDeviceWorkerEnqueuer: RegisterDeviceWorker.Enqueuer

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        mockkObject(SecureSharedPreferences.Companion)
        every { SecureSharedPreferences.getPrefsForUser(any(), any()) } returns mockSharedPreferences

        registerDeviceWorker = RegisterDeviceWorker(
            context,
            workerParameters,
            buildInfo,
            protonMailApiManager,
            fcmTokenManagerFactory = fcmTokenManagerFactory
        )
        registerDeviceWorkerEnqueuer = RegisterDeviceWorker.Enqueuer(
            context,
            workManager,
            accountManager,
            fcmTokenManagerFactory
        )
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(SecureSharedPreferences.Companion)
    }

    @Test
    fun shouldReturnFailureIfUsernameIsNotProvided() {
        runBlocking {
            // given
            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_PM_REGISTRATION_WORKER_ERROR to "User id not provided")
            )

            // when
            val workerResult = registerDeviceWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun shouldReturnRetryIfApiCallFails() {
        runBlocking {
            // given
            val userId = UserId("id")
            val ioException = IOException()

            every { workerParameters.inputData } returns workDataOf(KEY_PM_REGISTRATION_WORKER_USER_ID to userId.id)
            coEvery { protonMailApiManager.registerDevice(any(), any()) } throws ioException

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val workerResult = registerDeviceWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun shouldReturnSuccessIfApiCallSucceeds() {
        runBlocking {
            // given
            val userId = UserId("id")
            val mockRegisterDeviceResponse = mockk<ResponseBody> {
                every { code } returns RESPONSE_CODE_OK
            }

            every { workerParameters.inputData } returns workDataOf(KEY_PM_REGISTRATION_WORKER_USER_ID to userId.id)
            coEvery { protonMailApiManager.registerDevice(any(), any()) } returns mockRegisterDeviceResponse

            val expectedResult = ListenableWorker.Result.success()

            // when
            val workerResult = registerDeviceWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun verifySetTokenSentFunctionWasCalledWhenApiCallSucceeds() {
        runBlocking {
            // given
            val userId = UserId("id")
            val mockRegisterDeviceResponse = mockk<ResponseBody> {
                every { code } returns RESPONSE_CODE_OK
            }

            every { workerParameters.inputData } returns workDataOf(KEY_PM_REGISTRATION_WORKER_USER_ID to userId.id)
            coEvery { protonMailApiManager.registerDevice(any(), any()) } returns mockRegisterDeviceResponse

            // when
            registerDeviceWorker.doWork()

            // then
            coVerify { fcmTokenManager.setTokenSent(true) }
        }
    }

    @Test
    fun verifyWorkerBeingEnqueuedForEveryUserForWhichATokenHasNotBeenSent() {
        mockkObject(SecureSharedPreferences.Companion) {
            // given
            val (userIds, userPrefs) = (1..3).map {
                UserId(it.toString()) to newMockSharedPreferences
            }.toMap().let { it.keys to it.values.toList() }
            every { SecureSharedPreferences.getPrefsForUser(any(), any()) } answers {
                userPrefs[userIds.indexOf(secondArg())]
            }
            val accounts = userIds.map { user ->
                mockk<Account> {
                    every { userId } returns UserId(user.id)
                    every { state } returns AccountState.Ready
                }
            }
            coEvery { accountManager.getAccounts(AccountState.Ready) } returns flowOf(accounts)
            coEvery { accountManager.getAccounts() } returns flowOf(accounts)

            every { fcmTokenManagerFactory.create(userPrefs[0]).isTokenSentBlocking() } returns false
            every { fcmTokenManagerFactory.create(userPrefs[1]).isTokenSentBlocking() } returns true
            every { fcmTokenManagerFactory.create(userPrefs[2]).isTokenSentBlocking() } returns false

            // when
            registerDeviceWorkerEnqueuer()

            // then
            verify(exactly = 2) { workManager.enqueue(any<WorkRequest>()) }
        }
    }

    @Test
    fun verifyThatWorkerIsBeingEnqueuedWithTheCorrectParameters() {
        mockkObject(SecureSharedPreferences.Companion) {
            // given
            val (userIds, userPrefs) = (1..3).map {
                UserId(it.toString()) to newMockSharedPreferences
            }.toMap().let { it.keys to it.values.toList() }
            every { SecureSharedPreferences.getPrefsForUser(any(), any()) } answers {
                userPrefs[userIds.indexOf(secondArg())]
            }
            val accounts = userIds.map { user ->
                mockk<Account> {
                    every { userId } returns UserId(user.id)
                    every { state } returns AccountState.Ready
                }
            }
            coEvery { accountManager.getAccounts(AccountState.Ready) } returns flowOf(accounts)
            coEvery { accountManager.getAccounts() } returns flowOf(accounts)

            every { fcmTokenManagerFactory.create(userPrefs[0]).isTokenSentBlocking() } returns false
            every { fcmTokenManagerFactory.create(userPrefs[1]).isTokenSentBlocking() } returns true
            every { fcmTokenManagerFactory.create(userPrefs[2]).isTokenSentBlocking() } returns true

            val expectedInputData = workDataOf(KEY_PM_REGISTRATION_WORKER_USER_ID to userIds.first().id)
            val expectedConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            // when
            registerDeviceWorkerEnqueuer()

            // then
            val workRequestSlot = slot<WorkRequest>()
            verify { workManager.enqueue(capture(workRequestSlot)) }
            assertEquals(expectedInputData, workRequestSlot.captured.workSpec.input)
            assertEquals(expectedConstraints, workRequestSlot.captured.workSpec.constraints)
        }
    }
}
