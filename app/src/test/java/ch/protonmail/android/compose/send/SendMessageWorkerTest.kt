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

package ch.protonmail.android.compose.send

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.interceptors.UserIdTag
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.enumerations.MIMEType
import ch.protonmail.android.api.models.enumerations.PackageType
import ch.protonmail.android.api.models.factories.MessageSecurityOptions
import ch.protonmail.android.api.models.factories.PackageFactory
import ch.protonmail.android.api.models.factories.SendPreferencesFactory
import ch.protonmail.android.api.models.messages.send.MessageSendBody
import ch.protonmail.android.api.models.messages.send.MessageSendKey
import ch.protonmail.android.api.models.messages.send.MessageSendPackage
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.DetailedException
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.core.apiError
import ch.protonmail.android.core.messageId
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.pendingaction.data.worker.CleanUpPendingSendWorker
import ch.protonmail.android.testdata.UserTestData
import ch.protonmail.android.testdata.UserTestData.userId
import ch.protonmail.android.testdata.WorkerTestData
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.compose.SaveDraftResult
import ch.protonmail.android.utils.TryWithRetry
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.worker.repository.WorkerRepository
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.kotlin.deserialize
import me.proton.core.util.kotlin.serialize
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import timber.log.Timber
import java.net.SocketTimeoutException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.test.Test

class SendMessageWorkerTest : CoroutinesTest by CoroutinesTest() {

    private val context: Context = mockk(relaxed = true)

    private val parameters: WorkerParameters = mockk(relaxed = true)

    private val messageDetailsRepository: MessageDetailsRepository = mockk(relaxed = true)

    private val workManager: WorkManager = mockk(relaxed = true)

    private val saveDraft: SaveDraft = mockk(relaxed = true) {
        coEvery { this@mockk.invoke(any<SaveDraft.SaveDraftParameters>()) } returns
            SaveDraftResult.Success("newDraftId")
    }

    private val sendPreferencesFactoryAssistedFactory: SendPreferencesFactory.Factory = mockk(relaxed = true)

    private val sendPreferencesFactory: SendPreferencesFactory = mockk(relaxed = true)

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val userManager: UserManager = mockk(relaxed = true) {
        coEvery { getMailSettings(any()) } returns mockk(relaxed = true)
    }

    private val packageFactory: PackageFactory = mockk(relaxed = true)

    private val userNotifier: UserNotifier = mockk(relaxed = true)

    private val pendingActionDao: PendingActionDao = mockk(relaxed = true)

    private val databaseProvider: DatabaseProvider = mockk {
        every { providePendingActionDao(any()) } returns pendingActionDao
    }

    private val provideUniqueName: SendMessageWorker.ProvideUniqueName = mockk {
        every { this@mockk.invoke(any()) } returns WorkerTestData.UNIQUE_WORK_NAME
    }

    private val workerRepository: WorkerRepository = mockk {
        every { cancelUniqueWork(any()) } returns mockk()
    }

    private val provideUniqueCleanUpName: CleanUpPendingSendWorker.ProvideUniqueName = mockk {
        every { this@mockk.invoke(any()) } returns WorkerTestData.UNIQUE_WORK_NAME
    }

    private val worker = SendMessageWorker(
        context = context,
        params = parameters,
        messageDetailsRepository = messageDetailsRepository,
        saveDraft = saveDraft,
        sendPreferencesFactory = sendPreferencesFactoryAssistedFactory,
        apiManager = apiManager,
        packagesFactory = packageFactory,
        userManager = userManager,
        userNotifier = userNotifier,
        databaseProvider = databaseProvider,
        workerRepository = workerRepository,
        getCleanUpPendingSendWorkName = provideUniqueCleanUpName,
        tryWithRetry = TryWithRetry()
    )

