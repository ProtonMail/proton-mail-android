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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.crypto.AddressCrypto
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class UploadAttachmentsTest : CoroutinesTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var attachmentsRepository: AttachmentsRepository

    @RelaxedMockK
    private lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    private lateinit var crypto: AddressCrypto

    @InjectMockKs
    private lateinit var uploadAttachments: UploadAttachments

    @Test
    fun uploadAttachmentsCallsRepositoryWhenInputAttachmentsAreValid() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments.invoke(attachmentIds, message, crypto)

            verifySequence {
                attachmentsRepository.upload(attachment1, crypto)
                attachmentsRepository.upload(attachment2, crypto)
            }
        }
    }

    @Test
    fun uploadAttachmentsReturnsFailureIfAnyAttachmentFailsToBeUploaded() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2
            every { attachmentsRepository.upload(attachment2, crypto) } answers {
                AttachmentsRepository.Result.Failure("Failed to upload attachment2")
            }

            val result = uploadAttachments.invoke(attachmentIds, message, crypto)

            val expected = UploadAttachments.Result.Failure("Failed to upload attachment2")
            assertEquals(expected, result)
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentIsNotFoundInMessageRepository() {
        runBlockingTest {
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns null
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments.invoke(attachmentIds, message, crypto)

            verifySequence { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentFilePathIsNull() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns null
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments.invoke(attachmentIds, message, crypto)

            verify(exactly = 0) { attachmentsRepository.upload(attachment1, crypto) }
            verify { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentWasAlreadyUploaded() {
        runBlockingTest {
            val attachment1 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "1"
                every { filePath } returns "filePath1"
                every { isUploaded } returns false
                every { isFileExisting } returns true
            }
            val attachment2 = mockk<Attachment>(relaxed = true) {
                every { attachmentId } returns "2"
                every { filePath } returns "filePath2"
                every { isUploaded } returns true
                every { isFileExisting } returns true
            }
            val attachmentIds = listOf("1", "2")
            val message = Message()
            every { messageDetailsRepository.findAttachmentById("1") } returns attachment1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachment2

            uploadAttachments.invoke(attachmentIds, message, crypto)

            verify { attachmentsRepository.upload(attachment1, crypto) }
            verify(exactly = 0) { attachmentsRepository.upload(attachment2, crypto) }
        }
    }

    @Test
    fun uploadAttachmentsSkipsUploadingIfAttachmentFileDoesNotExist() {
        runBlockingTest {
            val attachmentIds = listOf("1", "2")
            val message = Message()
            val attachmentMock1 = mockk<Attachment>(relaxed = true) {
                every { isFileExisting } returns true
                every { filePath } returns "filePath1"
                every { attachmentId } returns "1"
                every { isUploaded } returns false
            }
            val attachmentMock2 = mockk<Attachment>(relaxed = true) {
                every { isFileExisting } returns false
                every { filePath } returns "filePath2"
                every { attachmentId } returns "2"
                every { isUploaded } returns false
            }
            every { messageDetailsRepository.findAttachmentById("1") } returns attachmentMock1
            every { messageDetailsRepository.findAttachmentById("2") } returns attachmentMock2

            uploadAttachments.invoke(attachmentIds, message, crypto)

            verify { attachmentsRepository.upload(attachmentMock1, crypto) }
            verify(exactly = 0) { attachmentsRepository.upload(attachmentMock2, crypto) }
        }
    }
}


