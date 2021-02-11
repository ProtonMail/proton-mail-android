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
import ch.protonmail.android.attachments.UploadAttachments
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.usecase.compose.SaveDraft.SaveDraftParameters
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.worker.drafts.CreateDraftWorker.Enqueuer
import ch.protonmail.android.worker.drafts.CreateDraftWorkerErrors
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import timber.log.Timber
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class SaveDraftTest : CoroutinesTest {

    @RelaxedMockK
    private lateinit var userNotifier: UserNotifier

    @RelaxedMockK
    private lateinit var uploadAttachments: UploadAttachments.Enqueuer

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

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun saveDraftSavesEncryptedDraftMessageToDb() {
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
    }

    @Test
    fun saveDraftLogsAWarningAndSavesEncryptedDraftMessageToDbIfDecryptedMessageBodyIsNull() {
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 7237L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = null
            }
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt("", true).armored } returns "encrypted empty message body"
            }
            every { addressCryptoFactory.create(Id("addressId"), Name(currentUsername)) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 7237L
            mockkStatic(Timber::class)

            // When
            saveDraft(
                SaveDraftParameters(message, emptyList(), null, FORWARD, "previousSenderId1273")
            )

            // Then
            val messageCaptor = slot<Message>()
            verify {
                Timber.w("Save Draft for messageId 456 - Decrypted Body was null, proceeding...")
            }
            coVerify { messageDetailsRepository.saveMessageLocally(capture(messageCaptor)) }
            assertEquals("encrypted empty message body", messageCaptor.captured.messageBody)
            unmockkStatic(Timber::class)
        }
    }

    @Test
    fun saveDraftsDoesNotInsertsPendingUploadWhenThereAreNoNewAttachments() {
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
    }

    @Test
    fun saveDraftsSchedulesCreateDraftWorker() {
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
                SaveDraftParameters(message, emptyList(), "parentId123", REPLY_ALL, "previousSenderId1273")
            )

            // Then
            verify { createDraftScheduler.enqueue(message, "parentId123", REPLY_ALL, "previousSenderId1273") }
        }
    }

    @Test
    fun saveDraftsIgnoresEmissionsFromCreateDraftWorkerWhenWorkInfoIsNull() {
        // This test is needed to ensure CreateDraftWorker is returning a flow of (Optional) WorkInfo?
        // as this is possible because of `getWorkInfoByIdLiveData` implementation
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            every {
                createDraftScheduler.enqueue(message, "parentId123", REPLY_ALL, "previousSenderId1273")
            } answers { flowOf(null) }

            // When
            saveDraft.invoke(
                SaveDraftParameters(message, emptyList(), "parentId123", REPLY_ALL, "previousSenderId1273")
            )

            // Then\
            coVerify(exactly = 0) { messageDetailsRepository.findMessageById(any()) }
        }
    }

    @Test
    fun saveDraftsUpdatesPendingForSendingMessageIdWithNewApiDraftIdWhenWorkerSucceedsAndMessageIsPendingForSending() {
        runBlockingTest {
            // Given
            val localMessageId = "45623"
            val message = Message().apply {
                dbId = 123L
                this.messageId = localMessageId
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
            }
            coEvery { messageDetailsRepository.findMessageById(any()) } returns null
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            every { pendingActionsDao.findPendingSendByMessageId("45623") } answers {
                PendingSend(
                    "234234", localMessageId, null, false, 834L
                )
            }
            val workOutputData = workDataOf(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to "createdDraftMessageId"
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
            coEvery { uploadAttachments.enqueue(any(), "createdDraftMessageId") } returns buildCreateDraftWorkerResponse(
                WorkInfo.State.SUCCEEDED
            )

            // When
            saveDraft.invoke(
                SaveDraftParameters(message, emptyList(), "parentId234", REPLY_ALL, "previousSenderId132423")
            ).first()

            // Then
            val expected = PendingSend("234234", "createdDraftMessageId", null, false, 834L)
            verify { pendingActionsDao.insertPendingForSend(expected) }
        }
    }

    @Test
    fun saveDraftsCallsUploadAttachmentsUseCaseToUploadNewAttachments() {
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
                KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to "createdDraftMessageId345"
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            val newAttachmentIds = listOf("2345", "453")
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("45623") } returns message
            coEvery { messageDetailsRepository.findMessageById("createdDraftMessageId345") } returns apiDraft
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
            coEvery { uploadAttachments.enqueue(any(), "createdDraftMessageId345") } returns buildCreateDraftWorkerResponse(
                WorkInfo.State.SUCCEEDED
            )

            // When
            saveDraft.invoke(
                SaveDraftParameters(message, newAttachmentIds, "parentId234", REPLY_ALL, "previousSenderId132423")
            ).first()

            // Then
            coVerify { uploadAttachments.enqueue(newAttachmentIds, "createdDraftMessageId345") }
        }
    }

    @Test
    fun saveDraftsReturnsFailureWhenWorkerFailsCreatingDraftOnAPI() {
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
                KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM to CreateDraftWorkerErrors.ServerError.name
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("45623") } returns message
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
            assertEquals(SaveDraftResult.OnlineDraftCreationFailed, result)
        }
    }

    @Test
    fun saveDraftsShowPersistentErrorAndReturnsErrorWhenUploadingNewAttachmentsFails() {
        runBlockingTest {
            // Given
            val localDraftId = "8345"
            val message = Message().apply {
                dbId = 123L
                this.messageId = "45623"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
                localId = localDraftId
                subject = "Message Subject"
            }
            val newAttachmentIds = listOf("2345", "453")
            val workOutputData = workDataOf(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to "newDraftId"
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            val errorMessage = "Can't upload attachments"
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("newDraftId") } returns message.copy(messageId = "newDraftId")
            coEvery { messageDetailsRepository.findMessageById("45623") } returns message
            coEvery { uploadAttachments.enqueue(newAttachmentIds, "newDraftId") } returns buildCreateDraftWorkerResponse(
                WorkInfo.State.FAILED
            )
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
            verify { userNotifier.showPersistentError(errorMessage, "Message Subject") }
            assertEquals(SaveDraftResult.UploadDraftAttachmentsFailed, result)
        }
    }

    @Test
    fun saveDraftReturnsSuccessWhenBothDraftCreationAndAttachmentsUploadSucceeds() {
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
                KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to "createdDraftMessageId345"
            )
            val workerStatusFlow = buildCreateDraftWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            val newAttachmentIds = listOf("2345", "453")
            coEvery { messageDetailsRepository.saveMessageLocally(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("45623") } returns message
            coEvery { messageDetailsRepository.findMessageById("createdDraftMessageId345") } returns apiDraft
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
            coEvery { uploadAttachments.enqueue(any(), "createdDraftMessageId345") } returns buildCreateDraftWorkerResponse(
                WorkInfo.State.SUCCEEDED
            )

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(message, newAttachmentIds, "parentId234", REPLY_ALL, "previousSenderId132423")
            ).first()

            // Then
            assertEquals(SaveDraftResult.Success("createdDraftMessageId345"), result)
        }
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

