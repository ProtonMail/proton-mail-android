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

package ch.protonmail.android.mailbox.domain.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkRequest
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.labels.data.remote.worker.KEY_UPDATE_LABELS_CONVERSATION_IDS
import ch.protonmail.android.labels.data.remote.worker.KEY_UPDATE_LABELS_ERROR_DESCRIPTION
import ch.protonmail.android.labels.data.remote.worker.KEY_UPDATE_LABELS_SELECTED_LABELS
import ch.protonmail.android.labels.data.remote.worker.KEY_UPDATE_LABELS_UNSELECTED_LABELS
import ch.protonmail.android.labels.data.remote.worker.KEY_UPDATE_LABELS_USER_ID
import ch.protonmail.android.labels.data.remote.worker.UpdateConversationsLabelsWorker
import ch.protonmail.android.labels.domain.usecase.UpdateConversationsLabels
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the behaviour of [UpdateConversationsLabelsWorker]
 */
class UpdateConversationsLabelsWorkerTest {

    private val context = mockk<Context>(relaxed = true)
    private val workerParameters = mockk<WorkerParameters>(relaxed = true)
    private val workManager = mockk<WorkManager>(relaxed = true)

    private val updateConversationsLabels: UpdateConversationsLabels = mockk()

    private lateinit var updateConversationsLabelsWorker: UpdateConversationsLabelsWorker
    private lateinit var updateConversationsLabelsWorkerEnqueuer: UpdateConversationsLabelsWorker.Enqueuer

    @BeforeTest
    fun setUp() {
        updateConversationsLabelsWorker = UpdateConversationsLabelsWorker(
            context,
            workerParameters,
            updateConversationsLabels
        )
        updateConversationsLabelsWorkerEnqueuer = UpdateConversationsLabelsWorker.Enqueuer(
            workManager
        )
    }

    @Test
    fun shouldEnqueueWorkerSuccessfullyWhenEnqueuerIsCalled() {
        // given
        val conversationIds = listOf("conversationId1", "conversationId2")
        val userId = UserId("userId")
        val selectedLabels = listOf("label1")
        val unselectedLabels = listOf("label2", "label3")
        val operationMock = mockk<Operation>()
        every { workManager.enqueue(any<WorkRequest>()) } returns operationMock

        // when
        val operationResult = updateConversationsLabelsWorkerEnqueuer.enqueue(
            conversationIds,
            userId,
            selectedLabels
        )

        // then
        assertEquals(operationMock, operationResult)
    }

    @Test
    fun shouldReturnSuccessWhenUseCaseReturnsSuccess() {
        runBlockingTest {
            // given
            val conversationIds = arrayOf("conversationId1", "conversationId2")
            val userId = "userId"
            val selectedLabels = arrayOf("label1")
            val unselectedLabels = arrayOf("label2", "label3")
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_CONVERSATION_IDS)
            } returns conversationIds
            every {
                workerParameters.inputData.getString(KEY_UPDATE_LABELS_USER_ID)
            } returns userId
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_SELECTED_LABELS)
            } returns selectedLabels
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_UNSELECTED_LABELS)
            } returns unselectedLabels
            coEvery {
                updateConversationsLabels(
                    conversationIds.toList(),
                    UserId(userId),
                    selectedLabels.toList()
                )
            } returns ConversationsActionResult.Success

            val expectedResult = ListenableWorker.Result.success()

            // when
            val workerResult = updateConversationsLabelsWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun shouldReturnFailureWhenUseCaseReturnsError() {
        runBlockingTest {
            // given
            val conversationIds = arrayOf("conversationId1", "conversationId2")
            val userId = "userId"
            val selectedLabels = arrayOf("label1")
            val unselectedLabels = arrayOf("label2", "label3")
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_CONVERSATION_IDS)
            } returns conversationIds
            every {
                workerParameters.inputData.getString(KEY_UPDATE_LABELS_USER_ID)
            } returns userId
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_SELECTED_LABELS)
            } returns selectedLabels
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_UNSELECTED_LABELS)
            } returns unselectedLabels
            coEvery {
                updateConversationsLabels(
                    conversationIds.toList(),
                    UserId(userId),
                    selectedLabels.toList()
                )
            } returns ConversationsActionResult.Error

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(
                    KEY_UPDATE_LABELS_ERROR_DESCRIPTION to "Could not complete the action"
                )
            )

            // when
            val workerResult = updateConversationsLabelsWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun shouldReturnFailureWhenInputDataIsIncomplete() {
        runBlockingTest {
            // given
            val userId = "userId"
            val unselectedLabels = arrayOf("label2", "label3")
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_CONVERSATION_IDS)
            } returns arrayOf()
            every {
                workerParameters.inputData.getString(KEY_UPDATE_LABELS_USER_ID)
            } returns userId
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_SELECTED_LABELS)
            } returns null
            every {
                workerParameters.inputData.getStringArray(KEY_UPDATE_LABELS_UNSELECTED_LABELS)
            } returns unselectedLabels

            val expectedResult = ListenableWorker.Result.failure(
                workDataOf(
                    KEY_UPDATE_LABELS_ERROR_DESCRIPTION to "Input data is incomplete"
                )
            )

            // when
            val workerResult = updateConversationsLabelsWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }
}
