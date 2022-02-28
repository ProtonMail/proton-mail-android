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

package ch.protonmail.android.mailbox.data.remote.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Tests the behaviour of [DeleteConversationsRemoteWorker]
 */
class DeleteConversationsRemoteWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    private val protonMailApiManager = mockk<ProtonMailApiManager>()

    private lateinit var deleteConversationsRemoteWorker: DeleteConversationsRemoteWorker
    private lateinit var deleteConversationsRemoteWorkerEnqueuer: DeleteConversationsRemoteWorker.Enqueuer

    @BeforeTest
    fun setUp() {
        deleteConversationsRemoteWorker = DeleteConversationsRemoteWorker(
            context,
            workerParameters,
            protonMailApiManager
        )
        deleteConversationsRemoteWorkerEnqueuer = DeleteConversationsRemoteWorker.Enqueuer(
            workManager
        )
    }

    @Test
    fun shouldEnqueueWorkerSuccessfullyWhenEnqueuerIsCalled() {
        // given
        val conversationIds = listOf("conversationId1", "conversationId2")
        val labelId = "labelId"
        val userId = UserId("userId")
        val operationMock = mockk<Operation>()
        every { workManager.enqueue(any<WorkRequest>()) } returns operationMock

        // when
        val operationResult = deleteConversationsRemoteWorkerEnqueuer.enqueue(conversationIds, labelId, userId)

        // then
        assertEquals(operationMock, operationResult)
    }

    @Test
    fun shouldReturnSuccessIfApiCallIsSuccessful() {
        runBlockingTest {
            // given
            val conversationIdsArray = arrayOf("conversationId1", "conversationId2")
            val labelId = "labelId"
            val userId = "userId"
            every {
                workerParameters.inputData.getStringArray(KEY_DELETE_WORKER_CONVERSATION_IDS)
            } returns conversationIdsArray
            every {
                workerParameters.inputData.getString(KEY_DELETE_WORKER_LABEL_ID)
            } returns labelId
            every {
                workerParameters.inputData.getString(KEY_DELETE_WORKER_USER_ID)
            } returns userId
            coEvery { protonMailApiManager.deleteConversations(any(), any()) } returns mockk()

            val expectedResult = ListenableWorker.Result.success()

            // when
            val result = deleteConversationsRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfConversationIdsListIsNull() {
        runBlockingTest {
            // given
            val labelId = "labelId"
            val userId = "userId"
            every {
                workerParameters.inputData.getStringArray(KEY_DELETE_WORKER_CONVERSATION_IDS)
            } returns null
            every {
                workerParameters.inputData.getString(KEY_DELETE_WORKER_LABEL_ID)
            } returns labelId
            every {
                workerParameters.inputData.getString(KEY_DELETE_WORKER_USER_ID)
            } returns userId

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_DELETE_WORKER_ERROR_DESCRIPTION to "Input data is not complete")
            )

            // when
            val result = deleteConversationsRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnRetryIfApiCallFailsAndRunAttemptsDoNotExceedTheLimit() {
        runBlockingTest {
            // given
            val conversationIdsArray = arrayOf("conversationId1", "conversationId2")
            val labelId = "labelId"
            val userId = "userId"
            every {
                workerParameters.inputData.getStringArray(KEY_DELETE_WORKER_CONVERSATION_IDS)
            } returns conversationIdsArray
            every {
                workerParameters.inputData.getString(KEY_DELETE_WORKER_LABEL_ID)
            } returns labelId
            every {
                workerParameters.inputData.getString(KEY_DELETE_WORKER_USER_ID)
            } returns userId
            coEvery { protonMailApiManager.deleteConversations(any(), any()) } throws IOException()

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val result = deleteConversationsRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfApiCallFailsAndRunAttemptsExceedTheLimit() {
        runBlockingTest {
            // given
            val conversationIdsArray = arrayOf("conversationId1", "conversationId2")
            val labelId = "labelId"
            val userId = "userId"
            every {
                workerParameters.inputData.getStringArray(KEY_DELETE_WORKER_CONVERSATION_IDS)
            } returns conversationIdsArray
            every {
                workerParameters.inputData.getString(KEY_DELETE_WORKER_LABEL_ID)
            } returns labelId
            every {
                workerParameters.inputData.getString(KEY_DELETE_WORKER_USER_ID)
            } returns userId
            every {
                workerParameters.runAttemptCount
            } returns 6
            coEvery {
                protonMailApiManager.deleteConversations(any(), any())
            } throws IOException()

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_DELETE_WORKER_ERROR_DESCRIPTION to "Run attempts exceeded the limit")
            )

            // when
            val result = deleteConversationsRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }
}
