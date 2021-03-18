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
import ch.protonmail.android.attachments.KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR
import ch.protonmail.android.attachments.UploadAttachments
import ch.protonmail.android.core.Constants.MessageActionType.FORWARD
import ch.protonmail.android.core.Constants.MessageActionType.NONE
import ch.protonmail.android.core.Constants.MessageActionType.REPLY
import ch.protonmail.android.core.Constants.MessageActionType.REPLY_ALL
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_DRAFT
import ch.protonmail.android.core.Constants.MessageLocationType.ALL_MAIL
import ch.protonmail.android.core.Constants.MessageLocationType.DRAFT
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.PendingSend
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.usecase.compose.SaveDraft.SaveDraftParameters
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.worker.drafts.CreateDraftWorker.Enqueuer
import ch.protonmail.android.worker.drafts.CreateDraftWorkerErrors
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_ERROR_ENUM
import ch.protonmail.android.worker.drafts.KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
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
import kotlin.test.Test

class SaveDraftTest : CoroutinesTest {

    private val userNotifier: UserNotifier = mockk(relaxed = true)

    private val uploadAttachments: UploadAttachments = mockk(relaxed = true)
    private val uploadAttachmentsEnqueuer: UploadAttachments.Enqueuer = mockk(relaxed = true)

    private val createDraftScheduler: Enqueuer = mockk(relaxed = true)

    private val pendingActionDao: PendingActionDao = mockk(relaxed = true)

    private val addressCryptoFactory: AddressCrypto.Factory = mockk(relaxed = true)

    private val messageDetailsRepository: MessageDetailsRepository = mockk(relaxed = true)

    private val currentUserId = Id("Id")

    private val saveDraft = SaveDraft(
        addressCryptoFactory,
        messageDetailsRepository,
        dispatchers,
        pendingActionDao,
        createDraftScheduler,
        currentUserId,
        uploadAttachmentsEnqueuer,
        userNotifier
    )

