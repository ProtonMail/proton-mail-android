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

package ch.protonmail.android.attachments

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.worker.KEY_WORKER_ERROR_DESCRIPTION
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadEmbeddedAttachmentsWorkerTest {

    private val context: Context = mockk(relaxed = true)

    private val parameters: WorkerParameters = mockk(relaxed = true)

    private val userManager: UserManager = mockk(relaxed = true)

    private val messageDetailsRepository: MessageDetailsRepository = mockk(relaxed = true)

    private val attachmentsHelper: AttachmentsHelper = mockk()

    private val handleSingleAttachment: HandleSingleAttachment = mockk(relaxed = true)

    private val handleEmbeddedImages: HandleEmbeddedImageAttachments = mockk(relaxed = true)

    private val worker = DownloadEmbeddedAttachmentsWorker(
        context,
        parameters,
        userManager,
        messageDetailsRepository,
        attachmentsHelper,
        handleSingleAttachment,
        handleEmbeddedImages
    )

    @Test
    fun verifyWorkerFailsWithNoMessageIdProvided() = runBlockingTest {
        // given
        every { parameters.inputData.getString(KEY_INPUT_DATA_USER_ID_STRING) } returns "userId"
        val expected = ListenableWorker.Result.failure(
            workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty message id")
        )

        // when
        val operationResult = worker.doWork()

        // then
        assertEquals(expected, operationResult)
    }

    @Test
    fun verifyWorkerFailsWithNoUserIdProvided() = runBlockingTest {
        // given
        every { parameters.inputData.getString(KEY_INPUT_DATA_MESSAGE_ID_STRING) } returns "messageId"
        val expected = ListenableWorker.Result.failure(
            workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty user id")
        )

        // when
        val operationResult = worker.doWork()

        // then
        assertEquals(expected, operationResult)
    }

    @Test
    fun verifyWorkerRetrievesMessageEmbeddedAttachmentsDataCorrectly() {
        val contentId1 = "contentId1"
        val contentId2 = "contentId2"
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.success()
            val testMessageId = "MId1"
            val testAddressId = "addressId123"
            val userId = "userId"
            val isPgpMessage = true
            val attachmentId1 = "attachment1"
            val attachmentId2 = "attachment2"
            val attachment1 = Attachment(attachmentId1, keyPackets = "OriginalAttachmentPackets1", inline = true)
            val attachment2 = Attachment(attachmentId2, keyPackets = "OriginalAttachmentPackets2", inline = true)
            val attachments = listOf(attachment1, attachment2)
            val message = mockk<Message> {
                every { messageId } returns testMessageId
                every { addressID } returns testAddressId
                every { Attachments } returns attachments
                every { decrypt(any()) } returns Unit
                every { isPGPMime } returns isPgpMessage
                every { embeddedImageIds } returns listOf(attachmentId1, attachmentId2)
            }
            val embeddedImage1 = EmbeddedImage(
                attachmentId1,
                "fileNamePic1",
                "key1",
                "mimeType",
                "contentEncoding1",
                contentId1,
                null,
                10L,
                "messageId1",
                null
            )
            val embeddedImage2 = EmbeddedImage(
                attachmentId2,
                "fileNamePic2",
                "key2",
                "mimeType",
                "contentEncoding2",
                contentId2,
                null,
                10L,
                "messageId2",
                null
            )
            val embeddedImageIds = listOf(attachmentId1, attachmentId2)
            val embeddedImages = listOf(embeddedImage1, embeddedImage2)
            every { attachmentsHelper.fromAttachmentToEmbeddedImage(attachment1, embeddedImageIds) } returns embeddedImage1
            every { attachmentsHelper.fromAttachmentToEmbeddedImage(attachment2, embeddedImageIds) } returns embeddedImage2
            every { parameters.inputData.getString(KEY_INPUT_DATA_MESSAGE_ID_STRING) } returns testMessageId
            every { parameters.inputData.getString(KEY_INPUT_DATA_USER_ID_STRING) } returns userId
            every { messageDetailsRepository.findMessageById(testMessageId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageByIdBlocking(testMessageId) } returns message
            coEvery { messageDetailsRepository.findAttachmentsByMessageId(testMessageId) } returns attachments
            coEvery { handleEmbeddedImages.invoke(embeddedImages, any(), testMessageId) } returns expected

            // when
            val operationResult = worker.doWork()

            // then
            coVerify { handleEmbeddedImages.invoke(embeddedImages, any(), testMessageId) }
            assertEquals(expected, operationResult)
        }
    }


    @Test
    fun verifyWorkerRetrievesMessageSingleAttachmentDataCorrectly() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.success()
            val testMessageId = "MId1"
            val testAddressId = "addressId123"
            val userId = "userId"
            val isPgpMessage = true
            val attachmentId1 = "attachment1"
            val attachmentId2 = "attachment2"
            val attachment1 = Attachment(attachmentId1, keyPackets = "OriginalAttachmentPackets1", inline = true)
            val attachment2 = Attachment(attachmentId2, keyPackets = "OriginalAttachmentPackets2", inline = true)
            val attachments = listOf(attachment1, attachment2)
            val message = mockk<Message> {
                every { messageId } returns testMessageId
                every { addressID } returns testAddressId
                every { Attachments } returns attachments
                every { decrypt(any()) } returns Unit
                every { isPGPMime } returns isPgpMessage
                every { embeddedImageIds } returns listOf(attachmentId1, attachmentId2)
            }
            every { attachmentsHelper.fromAttachmentToEmbeddedImage(any(), any()) } returns null
            every { parameters.inputData.getString(KEY_INPUT_DATA_MESSAGE_ID_STRING) } returns testMessageId
            every { parameters.inputData.getString(KEY_INPUT_DATA_USER_ID_STRING) } returns userId
            every { parameters.inputData.getString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING) } returns attachmentId1
            every { messageDetailsRepository.findMessageById(testMessageId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageByIdBlocking(testMessageId) } returns message
            coEvery { messageDetailsRepository.findAttachmentsByMessageId(testMessageId) } returns attachments
            coEvery { handleSingleAttachment.invoke(any(), any(), testMessageId) } returns expected

            // when
            val operationResult = worker.doWork()

            // then
            coVerify { handleSingleAttachment.invoke(any(), any(), testMessageId) }
            assertEquals(expected, operationResult)
        }
    }

}
