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

package ch.protonmail.android.labels.data.remote.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.worker.KEY_WORKER_ERROR_DESCRIPTION
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteLabelsWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var api: ProtonMailApiManager

    @MockK
    private lateinit var accountManager: AccountManager

    private lateinit var worker: DeleteLabelsWorker

    private val dispatchers = TestDispatcherProvider()

    private val testUserId = UserId("testUser")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        worker = DeleteLabelsWorker(
            context,
            parameters,
            api,
            dispatchers,
            accountManager
        )
    }

    @Test
    fun verifyWorkerFailsWithNoLabelIdProvided() {
        runTest(dispatchers.Main) {
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
        runTest(dispatchers.Main) {
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
        runTest(dispatchers.Main) {
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
