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

package ch.protonmail.android.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_ID
import ch.protonmail.android.attachments.KEY_INPUT_DATA_ATTACHMENT_ID_STRING
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.model.Attachment
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
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
    private lateinit var messageDao: MessageDao

    @MockK
    private lateinit var api: ProtonMailApiManager

    private val dispatchers = TestDispatcherProvider()

    private lateinit var worker: DeleteAttachmentWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        worker = DeleteAttachmentWorker(context, parameters, api, messageDao, dispatchers)
    }

    @Test
    fun verifyWorkerFailsWithNoAttachmentIdProvided() {
        runTest(dispatchers.Main) {
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
        runTest(dispatchers.Main) {
            // given
            val attachmentId = "id232"
            val deleteResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            val attachment = mockk<Attachment>()
            val expected = ListenableWorker.Result.success()

            every { messageDao.findAttachmentById(attachmentId) } returns attachment
            every { messageDao.deleteAttachment(attachment) } returns mockk()
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
        runTest(dispatchers.Main) {
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
            every { messageDao.findAttachmentById(attachmentId) } returns attachment
            every { messageDao.deleteAttachment(attachment) } returns mockk()
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
        runTest(dispatchers.Main) {
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
            every { messageDao.findAttachmentById(attachmentId) } returns attachment
            every { messageDao.deleteAttachment(attachment) } returns mockk()
            every { parameters.inputData } returns
                workDataOf(KEY_INPUT_DATA_ATTACHMENT_ID_STRING to attachmentId)
            coEvery { api.deleteAttachment(any()) } returns deleteResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(expected, operationResult)
            verify { messageDao.deleteAttachment(attachment) }
        }
    }
}
