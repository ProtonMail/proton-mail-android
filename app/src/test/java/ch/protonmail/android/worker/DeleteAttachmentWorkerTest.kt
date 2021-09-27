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
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_ID
import ch.protonmail.android.attachments.KEY_INPUT_DATA_ATTACHMENT_ID_STRING
import ch.protonmail.android.core.Constants
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteAttachmentWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var messagesDb: MessagesDao

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var worker: DeleteAttachmentWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        worker = DeleteAttachmentWorker(context, parameters, api, messagesDb, TestDispatcherProvider)
    }

    @Test
    fun verifyWorkerFailsWithNoAttachmentIdProvided() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot delete attachment with an empty id")
            )

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifySuccessResultIsGeneratedWithRequiredParameters() {
        runBlockingTest {
            // given
            val attachmentId = "id232"
            val deleteResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            val attachment = mockk<Attachment>()
            val expected = ListenableWorker.Result.success()

            every { messagesDb.findAttachmentById(attachmentId) } returns attachment
            every { messagesDb.deleteAttachment(attachment) } returns mockk()
            every { parameters.inputData } returns
                workDataOf(KEY_INPUT_DATA_ATTACHMENT_ID_STRING to attachmentId)
            coEvery { api.deleteAttachment(any()) } returns deleteResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifyFailureResultIsGeneratedWithRequiredParametersButWrongBackendResponse() {
        runBlockingTest {
            // given
            val attachmentId = "id232"
            val randomErrorCode = 11212
            val errorMessage = "an error occurred"
            val deleteResponse = mockk<ResponseBody> {
                every { code } returns randomErrorCode
                every { error } returns errorMessage
            }
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code $randomErrorCode")
            )
            val attachment = mockk<Attachment>()
            every { messagesDb.findAttachmentById(attachmentId) } returns attachment
            every { messagesDb.deleteAttachment(attachment) } returns mockk()
            every { parameters.inputData } returns
                workDataOf(KEY_INPUT_DATA_ATTACHMENT_ID_STRING to attachmentId)
            coEvery { api.deleteAttachment(any()) } returns deleteResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifyThatServerErrorInvalidIdIsIgnoredAndMessageIsRemovedFromDbWithSuccess() {
        runBlockingTest {
            // given
            val attachmentId = "id232"
            val errorCode = RESPONSE_CODE_INVALID_ID
            val errorMessage = "Invalid ID"
            val deleteResponse = mockk<ResponseBody> {
                every { code } returns errorCode
                every { error } returns errorMessage
            }
            val expected = ListenableWorker.Result.success()
            val attachment = mockk<Attachment>()
            every { messagesDb.findAttachmentById(attachmentId) } returns attachment
            every { messagesDb.deleteAttachment(attachment) } returns mockk()
            every { parameters.inputData } returns
                workDataOf(KEY_INPUT_DATA_ATTACHMENT_ID_STRING to attachmentId)
            coEvery { api.deleteAttachment(any()) } returns deleteResponse

            // when
            val operationResult = worker.doWork()

            // then
            verify { messagesDb.deleteAttachment(attachment) }
            assertEquals(operationResult, expected)
        }
    }
}
