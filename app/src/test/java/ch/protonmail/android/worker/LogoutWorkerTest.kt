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
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.events.LogoutEvent
import ch.protonmail.android.events.Status
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.AppUtil
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.mocks.newMockSharedPreferences
import org.junit.After
import kotlin.test.BeforeTest
import kotlin.test.Test
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

    private val testUserId = Id("id")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { parameters.taskExecutor } returns mockk(relaxed = true)
        worker = LogoutWorker(
            context,
            parameters,
            api,
            accountManager,
            userManager
        )
        mockkStatic(AppUtil::class, ProtonMailApplication::class)
        mockkObject(TokenManager.Companion, SecureSharedPreferences.Companion)
        every { SecureSharedPreferences.getPrefsForUser(any(), testUserId) } answers { newMockSharedPreferences }
    }

    @After
    fun tearDown() {
        unmockkStatic(AppUtil::class, ProtonMailApplication::class)
        unmockkObject(TokenManager.Companion, SecureSharedPreferences.Companion)
    }

    @Test
    fun verifyThatWhenUserNameIsNotEmptyDeviceIsUnregisteredAndAccessIsRevoked() =
        runBlockingTest {
            // given
            val registrationId = "Id1234"
            val userPrefs = newMockSharedPreferences
            every { parameters.inputData } returns workDataOf(
                KEY_INPUT_USER_ID to testUserId.s,
                KEY_INPUT_FCM_REGISTRATION_ID to registrationId
            )
            every { userManager.currentUserId } returns null
            val tokenManager = mockk<TokenManager>(relaxed = true)
            coEvery { userManager.getTokenManager(testUserId) } returns tokenManager
            coEvery { userManager.getNextLoggedInUser() } returns null
            coEvery { accountManager.allLoggedIn() } returns setOf(testUserId)
            coEvery { accountManager.clear() } just Runs
            coEvery { AppUtil.deleteSecurePrefs(userPrefs, any()) } just Runs
            every { AppUtil.postEventOnUi(LogoutEvent(Status.SUCCESS)) } just Runs
            every { TokenManager.getInstance(any(), testUserId) } returns tokenManager

            val revokeResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.unregisterDevice(registrationId) } returns revokeResponse
            coEvery { api.revokeAccess(testUserId) } returns revokeResponse
            val expected = ListenableWorker.Result.success()

            // when
            val result = worker.doWork()

            // then
            coVerify { accountManager.clear() }
            coVerify { api.unregisterDevice(registrationId) }
            coVerify { api.revokeAccess(testUserId) }
            assertEquals(expected, result)
        }

    @Test
    fun verifyThatWhenUserNameIsNotEmptyButAnExceptionOccursFailureWillBeReturned() =
        runBlockingTest {
            // given
            val registrationId = "Id1234"
            val exceptionMessage = "TestException"
            val testException = Exception(exceptionMessage)
            every { parameters.inputData } returns workDataOf(
                KEY_INPUT_USER_ID to testUserId.s,
                KEY_INPUT_FCM_REGISTRATION_ID to registrationId
            )
            every { userManager.currentUserId } returns testUserId
            coEvery { accountManager.allLoggedIn() } returns setOf(testUserId)
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to exceptionMessage)
            )
            coEvery { api.unregisterDevice(registrationId) } throws testException

            // when
            val result = worker.doWork()

            // then
            coVerify { api.unregisterDevice(registrationId) }
            assertEquals(expected, result)
        }

    @Test
    fun verifyThatEmptyUserNameWillCauseResultFailureToBeReturnedWithoutRetry() =
        runBlockingTest {
            // given
            every { parameters.inputData } returns workDataOf()
            every { userManager.currentUserId } returns null
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "User id is required")
            )

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
}