    @Test
    fun workerEnqueuerCreatesOneTimeRequestWorkerWhichIsUniqueForMessageId() {
        runBlockingTest {
            // Given
            val messageParentId = "98234"
            val attachmentIds = listOf("attachmentId234", "238")
            val messageId = "2834"
            val messageDbId = 534L
            val messageActionType = Constants.MessageActionType.REPLY_ALL
            val message = Message(messageId = messageId).apply {
                this.dbId = messageDbId
            }
            val previousSenderAddressId = "previousSenderId82348"
            val securityOptions = MessageSecurityOptions("password", "hint", 3_273_727L)
            val testUserId = UserId("8234")
            every { userManager.currentUserId } returns testUserId
            every { userManager.requireCurrentUserId() } returns testUserId

            // When
            SendMessageWorker.Enqueuer(workManager, userManager, provideUniqueName).enqueue(
                userId,
                message,
                attachmentIds,
                messageParentId,
                messageActionType,
                previousSenderAddressId,
                securityOptions
            )

            // Then
            val requestSlot = slot<OneTimeWorkRequest>()
            verify {
                workManager.enqueueUniqueWork(
                    WorkerTestData.UNIQUE_WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    capture(requestSlot)
                )
            }
            val workSpec = requestSlot.captured.workSpec
            val constraints = workSpec.constraints
            val inputData = workSpec.input
            val actualMessageDbId = inputData.getLong(KEY_INPUT_SEND_MESSAGE_MSG_DB_ID, -1)
            val actualAttachmentIds = inputData.getStringArray(KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS)
            val actualMessageLocalId = inputData.getString(KEY_INPUT_SEND_MESSAGE_MESSAGE_ID)
            val actualUserId = UserId(requireNotNull(inputData.getString(KEY_INPUT_SEND_MESSAGE_CURRENT_USER_ID)))
            val actualMessageParentId = inputData.getString(KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID)
            val actualMessageActionType = inputData.getInt(KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_ENUM_VAL, -1)
            val actualPreviousSenderAddress = inputData.getString(KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID)
            val actualMessageSecurityOptions = inputData.getString(KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED)
            assertEquals(message.dbId, actualMessageDbId)
            assertEquals(message.messageId, actualMessageLocalId)
            assertArrayEquals(attachmentIds.toTypedArray(), actualAttachmentIds)
            assertEquals(messageParentId, actualMessageParentId)
            assertEquals(testUserId, actualUserId)
            assertEquals(messageActionType.messageActionTypeValue, actualMessageActionType)
            assertEquals(previousSenderAddressId, actualPreviousSenderAddress)
            assertEquals(
                securityOptions, actualMessageSecurityOptions?.deserialize(MessageSecurityOptions.serializer())
            )
            assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
            assertEquals(BackoffPolicy.LINEAR, workSpec.backoffPolicy)
            assertEquals(10_000, workSpec.backoffDelayDuration)
            verify { workManager.getWorkInfoByIdLiveData(any()) }
        }
    }

