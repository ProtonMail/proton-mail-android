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

package ch.protonmail.android.mailbox.data.remote.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.Constants
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals

class PostToLocationWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    private val protonMailApiManager = mockk<ProtonMailApiManager>()

    private val postToLocationWorker = PostToLocationWorker(
        context,
        workerParameters,
        protonMailApiManager
    )
    private val postToLocationWorkerEnqueuer = PostToLocationWorker.Enqueuer(
        workManager
    )

    @Test
    fun shouldEnqueueWorkerSuccessfullyWhenEnqueuerIsCalled() {
        // given
        val messageIds = listOf("id1", "id2")
        val newLocation = Constants.MessageLocationType.INBOX
        val operationMock = mockk<Operation>()
        every { workManager.enqueue(any<WorkRequest>()) } returns operationMock

        // when
        val operationResult = postToLocationWorkerEnqueuer.enqueue(messageIds, newLocation)

        // then
        assertEquals(operationMock, operationResult)
    }

    @Test
    fun shouldReturnSuccessIfApiCallIsSuccessful() {
        runBlockingTest {
            // given
            val messageIds = arrayOf("id1", "id2")
            val newLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue
            every {
                workerParameters.inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
            } returns messageIds
            every {
                workerParameters.inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
            } returns newLocation
            coEvery { protonMailApiManager.labelMessages(any()) } returns mockk()

            val expectedResult = ListenableWorker.Result.success()

            // when
            val result = postToLocationWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfMessagesIdsListIsNull() {
        runBlockingTest {
            // given
            val newLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue
            every {
                workerParameters.inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
            } returns null
            every {
                workerParameters.inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
            } returns newLocation
            coEvery { protonMailApiManager.labelMessages(any()) } returns mockk()

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_LABEL_WORKER_ERROR_DESCRIPTION to "Input data is not complete")
            )

            // when
            val result = postToLocationWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnRetryIfApiCallFailsAndRunAttemptsDoNotExceedTheLimit() {
        runBlockingTest {
            // given
            val messageIds = arrayOf("id1", "id2")
            val newLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue
            every {
                workerParameters.inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
            } returns messageIds
            every {
                workerParameters.inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
            } returns newLocation
            coEvery { protonMailApiManager.labelMessages(any()) } throws IOException()

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val result = postToLocationWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfApiCallFailsAndRunAttemptsExceedTheLimit() {
        runBlockingTest {
            // given
            val messageIds = arrayOf("id1", "id2")
            val newLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue
            every {
                workerParameters.inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
            } returns messageIds
            every {
                workerParameters.inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
            } returns newLocation
            every {
                workerParameters.runAttemptCount
            } returns 4
            coEvery { protonMailApiManager.labelMessages(any()) } throws IOException()

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_LABEL_WORKER_ERROR_DESCRIPTION to "Run attempts exceeded the limit")
            )

            // when
            val result = postToLocationWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }
}
