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
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests the behaviour of [EmptyFolderRemoteWorker]
 */
class EmptyFolderRemoteWorkerTest {

    private val labelId = "labelId"
    private val userId = UserId("userId")
    private val userIdTag = UserIdTag(userId)

    private val context = mockk<Context>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)
    private val protonMailApiManager = mockk<ProtonMailApiManager>()
    private val workerParameters = mockk<WorkerParameters>(relaxed = true) {
        every {
            inputData.getString(KEY_EMPTY_FOLDER_LABEL_ID)
        } returns labelId
        every {
            inputData.getString(KEY_EMPTY_FOLDER_USER_ID)
        } returns userId.id
        every {
            runAttemptCount
        } returns 1
    }

    private lateinit var emptyFolderRemoteWorker: EmptyFolderRemoteWorker
    private lateinit var emptyFolderRemoteWorkerEnqueuer: EmptyFolderRemoteWorker.Enqueuer

    @BeforeTest
    fun setUp() {
        emptyFolderRemoteWorker = EmptyFolderRemoteWorker(
            context,
            workerParameters,
            protonMailApiManager
        )
        emptyFolderRemoteWorkerEnqueuer = EmptyFolderRemoteWorker.Enqueuer(
            workManager
        )
    }

    @Test
    fun `should enqueue worker successfully when enqueuer is called`() {
        // given
        val operationMock = mockk<Operation>()
        every { workManager.enqueue(any<WorkRequest>()) } returns operationMock

        // when
        val operationResult = emptyFolderRemoteWorkerEnqueuer.enqueue(userId, labelId)

        // then
        assertEquals(operationMock, operationResult)
    }

    @Test
    fun `should return success if API call is successful`() {
        runBlockingTest {
            // given
            coEvery { protonMailApiManager.emptyFolder(userIdTag, labelId) } just runs

            val expectedResult = ListenableWorker.Result.success()

            // when
            val result = emptyFolderRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `should return failure if label id is null`() {
        runBlockingTest {
            // given
            every {
                workerParameters.inputData.getString(KEY_EMPTY_FOLDER_LABEL_ID)
            } returns null

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_EMPTY_FOLDER_ERROR_DESCRIPTION to "Input data is not complete")
            )

            // when
            val result = emptyFolderRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `should return retry if API call fails and run attempts do not exceed the limit`() {
        runBlockingTest {
            // given
            coEvery { protonMailApiManager.emptyFolder(userIdTag, labelId) } throws IOException()

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val result = emptyFolderRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun `should return failure if API call fails and run attempts exceed the limit`() {
        runBlockingTest {
            // given
            every {
                workerParameters.runAttemptCount
            } returns EMPTY_FOLDER_MAX_RUN_ATTEMPTS + 1
            coEvery {
                protonMailApiManager.emptyFolder(userIdTag, labelId)
            } throws IOException()

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_EMPTY_FOLDER_ERROR_DESCRIPTION to "Run attempts exceeded the limit")
            )

            // when
            val result = emptyFolderRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }
}
