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
import ch.protonmail.android.core.Constants
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the behaviour of [MarkConversationsUnreadRemoteWorker].
 */
class MarkConversationsUnreadRemoteWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    private val protonMailApiManager = mockk<ProtonMailApiManager>()

    private lateinit var markConversationsUnreadRemoteWorker: MarkConversationsUnreadRemoteWorker
    private lateinit var markConversationsUnreadRemoteWorkerEnqueuer: MarkConversationsUnreadRemoteWorker.Enqueuer

    @BeforeTest
    fun setUp() {
        markConversationsUnreadRemoteWorker = MarkConversationsUnreadRemoteWorker(
            context,
            workerParameters,
            protonMailApiManager
        )
        markConversationsUnreadRemoteWorkerEnqueuer = MarkConversationsUnreadRemoteWorker.Enqueuer(
            workManager
        )
    }

    @Test
    fun shouldEnqueueWorkerSuccessfullyWhenEnqueuerIsCalled() {
        // given
        val conversationIds = listOf("conversationId1", "conversationId2")
        val userId = UserId("userId")
        val operationMock = mockk<Operation>()
        val location = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()
        every { workManager.enqueue(any<WorkRequest>()) } returns operationMock

        // when
        val operationResult = markConversationsUnreadRemoteWorkerEnqueuer.enqueue(conversationIds, location, userId)

        // then
        assertEquals(operationMock, operationResult)
    }

    @Test
    fun shouldReturnSuccessIfApiCallIsSuccessful() {
        runBlockingTest {
            // given
            val conversationIdsArray = arrayOf("conversationId1", "conversationId2")
            val userId = "userId"
            val tokenString = "token"
            val validUntilLong: Long = 123
            every {
                workerParameters.inputData.getStringArray(
                    KEY_MARK_UNREAD_WORKER_CONVERSATION_IDS
                )
            } returns conversationIdsArray
            every { workerParameters.inputData.getString(KEY_MARK_UNREAD_WORKER_LABEL_ID) } returns "0"
            every { workerParameters.inputData.getString(KEY_MARK_UNREAD_WORKER_USER_ID) } returns userId
            coEvery { protonMailApiManager.markConversationsUnread(any(), UserId(userId)) } returns mockk {
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
            val result = markConversationsUnreadRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfConversationIdsListIsNull() {
        runBlockingTest {
            // given
            every { workerParameters.inputData.getStringArray(KEY_MARK_UNREAD_WORKER_CONVERSATION_IDS) } returns null

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(
                    KEY_MARK_READ_WORKER_ERROR_DESCRIPTION to "Conversation ids list or labelId or userId is invalid"
                )
            )

            // when
            val result = markConversationsUnreadRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnRetryIfApiCallFailsAndRunAttemptsDoNotExceedTheLimit() {
        runBlockingTest {
            // given
            val conversationIdsArray = arrayOf("conversationId1", "conversationId2")
            val userId = "userId"
            every {
                workerParameters.inputData.getStringArray(KEY_MARK_UNREAD_WORKER_CONVERSATION_IDS)
            } returns conversationIdsArray
            every { workerParameters.inputData.getString(KEY_MARK_UNREAD_WORKER_LABEL_ID) } returns "0"
            every { workerParameters.inputData.getString(KEY_MARK_UNREAD_WORKER_USER_ID) } returns userId
            coEvery { protonMailApiManager.markConversationsUnread(any(), UserId(userId)) } throws IOException()

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val result = markConversationsUnreadRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun shouldReturnFailureIfApiCallFailsAndRunAttemptsExceedTheLimit() {
        runBlockingTest {
            // given
            val conversationIdsArray = arrayOf("conversationId1", "conversationId2")
            val userId = "userId"
            every {
                workerParameters.inputData.getStringArray(KEY_MARK_UNREAD_WORKER_CONVERSATION_IDS)
            } returns conversationIdsArray
            every { workerParameters.inputData.getString(KEY_MARK_UNREAD_WORKER_LABEL_ID) } returns "0"
            every { workerParameters.inputData.getString(KEY_MARK_UNREAD_WORKER_USER_ID) } returns userId
            every {
                workerParameters.runAttemptCount
            } returns 6
            coEvery { protonMailApiManager.markConversationsUnread(any(), UserId(userId)) } throws IOException()

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(KEY_MARK_UNREAD_WORKER_ERROR_DESCRIPTION to "Run attempts exceeded the limit")
            )

            // when
            val result = markConversationsUnreadRemoteWorker.doWork()

            // then
            assertEquals(expectedResult, result)
        }
    }
}
