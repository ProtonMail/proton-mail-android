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

package ch.protonmail.android.compose

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkManager
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.factories.MessageSecurityOptions
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.services.PostMessageServiceFactory
import ch.protonmail.android.compose.send.SendMessage
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.testAndroid.rx.TrampolineScheduler
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.compose.SaveDraftResult
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchPublicKeys
import ch.protonmail.android.utils.UiUtil
import ch.protonmail.android.utils.resources.StringResourceResolver
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

class ComposeMessageViewModelTest : CoroutinesTest {

    @get:Rule
    val trampolineSchedulerRule = TrampolineScheduler()

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var stringResourceResolver: StringResourceResolver

    @RelaxedMockK
    lateinit var composeMessageRepository: ComposeMessageRepository

    @RelaxedMockK
    lateinit var userManager: UserManager

    @RelaxedMockK
    lateinit var messageDetailsRepository: MessageDetailsRepository

    @RelaxedMockK
    lateinit var saveDraft: SaveDraft

    @RelaxedMockK
    lateinit var sendMessage: SendMessage

    @MockK
    lateinit var postMessageServiceFactory: PostMessageServiceFactory

    @MockK
    lateinit var deleteMessage: DeleteMessage

    @MockK
    lateinit var fetchPublicKeys: FetchPublicKeys

    @MockK
    lateinit var networkConfigurator: NetworkConfigurator

    @MockK
    lateinit var verifyConnection: VerifyConnection

    @MockK
    lateinit var workManager: WorkManager

