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
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.jobs.helper.EmbeddedImage
import ch.protonmail.android.worker.KEY_WORKER_ERROR_DESCRIPTION
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DownloadEmbeddedAttachmentsWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @MockK
    private lateinit var attachmentsHelper: AttachmentsHelper

    @RelaxedMockK
    private lateinit var handleSingleAttachment: HandleSingleAttachment

    @RelaxedMockK
    private lateinit var handleEmbeddedImages: HandleEmbeddedImageAttachments

    @InjectMockKs
    private lateinit var worker: DownloadEmbeddedAttachmentsWorker

    private var dispatcherProvider = TestDispatcherProvider

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyWorkerFailsWithNoMessageIdAndUsernameProvided() = runBlockingTest {
        // given
        val expected = ListenableWorker.Result.failure(
            workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty messageId or username")
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
            val userName = "userNameAbc"
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
            every { parameters.inputData.getString(KEY_INPUT_DATA_USERNAME_STRING) } returns userName
            coEvery { messageDetailsRepository.findSearchMessageById(testMessageId) } returns null
            coEvery { messageDetailsRepository.findMessageById(testMessageId) } returns message
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
            val userName = "userNameAbc"
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
            every { parameters.inputData.getString(KEY_INPUT_DATA_USERNAME_STRING) } returns userName
            every { parameters.inputData.getString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING) } returns attachmentId1
            coEvery { messageDetailsRepository.findSearchMessageById(testMessageId) } returns null
            coEvery { messageDetailsRepository.findMessageById(testMessageId) } returns message
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
