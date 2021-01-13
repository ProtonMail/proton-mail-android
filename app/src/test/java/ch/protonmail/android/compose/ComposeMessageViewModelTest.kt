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
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.services.PostMessageServiceFactory
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.testAndroid.rx.TrampolineScheduler
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.compose.SaveDraftResult
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.fetch.FetchPublicKeys
import io.mockk.MockKAnnotations
import ch.protonmail.android.utils.resources.StringResourceResolver
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import kotlin.test.BeforeTest
import kotlin.test.Test

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

            coEvery { messageDetailsRepository.findMessageById(createdDraftId) }
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
            coEvery { stringResourceResolver.invoke(errorResId) }
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
            coEvery { stringResourceResolver.invoke(errorResId) }
            assertEquals(expectedError, saveDraftErrorObserver.observedValues[0])
        }
    }

    private fun givenViewModelPropertiesAreInitialised() {
        // Needed to set class fields to the right value and allow code under test to get executed
        viewModel.prepareMessageData(false, "addressId", "mail-alias", false)
        viewModel.setupComposingNewMessage(false, Constants.MessageActionType.FORWARD, "parentId823", "")
        viewModel.oldSenderAddressId = "previousSenderAddressId"
    }

}
