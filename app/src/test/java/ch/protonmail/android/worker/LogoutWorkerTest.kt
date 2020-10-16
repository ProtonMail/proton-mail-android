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
import androidx.work.workDataOf
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.TokenManager
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.fcm.FcmUtil
import ch.protonmail.android.utils.AppUtil
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LogoutWorkerTest {

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var accountManager: AccountManager

    @RelaxedMockK
    private lateinit var context: Context

    @MockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var worker: LogoutWorker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        every { parameters.taskExecutor } returns mockk(relaxed = true)
        worker = LogoutWorker(
            context,
            parameters,
            api,
            accountManager,
            userManager,
            TestDispatcherProvider
        )
        mockkStatic(AppUtil::class)
        mockkStatic(TokenManager::class)
        mockkStatic(FcmUtil::class)
        mockkStatic(ProtonMailApplication::class)
    }

    @After
    fun tearDown() {
        unmockkStatic(AppUtil::class)
        unmockkStatic(TokenManager::class)
        unmockkStatic(FcmUtil::class)
        unmockkStatic(ProtonMailApplication::class)
    }

    @Test
    fun verifyThatWhenUserNameIsNotEmptyDeviceIsUnregisteredAndAccessIsRevoked() =
        runBlockingTest {
            // given
            val testUserName = "testUserName"
            val registrationId = "Id1234"
            every { parameters.inputData } returns workDataOf(KEY_INPUT_USER_NAME to testUserName)
            every { userManager.username } returns ""
            val tokenManager = mockk<TokenManager>(relaxed = true)
            every { userManager.getTokenManager(testUserName) } returns tokenManager
            every { userManager.nextLoggedInAccountOtherThanCurrent } returns null
            every { accountManager.getLoggedInUsers() } returns listOf(testUserName)
            every { accountManager.clear() } returns Unit
            every { AppUtil.deleteSecurePrefs(testUserName, any()) } returns Unit
            every { AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS)) } returns Unit
            every { ProtonMailApplication.getApplication().getSecureSharedPreferences(testUserName) } returns mockk(relaxed = true)
            every { TokenManager.getInstance(testUserName) } returns tokenManager
            every { FcmUtil.getRegistrationId() } returns registrationId

            val revokeResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.unregisterDevice(registrationId) } returns revokeResponse
            coEvery { api.revokeAccess(testUserName) } returns revokeResponse
            val expected = ListenableWorker.Result.success()

            // when
            val result = worker.doWork()

            // then
            verify { accountManager.clear() }
            coVerify { api.unregisterDevice(registrationId) }
            coVerify { api.revokeAccess(testUserName) }
            assertEquals(expected, result)
        }

    @Test
    fun verifyThatWhenUserNameIsNotEmptyButAnExceptionOccursRetryWillBeReturned() =
        runBlockingTest {
            // given
            val testUserName = "testUserName"
            val registrationId = "Id1234"
            val testException = Exception("TestException")
            every { parameters.inputData } returns workDataOf()
            every { userManager.username } returns testUserName
            every { accountManager.getLoggedInUsers() } returns listOf(testUserName)
            val expected = ListenableWorker.Result.retry()
            every { FcmUtil.getRegistrationId() } returns registrationId
            coEvery { api.unregisterDevice(registrationId) } throws testException

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }

    @Test
    fun verifyThatWhenUserNameIsNotEmptyButAnExceptionOccursMoreTHanThreeTimesAndFailureIsReturned() =
        runBlockingTest {
            // given
            val testUserName = "testUserName"
            val registrationId = "Id1234"
            val exceptionMessage = "TestException"
            val testException = Exception(exceptionMessage)
            every { parameters.inputData } returns workDataOf()
            every { userManager.username } returns testUserName
            every { accountManager.getLoggedInUsers() } returns listOf(testUserName)
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to exceptionMessage)
            )
            every { FcmUtil.getRegistrationId() } returns registrationId
            coEvery { api.unregisterDevice(registrationId) } throws testException

            // when
            worker.doWork()
            worker.doWork()
            worker.doWork()
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }

    @Test
    fun verifyThatEmptyUserNameWillCauseResultFailureToBeReturnedWithoutRetry() =
        runBlockingTest {
            // given
            every { parameters.inputData } returns workDataOf()
            every { userManager.username } returns ""
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with an empty user name")
            )

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
}