    @InjectMockKs
    lateinit var viewModel: ComposeMessageViewModel

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        // To avoid `EmptyList` to be returned by Mockk automatically as that causes
        // UnsupportedOperationException: Operation is not supported for read-only collection
        // when trying to add elements (in prod we ArrayList so this doesn't happen)
        every { userManager.user.senderEmailAddresses } returns mutableListOf()
    }

    @Test
    fun saveDraftCallsSaveDraftUseCaseWhenTheDraftIsNew() {
        runBlockingTest {
            // Given
            val message = Message()
            givenViewModelPropertiesAreInitialised()

            // When
            viewModel.saveDraft(message, hasConnectivity = false)

            // Then
            val parameters = SaveDraft.SaveDraftParameters(
                message,
                emptyList(),
                "parentId823",
                Constants.MessageActionType.FORWARD,
                "previousSenderAddressId"
            )
            coVerify { saveDraft(parameters) }
        }
    }

    @Test
    fun saveDraftReadsNewlyCreatedDraftFromRepositoryAndPostsItToLiveDataWhenSaveDraftUseCaseSucceeds() {
        runBlockingTest {
            // Given
            val message = Message()
            val createdDraftId = "newDraftId"
            val createdDraft = Message(messageId = createdDraftId, localId = "local28348")
            val savedDraftObserver = viewModel.savingDraftComplete.testObserver()
            givenViewModelPropertiesAreInitialised()
            coEvery { saveDraft(any()) } returns flowOf(SaveDraftResult.Success(createdDraftId))
            coEvery { messageDetailsRepository.findMessageById(createdDraftId) } returns createdDraft

            // When
            viewModel.saveDraft(message, hasConnectivity = false)

            coVerify { messageDetailsRepository.findMessageById(createdDraftId) }
            assertEquals(createdDraft, savedDraftObserver.observedValues[0])
        }
    }

    @Test
    fun saveDraftObservesMessageInComposeRepositoryToGetNotifiedWhenMessageIsSent() {
        runBlockingTest {
            // Given
            val createdDraftId = "newDraftId"
            val localDraftId = "localDraftId"
            val createdDraft = Message(messageId = createdDraftId, localId = localDraftId)
            givenViewModelPropertiesAreInitialised()
            coEvery { saveDraft(any()) } returns flowOf(SaveDraftResult.Success(createdDraftId))
            coEvery { messageDetailsRepository.findMessageById(createdDraftId) } returns createdDraft

            // When
            viewModel.saveDraft(Message(), hasConnectivity = false)

            // Then
            assertEquals(createdDraftId, viewModel.draftId)
            coVerify { composeMessageRepository.findMessageByIdObservable(createdDraftId) }
        }
    }

    @Test
    fun saveDraftCallsSaveDraftUseCaseWhenTheDraftIsExisting() {
        runBlockingTest {
            // Given
            val message = Message()
            givenViewModelPropertiesAreInitialised()
            viewModel.draftId = "non-empty-draftId"

            // When
            viewModel.saveDraft(message, hasConnectivity = false)

            // Then
            val parameters = SaveDraft.SaveDraftParameters(
                message,
                emptyList(),
                "parentId823",
                Constants.MessageActionType.FORWARD,
                "previousSenderAddressId"
            )
            coVerify { saveDraft(parameters) }
        }
    }

    @Test
    fun saveDraftResolvesLocalisedErrorMessageAndPostsOnLiveDataWhenSaveDraftUseCaseFailsCreatingTheDraft() {
        runBlockingTest {
            // Given
            val messageSubject = "subject"
            val message = Message(subject = messageSubject)
            val saveDraftErrorObserver = viewModel.savingDraftError.testObserver()
            val errorResId = R.string.failed_saving_draft_online
            givenViewModelPropertiesAreInitialised()
            coEvery { saveDraft(any()) } returns flowOf(SaveDraftResult.OnlineDraftCreationFailed)
            every { stringResourceResolver.invoke(errorResId) } returns "Error creating draft for message %s"

            // When
            viewModel.saveDraft(message, hasConnectivity = true)

            val expectedError = "Error creating draft for message $messageSubject"
            coVerify { stringResourceResolver.invoke(errorResId) }
            assertEquals(expectedError, saveDraftErrorObserver.observedValues[0])
        }
    }

    @Test
    fun saveDraftResolvesLocalisedErrorMessageAndPostsOnLiveDataWhenSaveDraftUseCaseFailsUploadingAttachments() {
        runBlockingTest {
            // Given
            val messageSubject = "subject"
            val message = Message(subject = messageSubject)
            val saveDraftErrorObserver = viewModel.savingDraftError.testObserver()
            val errorResId = R.string.attachment_failed
            givenViewModelPropertiesAreInitialised()
            coEvery { saveDraft(any()) } returns flowOf(SaveDraftResult.UploadDraftAttachmentsFailed)
            every { stringResourceResolver.invoke(errorResId) } returns "Error uploading attachments for subject "

            // When
            viewModel.saveDraft(message, hasConnectivity = true)

            val expectedError = "Error uploading attachments for subject $messageSubject"
            coVerify { stringResourceResolver.invoke(errorResId) }
            assertEquals(expectedError, saveDraftErrorObserver.observedValues[0])
        }
    }

    @Test
    fun saveDraftReadsNewlyCreatedDraftFromRepositoryAndPostsItToLiveDataWhenUpdatingDraftAndSaveDraftUseCaseSucceeds() {
        runBlockingTest {
            // Given
            val message = Message()
            val updatedDraftId = "updatedDraftId"
            val updatedDraft = Message(messageId = updatedDraftId, localId = "local82347")
            val savedDraftObserver = viewModel.savingDraftComplete.testObserver()
            givenViewModelPropertiesAreInitialised()
            viewModel.draftId = "non-empty draftId triggers update draft"
            coEvery { saveDraft(any()) } returns flowOf(SaveDraftResult.Success(updatedDraftId))
            coEvery { messageDetailsRepository.findMessageById(updatedDraftId) } returns updatedDraft

            // When
            viewModel.saveDraft(message, hasConnectivity = false)

            coVerify { messageDetailsRepository.findMessageById(updatedDraftId) }
            assertEquals(updatedDraft, savedDraftObserver.observedValues[0])
        }
    }

    @Test
    fun autoSaveDraftSchedulesJobToPerformSaveDraftAfterSomeDelay() {
        runBlockingTest(dispatchers.Io) {
            // Given
            val messageBody = "Message body being edited..."
            val messageId = "draft8237472"
            val message = Message(messageId, subject = "A subject")
            val buildMessageObserver = viewModel.buildingMessageCompleted.testObserver()
            givenViewModelPropertiesAreInitialised()
            // message was already saved once (we're updating)
            viewModel.draftId = messageId
            mockkStatic(UiUtil::class)
            every { UiUtil.toHtml(messageBody) } returns "<html> $messageBody <html>"
            every { UiUtil.fromHtml(any()) } returns mockk(relaxed = true)
            coEvery { composeMessageRepository.findMessage(messageId, dispatchers.Io) } returns message
            coEvery { composeMessageRepository.createAttachmentList(any(), dispatchers.Io) } returns emptyList()

            // When
            viewModel.autoSaveDraft(messageBody)
            viewModel.autoSaveJob?.join()

            // Then
            val expectedMessage = message.copy()
            assertEquals(expectedMessage, buildMessageObserver.observedValues[0]?.peekContent())
            assertEquals("&lt;html&gt; Message body being edited... &lt;html&gt;", viewModel.messageDataResult.content)
            unmockkStatic(UiUtil::class)
        }
    }

    @Test
    fun autoSaveDraftCancelsExistingJobBeforeSchedulingANewOneWhenCalledTwice() {
        runBlockingTest(dispatchers.Io) {
            // Given
            val messageBody = "Message body being edited again..."
            val messageId = "draft923823"
            val message = Message(messageId, subject = "Another subject")
            viewModel.buildingMessageCompleted.testObserver()
            givenViewModelPropertiesAreInitialised()
            // message was already saved once (we're updating)
            viewModel.draftId = messageId
            mockkStatic(UiUtil::class)
            every { UiUtil.toHtml(messageBody) } returns "<html> $messageBody <html>"
            every { UiUtil.fromHtml(any()) } returns mockk(relaxed = true)
            coEvery { composeMessageRepository.findMessage(messageId, dispatchers.Io) } returns message
            coEvery { composeMessageRepository.createAttachmentList(any(), dispatchers.Io) } returns emptyList()

            // When
            viewModel.autoSaveDraft(messageBody)
            assertNotNull(viewModel.autoSaveJob)
            val firstScheduledJob = viewModel.autoSaveJob
            viewModel.autoSaveDraft(messageBody)

            // Then
            assertTrue(firstScheduledJob?.isCancelled ?: false)
            assertTrue(viewModel.autoSaveJob?.isActive ?: false)
            unmockkStatic(UiUtil::class)
        }
    }

    @Test
    fun sendMessageCallsSendMessageUseCaseWithMessageParameters() {
        runBlockingTest {
            // Given
            val message = Message()
            givenViewModelPropertiesAreInitialised()
            viewModel.setMessagePassword("messagePassword", "a hint to discover it", true, 172800L, false)

            // When
            viewModel.sendMessage(message)

            // Then
            val params = SendMessage.SendMessageParameters(
                message,
                listOf(),
                "parentId823",
                Constants.MessageActionType.FORWARD,
                "previousSenderAddressId",
                MessageSecurityOptions("messagePassword", "a hint to discover it", 172800L)
            )
            coVerify { sendMessage(params) }
        }
    }

    private fun givenViewModelPropertiesAreInitialised() {
        // Needed to set class fields to the right value and allow code under test to get executed
        viewModel.prepareMessageData(false, "addressId", "mail-alias", false)
        viewModel.setupComposingNewMessage(false, Constants.MessageActionType.FORWARD, "parentId823", "")
        viewModel.oldSenderAddressId = "previousSenderAddressId"
    }

}
