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
import ch.protonmail.android.attachments.KEY_INPUT_DATA_USER_ID_STRING
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.runBlocking
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.Delinquent
import me.proton.core.user.domain.entity.User
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the functionality of [FetchUserWorker].
 *
 * @author Stefanija Boshkovska
 */

class FetchUserWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var oldUserManager: ch.protonmail.android.core.UserManager

    private lateinit var worker: FetchUserWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        mockkStatic(TextUtils::class)
        every { TextUtils.isEmpty(any()) } returns false

        worker = FetchUserWorker(
            context,
            parameters,
            userManager,
            oldUserManager
        )

        every { parameters.inputData.getString(KEY_INPUT_DATA_USER_ID_STRING) } returns "userId"
    }

    @Test
    fun `should return new user delinquency data when both API calls are successful`() {
        runBlocking {
            val mockUser = mockk<User> {
                every { delinquent } returns Delinquent.None
                every { name } returns "name"
            }

            coEvery { userManager.getUser(any(), any()) } returns mockUser

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
            val mockUser = mockk<User> {
                every { delinquent } returns Delinquent.InvoiceDelinquent
            }

            coEvery { userManager.getUser(any(), any()) } returns mockUser

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
            val mockException = mockk<Exception> {
                every { message } returns "exception"
            }

            coEvery { userManager.getUser(any(), any()) } throws mockException

            val expectedResult =
                ListenableWorker.Result.failure(workDataOf(FETCH_USER_INFO_WORKER_EXCEPTION_MESSAGE to "exception"))

            // when
            val workerResult = worker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }
}