    @Test
    fun workerSavesDraftPassingAMessageAndTheNeededParameters() = runBlockingTest {
        val messageDbId = 2373L
        val messageId = "8322223"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
        }
        givenFullValidInput(
            messageDbId,
            messageId,
            arrayOf("attId8327"),
            "parentId82384",
            Constants.MessageActionType.NONE,
            "prevSenderAddress"
        )
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(any()) } returns flowOf(mockk(relaxed = true))
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(messageId)
        coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk(relaxed = true)

        worker.doWork()

        val expectedParameters = SaveDraft.SaveDraftParameters(
            userId = userId,
            message = message,
            newAttachmentIds = listOf("attId8327"),
            parentId = "parentId82384",
            actionType = Constants.MessageActionType.NONE,
            previousSenderAddressId = "prevSenderAddress",
            trigger = SaveDraft.SaveDraftTrigger.SendingMessage
        )
        coVerify { saveDraft(expectedParameters) }
    }

    @Test
    fun workerTriesFindingTheMessageByMessageIdWhenMessageIsNotFoundByDatabaseId() = runBlockingTest {
        val messageDbId = 23712L
        val messageId = "8322224-1341"
        val message = Message(messageId = messageId)
        val createdDraftId = "createdDraftId"
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(null)
        coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(createdDraftId) } returns flowOf(null)
        coEvery { saveDraft.invoke(any()) } returns SaveDraftResult.Success(createdDraftId)

        worker.doWork()

        verify { userNotifier wasNot Called }
        val paramsSlot = slot<SaveDraft.SaveDraftParameters>()
        coVerify { saveDraft.invoke(capture(paramsSlot)) }
        assertEquals(message, paramsSlot.captured.message)
    }

    @Test
    fun workerNotifiesUserAndFailsWhenMessageIsNotFoundInTheDatabase() = runBlockingTest {
        val messageDbId = 2373L
        val messageId = "8322223"
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(null)
        coEvery { messageDetailsRepository.findMessageById(messageId) } returns flowOf(null)
        every { context.getString(R.string.message_drafted) } returns "error message 9214"

        val result = worker.doWork()

        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "MessageNotFound")
            ),
            result
        )
        verify { userNotifier.showSendMessageError("error message 9214", "") }
        coVerify(exactly = 0) { saveDraft(any()) }
        verify { pendingActionDao.deletePendingSendByDbId(messageDbId) }
    }

    @Test
    fun workerFailsReturningErrorWhenSaveDraftOperationFails() = runBlockingTest {
        val messageDbId = 2834L
        val messageId = "823472"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = "Subject 003"
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { saveDraft(any()) } returns SaveDraftResult.OnlineDraftCreationFailed
        every { parameters.runAttemptCount } returns 2
        every { context.getString(R.string.message_drafted) } returns "error message 9216"

        val result = worker.doWork()

        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "DraftCreationFailed")
            ),
            result
        )
        verify { pendingActionDao.deletePendingSendByMessageId("823472") }
        verify { userNotifier.showSendMessageError("error message 9216", "Subject 003") }
    }

    @Test
    fun workerFailsWithoutNotifyingUserWhenSaveDraftFailsWithUploadAttachmentError() = runBlockingTest {
        // we notify the user from the Save Draft Worker,
        // refer to test `SaveDraftTest.notifyUserWithUploadAttachmentErrorWhenAttachmentIsBroken`
        val messageDbId = 2834L
        val messageId = "823472"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = "Subject 003"
            this.attachments = listOf(Attachment(attachmentId = "attachmentId234", fileName = "Attachment_234.jpg"))
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { saveDraft(any()) } returns SaveDraftResult.UploadDraftAttachmentsFailed
        every { parameters.runAttemptCount } returns 0

        val result = worker.doWork()

        verify { pendingActionDao.deletePendingSendByMessageId("823472") }
        verify { userNotifier wasNot Called }
        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "UploadAttachmentsFailed")
            ),
            result
        )
    }

    @Test
    fun workerFailsWithoutNotifyingUserWhenSaveDraftFailsWithMessageAlreadySentError() = runBlockingTest {
        val messageDbId = 2835L
        val messageId = "823473"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = "Subject 004"
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { saveDraft(any()) } returns SaveDraftResult.MessageAlreadySent
        every { parameters.runAttemptCount } returns 0

        val result = worker.doWork()

        verify { pendingActionDao.deletePendingSendByMessageId("823473") }
        verify { userNotifier wasNot Called }
        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "MessageAlreadySent")
            ),
            result
        )
    }

    @Test
    fun `worker fails and notifies user when save draft fails with InvalidSender error`() = runBlockingTest {
        val messageDbId = 2835L
        val messageId = "823473"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = "Subject 005"
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { saveDraft(any()) } returns SaveDraftResult.InvalidSender
        every { context.getString(R.string.notification_invalid_sender_sending_failed) } returns "error message 823473"

        val result = worker.doWork()

        verify { pendingActionDao.deletePendingSendByMessageId("823473") }
        verify { userNotifier.showSendMessageError("error message 823473", "Subject 005") }
        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "InvalidSender")
            ),
            result
        )
    }

    @Test
    fun workerGetsTheUpdatedMessageThatWasSavedAsDraftWhenSavingDraftSucceeds() = runBlockingTest {
        val messageDbId = 328_423L
        val messageId = "82384203"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
        }
        val savedDraftId = "234232"
        val savedDraftMessage = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns savedDraftId
            every { this@mockk.toListString } returns "recipientOnSavedDraft@pm.me"
        }
        val userId = UserId("randomUser1234823")
        givenFullValidInput(messageDbId, messageId, userId = userId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftId) } returns flowOf(savedDraftMessage)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftId)
        coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk(relaxed = true)
        every { sendPreferencesFactoryAssistedFactory.create(userId) } returns sendPreferencesFactory

        worker.doWork()

        coVerify { messageDetailsRepository.findMessageById(savedDraftId) }
        verify { sendPreferencesFactory.fetch(listOf("recipientOnSavedDraft@pm.me")) }
    }

    @Test
    fun workerRetriesWhenTheUpdatedMessageThatWasSavedAsDraftIsNotFoundInTheDbAndMaxRetriesWasNotReached() =
        runBlockingTest {
            val messageDbId = 38_472L
            val messageId = "29837462"
            val message = Message().apply {
                dbId = messageDbId
                this.messageId = messageId
            }
            val savedDraftId = "2836462"
            givenFullValidInput(messageDbId, messageId)
            coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById(savedDraftId) } returns flowOf(null)
            coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftId)
            every { parameters.runAttemptCount } returns 1

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.Retry(), result)
            verify(exactly = 0) { pendingActionDao.deletePendingSendByMessageId(any()) }
            verify(exactly = 0) { userNotifier.showSendMessageError(any(), any()) }
        }

    @Test
    fun workerFailsWhenTheUpdatedMessageThatWasSavedAsDraftIsNotFoundInTheDbAndMaxRetriesWasReached() =
        runBlockingTest {
            val messageDbId = 82_384L
            val messageId = "82384823"
            val message = Message().apply {
                dbId = messageDbId
                this.messageId = messageId
                this.subject = "Subject 002"
            }
            val savedDraftId = "2836463"
            givenFullValidInput(messageDbId, messageId)
            coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById(savedDraftId) } returns flowOf(null)
            coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftId)
            every { parameters.runAttemptCount } returns 4
            every { context.getString(R.string.message_drafted) } returns "error message 9213"

            val result = worker.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "SavedDraftMessageNotFound")
                ),
                result
            )
            verify { pendingActionDao.deletePendingSendByMessageId("82384823") }
            verify { userNotifier.showSendMessageError("error message 9213", "Subject 002") }
        }

    @Test
    fun workerFetchesSendPreferencesForEachUniqueRecipientWhenSavingDraftSucceeds() = runBlockingTest {
        val messageDbId = 2834L
        val messageId = "823472"
        val toRecipientEmail = "to-recipient@pm.me"
        val toRecipient1Email = "to-recipient-1@pm.me"
        val ccRecipientEmail = "cc-recipient@protonmail.com"
        val bccRecipientEmail = "bcc-recipient@protonmail.ch"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.toList = listOf(
                MessageRecipient("recipient", toRecipientEmail),
                MessageRecipient("duplicatedMailRecipient", toRecipientEmail),
                MessageRecipient("recipient1", toRecipient1Email)
            )
            this.ccList = listOf(MessageRecipient("recipient2", ccRecipientEmail))
            this.bccList = listOf(MessageRecipient("recipient3", bccRecipientEmail))
        }
        val savedDraftMessageId = "6234723"
        val savedDraft = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns savedDraftMessageId
            every { this@mockk.toListString } returns "$toRecipientEmail,$toRecipientEmail,$toRecipient1Email"
            every { this@mockk.ccListString } returns ccRecipientEmail
            every { this@mockk.bccListString } returns bccRecipientEmail
        }
        givenFullValidInput(messageDbId, messageId)
        every { sendPreferencesFactoryAssistedFactory.create(any()) } returns sendPreferencesFactory
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk(relaxed = true)

        worker.doWork()

        val recipientsCaptor = slot<List<String>>()
        verify { sendPreferencesFactory.fetch(capture(recipientsCaptor)) }
        assertTrue(
            recipientsCaptor.captured.containsAll(
                listOf(
                    toRecipient1Email,
                    ccRecipientEmail,
                    toRecipientEmail,
                    bccRecipientEmail
                )
            )
        )
        assertEquals(4, recipientsCaptor.captured.size)
    }

    @Test
    fun workerRetriesSendingMessageWhenFetchingSendPreferencesFailsAndMaxRetriesWasNotReached() = runBlockingTest {
        val messageDbId = 34_238L
        val messageId = "823720"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(any()) } returns flowOf(Message())
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(messageId)
        every { sendPreferencesFactoryAssistedFactory.create(any()) } returns sendPreferencesFactory
        every { sendPreferencesFactory.fetch(any()) } throws Exception("test - failed fetching send preferences")
        every { parameters.runAttemptCount } returns 1

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.Retry(), result)
        verify(exactly = 0) { pendingActionDao.deletePendingSendByMessageId(any()) }
        verify(exactly = 0) { userNotifier.showSendMessageError(any(), any()) }
    }

    @Test
    fun workerFailsWhenFetchingSendPreferencesFailsAndMaxRetriesWasReached() = runBlockingTest {
        val messageDbId = 8435L
        val messageId = "923482"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = "Subject 001"
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(any()) } returns flowOf(message)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(messageId)
        every { sendPreferencesFactoryAssistedFactory.create(any()) } returns sendPreferencesFactory
        every { sendPreferencesFactory.fetch(any()) } throws Exception("test - failed fetching send preferences")
        every { parameters.runAttemptCount } returns 4
        every { context.getString(R.string.message_drafted) } returns "error message"

        val result = worker.doWork()

        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "FetchSendPreferencesFailed")
            ),
            result
        )
        verify { pendingActionDao.deletePendingSendByMessageId("923482") }
        verify { userNotifier.showSendMessageError("error message", "Subject 001") }
    }

    @Test
    fun workerFetchesSendPreferencesOnlyForNonEmptyRecipientListsWhenSavingDraftSucceeds() = runBlockingTest {
        val messageDbId = 2834L
        val messageId = "823472"
        val toRecipientEmail = "to-recipient@pm.me"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.toList = listOf(MessageRecipient("recipient", toRecipientEmail))
            this.ccList = emptyList()
            this.bccList = listOf(MessageRecipient("emptyBccRecipient", ""))
        }
        val savedDraftMessageId = "6234723"
        val savedDraft = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns savedDraftMessageId
            every { this@mockk.toListString } returns toRecipientEmail
            every { this@mockk.ccListString } returns ""
            every { this@mockk.bccListString } returns ""
        }
        val userId = UserId("randomUser9234823")
        givenFullValidInput(messageDbId, messageId, userId = userId)
        every { sendPreferencesFactoryAssistedFactory.create(userId) } returns sendPreferencesFactory
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk(relaxed = true)

        worker.doWork()

        verify { sendPreferencesFactory.fetch(listOf(toRecipientEmail)) }
    }

    @Test
    fun workerDecryptsSavedDraftMessageContentBeforeCreatingRequestPackets() = runBlockingTest {
        val messageDbId = 8234L
        val messageId = "823742"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
        }
        val savedDraftMessageId = "23742"
        val savedDraftMessage = mockk<Message>(relaxed = true)
        val securityOptions = MessageSecurityOptions("password", "hint", 172800L)
        val currentUserId = UserId("user")
        givenFullValidInput(messageDbId, messageId, securityOptions = securityOptions, userId = currentUserId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraftMessage)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk(relaxed = true)

        worker.doWork()

        verify { savedDraftMessage.decrypt(userManager, currentUserId) }
    }

    @Test
    fun workerPerformsSendMessageApiCallWhenDraftWasSavedAndSendPreferencesFetchedSuccessfully() = runBlockingTest {
        val messageDbId = 82_321L
        val messageId = "233472"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
        }
        val savedDraftMessageId = "6234723"
        val messageSendKey = MessageSendKey("algorithm", "key")
        val packages = listOf(
            MessageSendPackage(
                "body",
                messageSendKey,
                MIMEType.PLAINTEXT,
                mapOf("key" to messageSendKey)
            )
        )
        val savedDraftMessage = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns savedDraftMessageId
        }
        val sendPreference = SendPreference(
            "email",
            true,
            true,
            MIMEType.HTML,
            "publicKey",
            PackageType.PGP_MIME,
            false,
            false,
            true,
            false
        )
        val sendPreferences = mapOf(
            "key" to sendPreference
        )
        val securityOptions = MessageSecurityOptions("password", "hint", 172_800L)
        val currentUserId = UserId("user")
        givenFullValidInput(messageDbId, messageId, securityOptions = securityOptions, userId = currentUserId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraftMessage)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        every { sendPreferencesFactoryAssistedFactory.create(currentUserId) } returns sendPreferencesFactory
        coEvery { userManager.getMailSettings(currentUserId).autoSaveContacts } returns 1
        every { sendPreferencesFactory.fetch(any()) } returns sendPreferences
        every {
            packageFactory.generatePackages(savedDraftMessage, listOf(sendPreference), securityOptions, currentUserId)
        } returns packages
        coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk(relaxed = true)

        worker.doWork()

        val expiresAfterSeconds = 172_800L
        val autoSaveContacts = userManager.getMailSettings(currentUserId).autoSaveContacts
        val requestBody = MessageSendBody(packages, expiresAfterSeconds, autoSaveContacts)
        val userIdTag = UserIdTag(currentUserId)
        coVerify { apiManager.sendMessage(savedDraftMessageId, requestBody, userIdTag) }
    }

    @Test
    fun workerReturnsErrorWhenMessageSecurityOptionsParameterWasNotGivenOrInvalid() = runBlockingTest {
        val messageDbId = 8_238_423L
        val messageId = "8234042"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = "Subject 002"
        }
        val savedDraftMessageId = "237684"
        val savedDraft = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns savedDraftMessageId
            every { this@mockk.subject } returns "Subject 002"
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        coEvery { userManager.getMailSettings(any()).autoSaveContacts } returns 1
        every { sendPreferencesFactory.fetch(any()) } returns mapOf()
        every { parameters.inputData.getString(KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED) } returns null
        every { context.getString(R.string.message_drafted) } returns "error message 9215"
        every { parameters.runAttemptCount } returns 4

        val result = worker.doWork()

        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "FailureBuildingApiRequest")
            ),
            result
        )
        verify { userNotifier.showSendMessageError("error message 9215", "Subject 002") }
        coVerify(exactly = 0) { packageFactory.generatePackages(any(), any(), any(), any()) }
    }

    @Test
    fun workerDefaultsToNotAutoSavingContactsWhenGettingAutoSaveContactsPreferenceFails() = runBlockingTest {
        val messageDbId = 823_742L
        val messageId = "923742"
        val message = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns messageId
        }
        val messageSendKey = MessageSendKey("algorithm", "key")
        val packages = listOf(
            MessageSendPackage(
                "body",
                messageSendKey,
                MIMEType.PLAINTEXT,
                mapOf("key" to messageSendKey)
            )
        )
        val savedDraftMessageId = "283472"
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        // The following mock reuses `message` defined above for convenience. It's actually the saved draft message
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(message)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        every { sendPreferencesFactory.fetch(any()) } returns mapOf()
        every { packageFactory.generatePackages(any(), any(), any(), any()) } returns packages
        coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk(relaxed = true)

        worker.doWork()

        val requestBody = MessageSendBody(packages, -1, 0)
        coVerify { apiManager.sendMessage(any(), requestBody, any()) }
    }

    @Test
    fun workerRetriesWhenPackageFactoryFailsThrowingAnExceptionAnMaxRetriesWereNotReached() = runBlockingTest {
        val messageDbId = 2_376_472L
        val messageId = "823742"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = "Subject 008"
        }
        val savedDraftMessageId = "9238427"
        val savedDraft = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns savedDraftMessageId
        }
        val exception = Exception("TEST - Failure creating packages")
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        every { sendPreferencesFactory.fetch(any()) } returns mapOf()
        every { packageFactory.generatePackages(any(), any(), any(), any()) } throws exception
        every { parameters.runAttemptCount } returns 1
        mockkStatic(Timber::class)

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.Retry(), result)
        verify(exactly = 0) { userNotifier.showSendMessageError(any(), any()) }
        verify(exactly = 0) { pendingActionDao.deletePendingSendByMessageId(any()) }
        verify { Timber.d(exception, "Send Message Worker failed with error = FailureBuildingApiRequest. Retrying...") }
        unmockkStatic(Timber::class)
    }

    @Test
    fun workerFailsWhenPackageFactoryFailsThrowingAnExceptionAndMaxRetriesWereReached() = runBlockingTest {
        val messageDbId = 8_234_723L
        val messageId = "7237723"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = "Subject 005"
        }
        val savedDraftMessageId = "7236438"
        val savedDraft = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns savedDraftMessageId
            every { this@mockk.subject } returns "Subject 005"
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        every { sendPreferencesFactory.fetch(any()) } returns mapOf()
        every { packageFactory.generatePackages(any(), any(), any(), any()) } throws Exception(
            "TEST - Failure creating packages"
        )
        every { parameters.runAttemptCount } returns 4
        every { context.getString(R.string.message_drafted) } returns "Error sending message"

        val result = worker.doWork()

        assertEquals(
            ListenableWorker.Result.failure(
                workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "FailureBuildingApiRequest")
            ),
            result
        )
        verify { userNotifier.showSendMessageError("Error sending message", "Subject 005") }
        verify { pendingActionDao.deletePendingSendByMessageId(savedDraftMessageId) }
    }

    @Test
    fun workerRetriesWhenSendMessageRequestFailsAndMaxRetriesWereNotReached() = runBlockingTest {
        val messageDbId = 723_743L
        val messageId = "8237426"
        val message = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns messageId
        }
        val savedDraftMessageId = "122748"
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        // The following mock reuses `message` defined above for convenience. It's actually the saved draft message
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(message)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        coEvery { userManager.getMailSettings(any()) } returns mockk()
        every { sendPreferencesFactory.fetch(any()) } returns mapOf()
        coEvery { apiManager.sendMessage(any(), any(), any()) } throws SocketTimeoutException("test - timeout")

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.Retry(), result)
        verify(exactly = 0) { userNotifier.showSendMessageError(any(), any()) }
        verify(exactly = 0) { pendingActionDao.deletePendingSendByMessageId(any()) }
    }

    @Test
    fun workerFailsRemovingPendingForSendMessageAndShowingErrorWhenSendMessageRequestFailsAndMaxRetriesWereReached() =
        runBlockingTest {
            val messageDbId = 823_742L
            val messageId = "122349"
            val subject = "message subject"
            val message = Message().apply {
                dbId = messageDbId
                this.messageId = messageId
                this.subject = subject
            }
            val savedDraftMessageId = "283472"
            val savedDraft = mockk<Message>(relaxed = true) {
                every { this@mockk.dbId } returns messageDbId
                every { this@mockk.messageId } returns savedDraftMessageId
                every { this@mockk.subject } returns subject
            }
            val errorMessage = "Sending Message Failed. Message is saved to drafts."
            givenFullValidInput(messageDbId, messageId)
            coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
            coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
            every { sendPreferencesFactory.fetch(any()) } returns mapOf()
            every { parameters.runAttemptCount } returns 4
            coEvery { apiManager.sendMessage(any(), any(), any()) } throws SocketTimeoutException(
                "test - call timed out"
            )
            every { context.getString(R.string.message_drafted) } returns errorMessage

            val result = worker.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "ErrorPerformingApiRequest")
                ),
                result
            )
            verify { userNotifier.showSendMessageError(errorMessage, subject) }
            verify { pendingActionDao.deletePendingSendByMessageId(savedDraftMessageId) }
        }

    @Test
    fun `remove pending send, move message to sent folder, and cancel cleanup worker when sending succeeds`() =
        runBlockingTest {
            val messageDbId = 234_827L
            val messageId = "9282384"
            val subject = "message subject"
            val message = Message().apply {
                dbId = messageDbId
                this.messageId = messageId
                this.subject = subject
            }
            val savedDraftMessageId = "283472"
            val savedDraft = mockk<Message>(relaxed = true) {
                every { this@mockk.dbId } returns messageDbId
                every { this@mockk.messageId } returns savedDraftMessageId
                every { this@mockk.subject } returns subject
            }
            givenFullValidInput(messageDbId, messageId)
            coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
            coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
            every { sendPreferencesFactory.fetch(any()) } returns mapOf()
            val apiResponseMessage = mockk<Message> {
                every { this@mockk.messageBody } returns "this is the body of the message that was sent"
                every { this@mockk.replyTos } returns listOf(MessageRecipient("recipient", "address@pm.me"))
                every { this@mockk.numAttachments } returns 3
                justRun { this@mockk.writeTo(savedDraft) }
            }
            coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk {
                every { code } returns 1_000
                every { sent } returns apiResponseMessage
            }

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.success(), result)
            verify { apiResponseMessage.writeTo(savedDraft) }
            verify { savedDraft.location = Constants.MessageLocationType.SENT.messageLocationTypeValue }
            verify {
                savedDraft.setLabelIDs(
                    listOf(
                        Constants.MessageLocationType.ALL_SENT.messageLocationTypeValue.toString(),
                        Constants.MessageLocationType.ALL_MAIL.messageLocationTypeValue.toString(),
                        Constants.MessageLocationType.SENT.messageLocationTypeValue.toString()
                    )
                )
            }

            coVerify { messageDetailsRepository.saveMessage(savedDraft) }
            verify(exactly = 1) { pendingActionDao.deletePendingSendByMessageId(savedDraftMessageId) }
            verify { workerRepository.cancelUniqueWork(WorkerTestData.UNIQUE_WORK_NAME) }
        }

    @Test
    fun `notify user and cancel clean up worker when send succeeds`() = runBlockingTest {
        val messageDbId = 9_282_384L
        val messageId = "982349"
        val subject = "message subject"
        val message = Message().apply {
            dbId = messageDbId
            this.messageId = messageId
            this.subject = subject
        }
        val savedDraftMessageId = "283472"
        val savedDraft = mockk<Message>(relaxed = true) {
            every { this@mockk.dbId } returns messageDbId
            every { this@mockk.messageId } returns savedDraftMessageId
            every { this@mockk.subject } returns subject
        }
        givenFullValidInput(messageDbId, messageId)
        coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
        coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
        coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
        every { sendPreferencesFactory.fetch(any()) } returns mapOf()
        coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk {
            every { code } returns 1_000
            every { sent } returns savedDraft
        }

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { userNotifier.showMessageSent() }
        verify { workerRepository.cancelUniqueWork(WorkerTestData.UNIQUE_WORK_NAME) }
    }

    @Test
    fun workerNotifiesUserOfTheSendingFailureAndRemovesPendingForSendWhenAPICallsReturnsFailureBodyCode() =
        runBlockingTest {
            val messageDbId = 2_132_372L
            val messageId = "8232832"
            val subject = "message subject 2"
            val message = Message().apply {
                dbId = messageDbId
                this.messageId = messageId
                this.subject = subject
            }
            val savedDraftMessageId = "923842"
            val savedDraft = mockk<Message>(relaxed = true) {
                every { this@mockk.dbId } returns messageDbId
                every { this@mockk.messageId } returns savedDraftMessageId
                every { this@mockk.subject } returns subject
            }
            val apiError = "Detailed API error explanation"
            val userErrorMessage = "Sending Message Failed! Message is saved to drafts."
            givenFullValidInput(messageDbId, messageId)
            coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
            coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
            every { sendPreferencesFactory.fetch(any()) } returns mapOf()
            coEvery { apiManager.sendMessage(any(), any(), any()) } returns mockk {
                every { code } returns 8_237
                every { sent } returns null
                every { error } returns apiError
            }
            every { context.getString(R.string.message_drafted) } returns userErrorMessage
            mockkStatic(Timber::class)

            val result = worker.doWork()

            assertEquals(
                ListenableWorker.Result.failure(
                    workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to "ApiRequestReturnedBadBodyCode")
                ),
                result
            )
            verify { userNotifier.showSendMessageError(userErrorMessage, subject) }
            verify { pendingActionDao.deletePendingSendByMessageId(savedDraftMessageId) }
            verify {
                Timber.e(
                    DetailedException()
                        .apiError(8237, "Detailed API error explanation")
                        .messageId("923842"),
                    "Send Message API call failed for messageId $savedDraftMessageId with error $apiError"
                )
            }
            unmockkStatic(Timber::class)
        }

    @Test(expected = CancellationException::class)
    fun workerFailsWithoutRetryingShowsErrorAndRethrowsExceptionWhenApiCallFailsWithExceptionDifferentThanIOException() =
        runBlockingTest {
            val messageDbId = 82_374L
            val messageId = "823482"
            val message = Message().apply {
                dbId = messageDbId
                this.messageId = messageId
                this.subject = "Subject 008"
            }
            val savedDraftMessageId = "82383"
            val savedDraft = mockk<Message>(relaxed = true) {
                every { this@mockk.dbId } returns messageDbId
                every { this@mockk.messageId } returns savedDraftMessageId
                every { this@mockk.subject } returns "Subject 000"
            }
            givenFullValidInput(messageDbId, messageId)
            coEvery { messageDetailsRepository.findMessageByDatabaseId(messageDbId) } returns flowOf(message)
            coEvery { messageDetailsRepository.findMessageById(savedDraftMessageId) } returns flowOf(savedDraft)
            coEvery { saveDraft(any()) } returns SaveDraftResult.Success(savedDraftMessageId)
            every { sendPreferencesFactory.fetch(any()) } returns mapOf()
            coEvery { apiManager.sendMessage(any(), any(), any()) } throws CancellationException("test - cancelled")
            every { context.getString(R.string.message_drafted) } returns "error message 8234"
            every { parameters.runAttemptCount } returns 1

            try {
                worker.doWork()
            } catch (exception: CancellationException) {
                verify { pendingActionDao.deletePendingSendByMessageId("82383") }
                verify { userNotifier.showSendMessageError("error message 8234", "Subject 008") }
                throw exception
            }
        }

    private fun givenFullValidInput(
        messageDbId: Long,
        messageId: String,
        attachments: Array<String> = arrayOf("attId62364"),
        parentId: String = "parentId72364",
        messageActionType: Constants.MessageActionType = Constants.MessageActionType.REPLY,
        previousSenderAddress: String = "prevSenderAddress923",
        securityOptions: MessageSecurityOptions? = MessageSecurityOptions(null, null, -1),
        userId: UserId = UserTestData.userId
    ) {
        every { parameters.inputData.getLong(KEY_INPUT_SEND_MESSAGE_MSG_DB_ID, -1) } answers { messageDbId }
        every { parameters.inputData.getStringArray(KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS) } answers { attachments }
        every { parameters.inputData.getString(KEY_INPUT_SEND_MESSAGE_MESSAGE_ID) } answers { messageId }
        every { parameters.inputData.getString(KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID) } answers { parentId }
        every { parameters.inputData.getString(KEY_INPUT_SEND_MESSAGE_CURRENT_USER_ID) } answers { userId.id }
        every { parameters.inputData.getInt(KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_ENUM_VAL, -1) } answers {
            messageActionType.messageActionTypeValue
        }
        every { parameters.inputData.getString(KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID) } answers {
            previousSenderAddress
        }
        every { parameters.inputData.getString(KEY_INPUT_SEND_MESSAGE_SECURITY_OPTIONS_SERIALIZED) } answers {
            securityOptions!!.serialize()
        }
    }
}

