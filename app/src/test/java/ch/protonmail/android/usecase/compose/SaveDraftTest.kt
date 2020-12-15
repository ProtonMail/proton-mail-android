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

package ch.protonmail.android.usecase.compose

import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingSend
import ch.protonmail.android.api.models.room.pendingActions.PendingUpload
import ch.protonmail.android.attachments.UploadAttachments
import ch.protonmail.android.attachments.UploadAttachments.Result.Failure
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.usecase.compose.SaveDraft.Result
import ch.protonmail.android.usecase.compose.SaveDraft.SaveDraftParameters
import ch.protonmail.android.worker.CreateDraftWorker.CreateDraftWorkerErrors
import ch.protonmail.android.worker.CreateDraftWorker.Enqueuer
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_ERROR_ENUM
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.util.UUID

@ExtendWith(MockKExtension::class)
class SaveDraftTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var uploadAttachments: UploadAttachments

    @RelaxedMockK
    private lateinit var createDraftScheduler: Enqueuer

    @RelaxedMockK
    private lateinit var pendingActionsDao: PendingActionsDao

    @RelaxedMockK
    private lateinit var addressCryptoFactory: AddressCrypto.Factory

    @RelaxedMockK
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @InjectMockKs
    lateinit var saveDraft: SaveDraft

    private val currentUsername = "username"

    @Test
    fun saveDraftSavesEncryptedDraftMessageToDb() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt("Message body in plain text", true).armored } returns "encrypted armored content"
            }
            every { addressCryptoFactory.create(Id("addressId"), Name(currentUsername)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 123L

            // When
            saveDraft(
                SaveDraftParameters(message, emptyList(), null, FORWARD, "previousSenderId1273")
            )

            // Then
            val expectedMessage = message.copy(messageBody = "encrypted armored content")
            expectedMessage.setLabelIDs(
                listOf(
                    ALL_DRAFT.messageLocationTypeValue.toString(),
                    ALL_MAIL.messageLocationTypeValue.toString(),
                    DRAFT.messageLocationTypeValue.toString()
                )
            )
            coVerify { messageDetailsRepository.saveMessageLocally(expectedMessage) }
        }

    @Test
    fun saveDraftInsertsPendingDraftInPendingActionsDatabase() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt("Message body in plain text", true).armored } returns "encrypted armored content"
            }
            every { addressCryptoFactory.create(Id("addressId"), Name(currentUsername)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 123L

            // When
            saveDraft(
                SaveDraftParameters(message, emptyList(), null, FORWARD, "previousSenderId1273")
            )

            // Then
            coVerify { messageDetailsRepository.insertPendingDraft(123L) }
        }

    @Test
    fun saveDraftsInsertsPendingUploadWhenThereAreNewAttachments() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt("Message body in plain text", true).armored } returns "encrypted armored content"
            }
            every { addressCryptoFactory.create(Id("addressId"), Name(currentUsername)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 123L

            // When
            val newAttachments = listOf("attachmentId")
            saveDraft.invoke(
                SaveDraftParameters(message, newAttachments, "parentId", REPLY, "previousSenderId1273")
            )

            // Then
            verify { pendingActionsDao.insertPendingForUpload(PendingUpload("456")) }
        }

    @Test
    fun saveDraftsDoesNotInsertsPendingUploadWhenThereAreNoNewAttachments() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L

            // When
            saveDraft.invoke(
                SaveDraftParameters(message, emptyList(), "parentId", FORWARD, "previousSenderId1273")
            )

            // Then
            verify(exactly = 0) { pendingActionsDao.insertPendingForUpload(any()) }
        }

    @Test
    fun sendDraftReturnsSendingInProgressErrorWhenMessageIsAlreadyBeingSent() =
        runBlockingTest {
            // Given
            val messageDbId = 345L
            val message = Message().apply {
                dbId = messageDbId
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            every { messageDetailsRepository.findMessageByMessageDbId(messageDbId) } returns message
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns messageDbId
            every { pendingActionsDao.findPendingSendByDbId(messageDbId) } returns PendingSend("anyMessageId")

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(message, emptyList(), "parentId123", FORWARD, "previousSenderId1273")
            )

            // Then
            val expectedError = Result.SendingInProgressError
            assertEquals(expectedError, result.first())
            verify(exactly = 0) { createDraftScheduler.enqueue(any(), any(), any(), any()) }
        }

    @Test
    fun saveDraftsSchedulesCreateDraftWorker() =
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            every { pendingActionsDao.findPendingSendByDbId(9833L) } returns null

            // When
            saveDraft.invoke(
                SaveDraftParameters(message, emptyList(), "parentId123", REPLY_ALL, "previousSenderId1273")
            )

            // Then
            verify { createDraftScheduler.enqueue(message, "parentId123", REPLY_ALL, "previousSenderId1273") }
        }

    @Test
    fun saveDraftsUpdatesPendingForSendingMessageIdWithNewApiDraftIdWhenWorkerSucceedsAndMessageIsPendingForSending() =
        runBlockingTest {
            // Given
            val localDraftId = "8345"
            val message = Message().apply {
                dbId = 123L
                this.messageId = "45623"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
                localId = localDraftId
            }
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            every { pendingActionsDao.findPendingSendByDbId(9833L) } returns null
            every { pendingActionsDao.findPendingSendByOfflineMessageId("45623") } answers {
                PendingSend(
                    "234234", localDraftId, "offlineId", false, 834L
                )
            }
            coEvery { uploadAttachments(any(), any(), any()) } returns UploadAttachments.Result.Success
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID to "createdDraftMessageId"
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            every {
                createDraftScheduler.enqueue(
                    message,
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423"
                )
            } answers { workerStatusFlow }

            // When
            saveDraft.invoke(
                SaveDraftParameters(message, emptyList(), "parentId234", REPLY_ALL, "previousSenderId132423")
            ).first()

            // Then
            val expected = PendingSend("234234", "createdDraftMessageId", "offlineId", false, 834L)
            verify { pendingActionsDao.insertPendingForSend(expected) }
        }

    @Test
    fun saveDraftsDeletesOfflineDraftWhenCreatingRemoteDraftThroughApiSucceds() =
        runBlockingTest {
            // Given
            val localDraftId = "8345"
            val message = Message().apply {
                dbId = 123L
                this.messageId = "45623"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
                localId = localDraftId
            }
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            every { messageDetailsRepository.findMessageById("45623") } returns message
            every { pendingActionsDao.findPendingSendByDbId(9833L) } returns null
            every { pendingActionsDao.findPendingSendByOfflineMessageId(localDraftId) } returns PendingSend()
            coEvery { uploadAttachments(any(), any(), any()) } returns UploadAttachments.Result.Success
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID to "createdDraftMessageId345"
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            every {
                createDraftScheduler.enqueue(
                    message,
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423"
                )
            } answers { workerStatusFlow }

            // When
            saveDraft.invoke(
                SaveDraftParameters(message, emptyList(), "parentId234", REPLY_ALL, "previousSenderId132423")
            ).first()

            // Then
            verify { messageDetailsRepository.deleteMessage(message) }
        }

    @Test
    fun saveDraftsCallsUploadAttachmentsUseCaseToUploadNewAttachments() =
        runBlockingTest {
            // Given
            val localDraftId = "8345"
            val message = Message().apply {
                dbId = 123L
                this.messageId = "45623"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
                localId = localDraftId
            }
            val apiDraft = message.copy(messageId = "createdDraftMessageId345")
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID to "createdDraftMessageId345"
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            val newAttachmentIds = listOf("2345", "453")
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            every { messageDetailsRepository.findMessageById("45623") } returns message
            every { messageDetailsRepository.findMessageById("createdDraftMessageId345") } returns apiDraft
            every { pendingActionsDao.findPendingSendByDbId(9833L) } returns null
            every { pendingActionsDao.findPendingSendByOfflineMessageId(localDraftId) } returns PendingSend()
            coEvery { uploadAttachments(any(), apiDraft, any()) } returns UploadAttachments.Result.Success
            every {
                createDraftScheduler.enqueue(
                    message,
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423"
                )
            } answers { workerStatusFlow }
            val addressCrypto = mockk<AddressCrypto>(relaxed = true)
            every { addressCryptoFactory.create(Id("addressId"), Name(currentUsername)) } returns addressCrypto

            // When
            saveDraft.invoke(
                SaveDraftParameters(message, newAttachmentIds, "parentId234", REPLY_ALL, "previousSenderId132423")
            ).first()

            // Then
            coVerify { uploadAttachments(newAttachmentIds, apiDraft, addressCrypto) }
        }

    @Test
    fun saveDraftsReturnsFailureWhenWorkerFailsCreatingDraftOnAPI() =
        runBlockingTest {
            // Given
            val localDraftId = "8345"
            val message = Message().apply {
                dbId = 123L
                this.messageId = "45623"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
                localId = localDraftId
            }
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_ERROR_ENUM to CreateDraftWorkerErrors.ServerError.name
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            every { messageDetailsRepository.findMessageById("45623") } returns message
            every { pendingActionsDao.findPendingSendByDbId(9833L) } returns null
            every {
                createDraftScheduler.enqueue(
                    message,
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423"
                )
            } answers { workerStatusFlow }

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(message, emptyList(), "parentId234", REPLY_ALL, "previousSenderId132423")
            ).first()

            // Then
            assertEquals(Result.OnlineDraftCreationFailed, result)
        }

    @Test
    fun saveDraftsReturnsErrorWhenUploadingNewAttachmentsFails() =
        runBlockingTest {
            // Given
            val localDraftId = "8345"
            val message = Message().apply {
                dbId = 123L
                this.messageId = "45623"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
                localId = localDraftId
            }
            val newAttachmentIds = listOf("2345", "453")
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_DRAFT_RESULT_MESSAGE_ID to "newDraftId"
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            every { messageDetailsRepository.findMessageById("45623") } returns message
            every { pendingActionsDao.findPendingSendByDbId(9833L) } returns null
            coEvery { uploadAttachments(newAttachmentIds, any(), any()) } returns Failure("Can't upload attachments")
            every {
                createDraftScheduler.enqueue(
                    message,
                    "parentId234",
                    REPLY,
                    "previousSenderId132423"
                )
            } answers { workerStatusFlow }

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(message, newAttachmentIds, "parentId234", REPLY, "previousSenderId132423")
            ).first()

            // Then
            assertEquals(Result.UploadDraftAttachmentsFailed, result)
        }


    private fun buildCreateDraftWorkerResponse(
        endState: WorkInfo.State,
        outputData: Data? = workDataOf()
    ): Flow<WorkInfo> {
        val workInfo = WorkInfo(
            UUID.randomUUID(),
            endState,
            outputData!!,
            emptyList(),
            outputData,
            0
        )
        return MutableStateFlow(workInfo)
    }

}

