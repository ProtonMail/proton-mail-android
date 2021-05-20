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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the behaviour of [MarkConversationsReadRemoteWorker].
 */
class MarkConversationsReadRemoteWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    private val protonMailApiManager = mockk<ProtonMailApiManager>()

    private lateinit var markConversationsReadRemoteWorker: MarkConversationsReadRemoteWorker
    private lateinit var markConversationsReadRemoteWorkerEnqueuer: MarkConversationsReadRemoteWorker.Enqueuer

    @BeforeTest
    fun setUp() {
        markConversationsReadRemoteWorker = MarkConversationsReadRemoteWorker(
            context,
            workerParameters,
            protonMailApiManager
        )
        markConversationsReadRemoteWorkerEnqueuer = MarkConversationsReadRemoteWorker.Enqueuer(
            workManager
        )
    }

    @Test
    fun shouldEnqueueWorkerSuccessfullyWhenEnqueuerIsCalled() {
        // given
        val conversationIds = listOf("conversationId1", "conversationId2")
        val operationMock = mockk<Operation>()
        every { workManager.enqueue(any<WorkRequest>()) } returns operationMock

        // when
        val operationResult = markConversationsReadRemoteWorkerEnqueuer.enqueue(conversationIds)

        // then
        assertEquals(operationMock, operationResult)
    }

    @Test
    fun shouldReturnSuccessIfApiCallIsSuccessful() {
        runBlockingTest {
            // given
            val conversationIdsArray = arrayOf("conversationId1", "conversationId2")
            val tokenString = "token"
            val validUntilLong: Long = 123
            every { workerParameters.inputData.getStringArray(KEY_MARK_READ_WORKER_CONVERSATION_IDS) } returns conversationIdsArray
            coEvery { protonMailApiManager.markConversationsRead(any()) } returns mockk {
                every { code } returns Constants.RESPONSE_CODE_MULTIPLE_OK
                every { undoToken } returns mockk {
                    every { token } returns tokenString
                    every { validUntil } returns validUntilLong
                }
            }

            val expectedResult = ListenableWorker.Result.success(
                workDataOf(
                    KEY_MARK_READ_WORKER_UNDO_TOKEN to tokenString,
                    KEY_MARK_READ_WORKER_VALID_UNTIL to validUntilLong
                )
            )

            // when
            val result = markConversationsReadRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfConversationIdsListIsNull() {
        runBlockingTest {
            // given
            every { workerParameters.inputData.getStringArray(KEY_MARK_READ_WORKER_CONVERSATION_IDS) } returns null

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_MARK_READ_WORKER_ERROR_DESCRIPTION to "Conversation ids list is null")
            )

            // when
            val result = markConversationsReadRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnRetryIfApiCallFails() {
        runBlockingTest {
            // given
            val conversationIdsArray = arrayOf("conversationId1", "conversationId2")
            every { workerParameters.inputData.getStringArray(KEY_MARK_READ_WORKER_CONVERSATION_IDS) } returns conversationIdsArray
            coEvery { protonMailApiManager.markConversationsRead(any()) } throws IOException()

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val result = markConversationsReadRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }
}
