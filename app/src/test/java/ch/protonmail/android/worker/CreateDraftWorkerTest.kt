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
import androidx.work.Data
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.NewMessage
import ch.protonmail.android.api.models.messages.receive.MessageFactory
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CreateDraftWorkerTest : CoroutinesTest {


    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var messageFactory: MessageFactory

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    private lateinit var pendingActionsDao: PendingActionsDao

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @InjectMockKs
    private lateinit var worker: CreateDraftWorker

    @Test
    fun workerEnqueuerCreatesOneTimeRequestWorkerWithParams() {
        runBlockingTest {
            val messageParentId = "98234"
            val messageLocalId = "2834"
            val messageDbId = 534L
            val message = Message(messageLocalId)
            message.dbId = messageDbId
            val requestSlot = slot<OneTimeWorkRequest>()
            every { workManager.enqueue(capture(requestSlot)) } answers { mockk() }

            CreateDraftWorker.Enqueuer(workManager).enqueue(message, messageParentId)

            val constraints = requestSlot.captured.workSpec.constraints
            val inputData = requestSlot.captured.workSpec.input
            val actualMessageDbId = inputData.getLong(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID, -1)
            val actualMessageLocalId = inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_LOCAL_ID)
            val actualMessageParentId = inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID)
            assertEquals(message.dbId, actualMessageDbId)
            assertEquals(message.messageId, actualMessageLocalId)
            assertEquals(messageParentId, actualMessageParentId)
            assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
            verify { workManager.getWorkInfoByIdLiveData(any()) }
        }
    }

    @Test
    fun workerReturnsSendingInProgressErrorWhenMessageIsAlreadyBeingSent() {
        runBlockingTest {
            val messageDbId = 345L
            val message = Message().apply { dbId = messageDbId }
            givenMessageIdInput(messageDbId)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { pendingActionsDao.findPendingSendByDbId(messageDbId) } returns PendingSend("anyMessageId")

            val result = worker.doWork()

            val error = CreateDraftWorker.CreateDraftWorkerErrors.SendingInProgressError
            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerReturnsMessageNotFoundErrorWhenMessageDetailsRepositoryDoesNotReturnAValidMessage() {
        runBlockingTest {
            val messageDbId = 345L
            givenMessageIdInput(messageDbId)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns null

            val result = worker.doWork()

            val error = CreateDraftWorker.CreateDraftWorkerErrors.MessageNotFound
            val expectedFailure = ListenableWorker.Result.failure(
                Data.Builder().putString(KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerCreatesDraftAPIObjectWithParentIdWhenParentIdIsGiven() {
        runBlockingTest {
            val parentId = "89345"
            val messageDbId = 345L
            val message = Message().apply { dbId = messageDbId }
            val apiDraftMessage = mockk<NewMessage>(relaxed = true)
            givenMessageIdInput(messageDbId)
            givenParentIdInput(parentId)
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            every { pendingActionsDao.findPendingSendByDbId(messageDbId) } returns null
            every { messageFactory.createDraftApiMessage(message) } answers { apiDraftMessage }

            worker.doWork()

            verify { apiDraftMessage.setParentID(parentId) }
        }
    }

    private fun givenParentIdInput(parentId: String) {
        every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_PARENT_ID) } answers { parentId }
    }

    private fun givenMessageIdInput(messageDbId: Long) {
        every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_DRAFT_MESSAGE_DB_ID, -1) } answers { messageDbId }
    }
}
