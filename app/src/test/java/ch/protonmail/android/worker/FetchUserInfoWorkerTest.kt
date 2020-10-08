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
import android.text.TextUtils
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.UserInfo
import ch.protonmail.android.api.models.address.AddressesResponse
import ch.protonmail.android.api.segments.RESPONSE_CODE_UNAUTHORIZED
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

/**
 * Tests the functionality of [FetchUserInfoWorker].
 *
 * @author Stefanija Boshkovska
 */

class FetchUserInfoWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var protonMailApiManager: ProtonMailApiManager

    @MockK
    private lateinit var userManager: UserManager

    private lateinit var worker: FetchUserInfoWorker

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } returns false

        worker = FetchUserInfoWorker(
            context,
            parameters,
            protonMailApiManager,
            userManager,
            TestDispatcherProvider
        )
    }

    @Test
    fun `should return new user delinquency data when both API calls are successful`() {
        runBlocking {
            val mockUser = spyk<User> {
                every { delinquent } returns false
                every { name } returns "name"
                every { setAddresses(any()) } just runs
            }
            val mockUserInfoResponse = mockk<UserInfo> {
                every { code } returns Constants.RESPONSE_CODE_OK
                every { user } returns mockUser
            }
            val mockAddressesResponse = mockk<AddressesResponse> {
                every { code } returns Constants.RESPONSE_CODE_OK
                every { addresses } returns mockk()
            }

            coEvery { protonMailApiManager.fetchUserInfo() } returns mockUserInfoResponse
            coEvery { protonMailApiManager.fetchAddresses() } returns mockAddressesResponse

            every { userManager.user } returns mockUser

            val expectedResult = ListenableWorker.Result.success(workDataOf(FETCH_USER_INFO_WORKER_RESULT to false))

            // when
            val workerResult = worker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun `should return old user delinquency data when at least one of the API calls responds with error code`() {
        runBlocking {
            val mockUser = spyk<User> {
                every { delinquent } returns true
            }
            val mockUserInfoResponse = mockk<UserInfo> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            val mockAddressesResponse = mockk<AddressesResponse> {
                every { code } returns RESPONSE_CODE_UNAUTHORIZED
            }

            coEvery { protonMailApiManager.fetchUserInfo() } returns mockUserInfoResponse
            coEvery { protonMailApiManager.fetchAddresses() } returns mockAddressesResponse

            every { userManager.user } returns mockUser

            val expectedResult = ListenableWorker.Result.success(workDataOf(FETCH_USER_INFO_WORKER_RESULT to true))

            // when
            val workerResult = worker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun `should return failure result when at least one of the API calls throws an exception`() {
        runBlocking {
            val mockUserInfoResponse = mockk<UserInfo> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            val mockException = spyk<IOException> {
                every { message } returns "exception"
            }

            coEvery { protonMailApiManager.fetchUserInfo() } returns mockUserInfoResponse
            coEvery { protonMailApiManager.fetchAddresses() } throws mockException

            val expectedResult = ListenableWorker.Result.failure(workDataOf(FETCH_USER_INFO_WORKER_EXCEPTION_MESSAGE to "exception"))

            // when
            val workerResult = worker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }
}