    @Test
    fun saveDraftEncryptsMessageAndSavesItToDbWhenTriggerIsUserRequestedSave() {
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
            every { addressCryptoFactory.create(currentUserId, Id("addressId")) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessage(message) } returns 123L

            // When
            saveDraft(
                SaveDraftParameters(
                    message,
                    emptyList(),
                    null,
                    FORWARD,
                    "previousSenderId1273",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
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
            coVerify { messageDetailsRepository.saveMessage(expectedMessage) }
        }
    }

    @Test
    fun saveDraftEncryptsMessageAndSavesItToDbWhenTriggerIsAutoSave() {
        runBlockingTest {
            // Given
            val message = Message().apply {
                dbId = 8923L
                this.messageId = "8234"
                addressID = "addressIdAutoSave"
                decryptedBody = "Message body in plain text auto saving"
            }
            val encryptedBody = "encrypted armored content auto saving"
            val addressCrypto = mockk<AddressCrypto> {
                every { encrypt("Message body in plain text auto saving", true).armored } returns encryptedBody
            }
            every { addressCryptoFactory.create(currentUserId, Id("addressIdAutoSave")) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessage(message) } returns 8923L

            // When
            saveDraft(
                SaveDraftParameters(
                    message,
                    emptyList(),
                    null,
                    FORWARD,
                    "previousSenderId1273",
                    SaveDraft.SaveDraftTrigger.AutoSave
                )
            )

            // Then
            val expectedMessage = message.copy(messageBody = encryptedBody)
            expectedMessage.setLabelIDs(
                listOf(
                    ALL_DRAFT.messageLocationTypeValue.toString(),
                    ALL_MAIL.messageLocationTypeValue.toString(),
                    DRAFT.messageLocationTypeValue.toString()
                )
            )
            coVerify { messageDetailsRepository.saveMessage(expectedMessage) }
        }
    }

    @Test
    fun saveDraftDoesNotEncryptTheMessageAgainWhenTheTriggerIsSendingMessage() {
        runBlockingTest {
            // Given
            val encryptedBody = "message encrypted armored content, encrypted by SEND use case"
            val message = Message().apply {
                dbId = 823L
                this.messageId = "8234"
                addressID = "senderAddressId"
                messageBody = encryptedBody
                decryptedBody = null
            }
            coEvery { messageDetailsRepository.saveMessage(message) } returns 823L

            // When
            saveDraft(
                SaveDraftParameters(
                    message,
                    emptyList(),
                    null,
                    NONE,
                    "previousSenderId1273",
                    SaveDraft.SaveDraftTrigger.SendingMessage
                )
            )

            // Then
            val expectedMessage = message.copy(messageBody = encryptedBody)
            expectedMessage.setLabelIDs(
                listOf(
                    ALL_DRAFT.messageLocationTypeValue.toString(),
                    ALL_MAIL.messageLocationTypeValue.toString(),
                    DRAFT.messageLocationTypeValue.toString()
                )
            )
            coVerify { messageDetailsRepository.saveMessage(expectedMessage) }
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
            every { addressCryptoFactory.create(currentUserId, Id("addressId")) } returns addressCrypto
            coEvery { messageDetailsRepository.saveMessage(message) } returns 7237L
            mockkStatic(Timber::class)

            // When
            saveDraft(
                SaveDraftParameters(
                    message,
                    emptyList(),
                    null,
                    FORWARD,
                    "previousSenderId1273",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
            )

            // Then
            val messageCaptor = slot<Message>()
            verify {
                Timber.w("Save Draft for messageId 456 - Decrypted Body was null, proceeding...")
            }
            coVerify { messageDetailsRepository.saveMessage(capture(messageCaptor)) }
            assertEquals("encrypted empty message body", messageCaptor.captured.messageBody)
            unmockkStatic(Timber::class)
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
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L

            // When
            saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    emptyList(),
                    "parentId123",
                    REPLY_ALL,
                    "previousSenderId1273",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
            )

            // Then
            verify {
                createDraftScheduler.enqueue(
                    userId = currentUserId,
                    message = message,
                    parentId = "parentId123",
                    actionType = REPLY_ALL,
                    previousSenderAddressId = "previousSenderId1273"
                )
            }
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
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            every {
                createDraftScheduler.enqueue(
                    userId = currentUserId,
                    message = message,
                    parentId = "parentId123",
                    actionType = REPLY_ALL,
                    previousSenderAddressId = "previousSenderId1273"
                )
            } answers { flowOf(null) }

            // When
            saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    emptyList(),
                    "parentId123",
                    REPLY_ALL,
                    "previousSenderId1273",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
            )

            // Then
            coVerify(exactly = 0) { pendingActionDao.findPendingSendByMessageId("456") }
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
            coEvery { messageDetailsRepository.findMessageById(any()) } returns flowOf(null)
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            every { pendingActionDao.findPendingSendByMessageId("45623") } answers {
                PendingSend(
                    "234234", localMessageId, null, false, 834L
                )
            }
            val workOutputData = workDataOf(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to "createdDraftMessageId"
            )
            val workerStatusFlow = buildWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            every {
                createDraftScheduler.enqueue(
                    userId = currentUserId,
                    message = message,
                    parentId = "parentId234",
                    actionType = REPLY_ALL,
                    previousSenderAddressId = "previousSenderId132423"
                )
            } answers { workerStatusFlow }
            coEvery { uploadAttachmentsEnqueuer.enqueue(any(), "createdDraftMessageId", false) } returns buildWorkerResponse(
                WorkInfo.State.SUCCEEDED
            )

            // When
            saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    emptyList(),
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
            ).first()

            // Then
            val expected = PendingSend("234234", "createdDraftMessageId", null, false, 834L)
            verify { pendingActionDao.insertPendingForSend(expected) }
        }
    }

    @Test
    fun saveDraftsCallsUploadAttachmentsUseCaseToUploadNewAttachmentsWhenSavingWasTriggeredByTheUser() {
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
            val workerStatusFlow = buildWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            val newAttachmentIds = listOf("2345", "453")
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("45623") } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById("createdDraftMessageId345") } returns flowOf(apiDraft)
            every {
                createDraftScheduler.enqueue(
                    userId = currentUserId,
                    message = message,
                    parentId = "parentId234",
                    actionType = REPLY_ALL,
                    previousSenderAddressId = "previousSenderId132423"
                )
            } answers { workerStatusFlow }
            val addressCrypto = mockk<AddressCrypto>(relaxed = true)
            every { addressCryptoFactory.create(currentUserId, Id("addressId")) } returns addressCrypto
            coEvery { uploadAttachmentsEnqueuer.enqueue(any(), "createdDraftMessageId345", false) } returns buildWorkerResponse(
                WorkInfo.State.SUCCEEDED
            )

            // When
            saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    newAttachmentIds,
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
            ).first()

            // Then
            coVerify { uploadAttachmentsEnqueuer.enqueue(newAttachmentIds, "createdDraftMessageId345", false) }
        }
    }

    @Test
    fun saveDraftsCallsUploadAttachmentsUseCaseToUploadNewAttachmentsWhenSavingWasTriggeredBySendingMessage() {
        runBlockingTest {
            // Given
            val localDraftId = "83432"
            val message = Message().apply {
                dbId = 123L
                this.messageId = "456832"
                addressID = "addressId1"
                decryptedBody = "Message body in plain text"
                localId = localDraftId
            }
            val apiDraft = message.copy(messageId = "createdDraftMessageId346")
            val workOutputData = workDataOf(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to "createdDraftMessageId346"
            )
            val workerStatusFlow = buildWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            val newAttachmentIds = listOf("2345", "453")
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("45623") } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById("createdDraftMessageId346") } returns flowOf(apiDraft)
            every {
                createDraftScheduler.enqueue(
                    currentUserId,
                    message,
                    "parentId235",
                    REPLY_ALL,
                    "previousSenderId132424"
                )
            } answers { workerStatusFlow }
            val addressCrypto = mockk<AddressCrypto>(relaxed = true)
            every { addressCryptoFactory.create(currentUserId, Id("addressId")) } returns addressCrypto
            coEvery { uploadAttachmentsEnqueuer.enqueue(any(), "createdDraftMessageId346", true) } returns buildWorkerResponse(
                WorkInfo.State.SUCCEEDED
            )

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    newAttachmentIds,
                    "parentId235",
                    REPLY_ALL,
                    "previousSenderId132424",
                    SaveDraft.SaveDraftTrigger.SendingMessage
                )
            ).first()

            // Then
            coVerify { uploadAttachmentsEnqueuer.enqueue(newAttachmentIds, "createdDraftMessageId346", true) }
            assertEquals(SaveDraftResult.Success(apiDraft.messageId!!), result)
        }
    }

    @Test
    fun saveDraftsDoesNotCallUploadAttachmentsUseCaseWhenSavingWasTriggeredByAutoSave() {
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
            val workerStatusFlow = buildWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            val newAttachmentIds = listOf("2345", "453")
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("45623") } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById("createdDraftMessageId345") } returns flowOf(apiDraft)
            every {
                createDraftScheduler.enqueue(
                    currentUserId,
                    message,
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423"
                )
            } answers { workerStatusFlow }
            val addressCrypto = mockk<AddressCrypto>(relaxed = true)
            every { addressCryptoFactory.create(currentUserId, Id("addressId")) } returns addressCrypto

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    newAttachmentIds,
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423",
                    SaveDraft.SaveDraftTrigger.AutoSave
                )
            ).first()

            // Then
            coVerify(exactly = 0) { uploadAttachmentsEnqueuer.enqueue(any(), any(), any()) }
            assertEquals(SaveDraftResult.Success(apiDraft.messageId!!), result)
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
            val workerStatusFlow = buildWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("45623") } returns flowOf(message)
            every {
                createDraftScheduler.enqueue(
                    userId = currentUserId,
                    message = message,
                    parentId = "parentId234",
                    actionType = REPLY_ALL,
                    previousSenderAddressId = "previousSenderId132423"
                )
            } answers { workerStatusFlow }


            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    emptyList(),
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
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
            val createDraftOutputData = workDataOf(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to "newDraftId"
            )
            val createDraftWorkerResult = buildWorkerResponse(WorkInfo.State.SUCCEEDED, createDraftOutputData)
            val errorMessage = "Can't upload attachments"
            val uploadWorkOutputData = workDataOf(
                KEY_OUTPUT_RESULT_UPLOAD_ATTACHMENTS_ERROR to errorMessage
            )
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("newDraftId") } returns flowOf(message.copy(messageId = "newDraftId"))
            coEvery { messageDetailsRepository.findMessageById("45623") } returns flowOf(message)
            coEvery { uploadAttachmentsEnqueuer.enqueue(newAttachmentIds, "newDraftId", false) } returns buildWorkerResponse(
                WorkInfo.State.FAILED,
                uploadWorkOutputData
            )
            every {
                createDraftScheduler.enqueue(
                    userId = currentUserId,
                    message = message,
                    parentId = "parentId234",
                    actionType = REPLY,
                    previousSenderAddressId = "previousSenderId132423"
                )
            } answers { createDraftWorkerResult }

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    newAttachmentIds,
                    "parentId234",
                    REPLY,
                    "previousSenderId132423",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
            ).first()

            // Then
            verify { userNotifier.showPersistentError(errorMessage, "Message Subject") }
            assertEquals(SaveDraftResult.UploadDraftAttachmentsFailed, result)
        }
    }

    @Test
    fun saveDraftsReturnsErrorWhenBackgroundWorkToUploadNewAttachmentsIsCancelled() {
        runBlockingTest {
            // Given
            val localDraftId = "832834"
            val message = Message().apply {
                dbId = 8234L
                this.messageId = "2374"
                addressID = "addressId"
                decryptedBody = "Message body in plain text"
                localId = localDraftId
                subject = "Message Subject"
            }
            val newAttachmentIds = listOf("23456", "4531")
            val createDraftOutputData = workDataOf(
                KEY_OUTPUT_RESULT_SAVE_DRAFT_MESSAGE_ID to "newDraftId2384"
            )
            val createDraftWorkerResult = buildWorkerResponse(WorkInfo.State.SUCCEEDED, createDraftOutputData)
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            coEvery {
                messageDetailsRepository.findMessageById("UploadDraftAttachmentsFailed")
            } returns flowOf(message.copy(messageId = "newDraftId2384"))
            coEvery { messageDetailsRepository.findMessageById("45623") } returns flowOf(message)
            coEvery { uploadAttachmentsEnqueuer.enqueue(newAttachmentIds, "newDraftId2384", false) } returns buildWorkerResponse(
                WorkInfo.State.CANCELLED
            )
            every {
                createDraftScheduler.enqueue(
                    currentUserId,
                    message,
                    "parentId234",
                    REPLY,
                    "previousSenderId132423"
                )
            } answers { createDraftWorkerResult }

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    newAttachmentIds,
                    "parentId234",
                    REPLY,
                    "previousSenderId132423",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
            ).first()

            // Then
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
            val workerStatusFlow = buildWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            val newAttachmentIds = listOf("2345", "453")
            coEvery { messageDetailsRepository.saveMessage(message) } returns 9833L
            coEvery { messageDetailsRepository.findMessageById("45623") } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById("createdDraftMessageId345") } returns flowOf(apiDraft)
            every {
                createDraftScheduler.enqueue(
                    userId = currentUserId,
                    message = message,
                    parentId = "parentId234",
                    actionType = REPLY_ALL,
                    previousSenderAddressId = "previousSenderId132423"
                )
            } answers { workerStatusFlow }
            val addressCrypto = mockk<AddressCrypto>(relaxed = true)
            every { addressCryptoFactory.create(currentUserId, Id("addressId")) } returns addressCrypto
            coEvery { uploadAttachmentsEnqueuer.enqueue(any(), "createdDraftMessageId345", false) } returns buildWorkerResponse(
                WorkInfo.State.SUCCEEDED
            )

            // When
            val result = saveDraft.invoke(
                SaveDraftParameters(
                    message,
                    newAttachmentIds,
                    "parentId234",
                    REPLY_ALL,
                    "previousSenderId132423",
                    SaveDraft.SaveDraftTrigger.UserRequested
                )
            ).first()

            // Then
            verify { uploadAttachmentsEnqueuer.enqueue(newAttachmentIds, "createdDraftMessageId345", false) }
            assertEquals(SaveDraftResult.Success("createdDraftMessageId345"), result)
        }
    }

    private fun buildWorkerResponse(
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

