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
import ch.protonmail.android.api.ProtonMailApiManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteLabelWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var api: ProtonMailApiManager

    @MockK
    private lateinit var accountManager: AccountManager

    private lateinit var worker: DeleteLabelWorker

    private val testUserId = UserId("testUser")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        worker = DeleteLabelWorker(
            context,
            parameters,
            api,
            TestDispatcherProvider,
            accountManager
        )
    }

    @Test
    fun verifyWorkerFailsWithNoLabelIdProvided() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty label id")
            )

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun verifyWorkerSuccessesWithLabelIdProvided() {
        runBlockingTest {
            // given
            val labelId = "id1"
            val deleteResponse = ApiResult.Success(Unit)
            val expected = ListenableWorker.Result.success()

            every { parameters.inputData } returns workDataOf(KEY_INPUT_DATA_LABEL_IDS to arrayOf(labelId))
            coEvery { api.deleteLabel(testUserId, any()) } returns deleteResponse

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun verifyWorkerFailureWithLabelIdProvidedButBadServerResponse() {
        runBlockingTest {
            // given
            val labelId = "id1"
            val errorCode = 12123
            val error = "Test API Error"
            val protonErrorData = ApiResult.Error.ProtonData(errorCode, error)
            val deleteResponse = ApiResult.Error.Http(errorCode, error, protonErrorData)
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code $errorCode")
            )

            every { parameters.inputData } returns workDataOf(KEY_INPUT_DATA_LABEL_IDS to arrayOf(labelId))
            coEvery { api.deleteLabel(testUserId, any()) } returns deleteResponse

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

}
