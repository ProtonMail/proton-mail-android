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
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import ch.protonmail.android.activities.AddAttachmentsActivity
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Attachment
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.attachments.AttachmentsViewModel.ViewState
import ch.protonmail.android.attachments.AttachmentsViewModel.ViewState.MissingConnectivity
import ch.protonmail.android.attachments.AttachmentsViewModel.ViewState.UpdateAttachments
import ch.protonmail.android.core.QueueNetworkUtil
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifySequence
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test


class AttachmentsViewModelTest : CoroutinesTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    lateinit var messageRepository: MessageDetailsRepository

    @RelaxedMockK
    lateinit var networkUtils: QueueNetworkUtil

    @RelaxedMockK
    private lateinit var mockObserver: Observer<ViewState>

    @RelaxedMockK
    private lateinit var savedState: SavedStateHandle

    private lateinit var viewModel: AttachmentsViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = AttachmentsViewModel(
            savedState,
            dispatchers,
            messageRepository,
            networkUtils
        )
        viewModel.viewState.observeForever(mockObserver)
        every { networkUtils.isConnected() } returns true
    }

    @Test
    fun initFindsMessageInDatabase() = runBlockingTest {
        val messageId = "draftId3214"
        coEvery { messageRepository.findMessageById(messageId) } returns mockk(relaxed = true)
        every { savedState.get<String>(AddAttachmentsActivity.EXTRA_DRAFT_ID) } returns messageId

        viewModel.init()

        coVerify { messageRepository.findMessageById(messageId) }
    }

    @Test
    fun initObservesMessageRepositoryByMessageDbIdWhenGivenMessageIdIsFound() = runBlockingTest {
        val messageId = "draftId234"
        val messageDbId = 124L
        val message = Message(messageId = messageId).apply { dbId = messageDbId }
        coEvery { messageRepository.findMessageById(messageId) } returns message
        every { savedState.get<String>(AddAttachmentsActivity.EXTRA_DRAFT_ID) } returns messageId

        viewModel.init()

        coVerify { messageRepository.findMessageByDbId(messageDbId) }
    }

    @Test
    fun initUpdatesViewStateWhenMessageIsUpdatedInDbAsAResultOfDraftCreationCompleting() = runBlockingTest {
        val messageId = "91bbb263-2bf2-43dd-a079-233a305e69df"
        val messageDbId = 124L
        val message = Message(messageId = messageId).apply { dbId = messageDbId }
        val updatedMessageAttachments = listOf(Attachment(attachmentId = "updatedAttId"))
        val remoteMessage = message.copy(messageId = "Remote message id").apply {
            Attachments = updatedMessageAttachments
        }
        coEvery { messageRepository.findMessageById(messageId) } returns message
        coEvery { messageRepository.findMessageByDbId(messageDbId) } returns flowOf(remoteMessage)
        every { savedState.get<String>(AddAttachmentsActivity.EXTRA_DRAFT_ID) } returns messageId

        viewModel.init()

        val expectedState = UpdateAttachments(updatedMessageAttachments)
        coVerify { mockObserver.onChanged(expectedState) }
    }

    @Test
    fun initStopsListeningForMessageUpdatesWhenDraftCreationCompletedEventWasReceived() = runBlockingTest {
        val messageId = "91bbb263-2bf2-43dd-a079-233a305e69df"
        val messageDbId = 124L
        val message = Message(messageId = messageId).apply { dbId = messageDbId }
        val updatedMessageAttachments = listOf(Attachment(attachmentId = "updatedAttId"))
        val remoteMessage = message.copy(messageId = "Remote message id").apply {
            Attachments = updatedMessageAttachments
        }
        val updatedDraftMessage = remoteMessage.copy(messageId = "Updated remote messageID")
        coEvery { messageRepository.findMessageById(messageId) } returns message
        coEvery { messageRepository.findMessageByDbId(messageDbId) } returns flowOf(
            remoteMessage, updatedDraftMessage
        )
        every { savedState.get<String>(AddAttachmentsActivity.EXTRA_DRAFT_ID) } returns messageId

        viewModel.init()

        val expectedState = UpdateAttachments(updatedMessageAttachments)
        coVerifySequence { mockObserver.onChanged(expectedState) }
    }

    @Test
    fun initDoesNotUpdateViewStateWhenMessageIsUpdatedInDbAsAResultOfDraftUpdateCompleting() = runBlockingTest {
        val messageId = "remote-draft-message ID"
        val messageDbId = 2384L
        val message = Message().apply { dbId = messageDbId }
        val updatedMessageAttachments = listOf(Attachment(attachmentId = "updatedAttId"))
        val remoteMessage = message.copy(messageId = "Updated Draft Remote message id").apply {
            Attachments = updatedMessageAttachments
        }
        coEvery { messageRepository.findMessageById(messageId) } returns message
        coEvery { messageRepository.findMessageByDbId(messageDbId) } returns flowOf(remoteMessage)
        every { savedState.get<String>(AddAttachmentsActivity.EXTRA_DRAFT_ID) } returns messageId

        viewModel.init()

        coVerify(exactly = 0) { mockObserver.onChanged(any()) }
    }

    @Test
    fun initDoesNotUpdateViewStateWhenMessageThatWasUpdatedInDbIsNotARemoteMessage() = runBlockingTest {
        val messageId = "91bbb263-2bf2-43dd-a079-233a305e69df"
        val messageDbId = 124L
        val message = Message(messageId = messageId).apply { dbId = messageDbId }
        val updatedLocalMessage = message.copy(messageId = "82ccc723-2bf2-43dd-f834-233a305e69df")
        coEvery { messageRepository.findMessageById(messageId) } returns message
        coEvery { messageRepository.findMessageByDbId(messageDbId) } returns flowOf(updatedLocalMessage)
        every { savedState.get<String>(AddAttachmentsActivity.EXTRA_DRAFT_ID) } returns messageId

        viewModel.init()

        coVerify(exactly = 0) { mockObserver.onChanged(any()) }
    }

    @Test
    fun initPostsOfflineViewStateWhenThereIsNoConnection() = runBlockingTest {
        val messageId = "91bbb263-2bf2-43dd-a079-233a305e69df"
        val messageDbId = 124L
        val message = Message(messageId = messageId).apply { dbId = messageDbId }
        coEvery { messageRepository.findMessageById(messageId) } returns message
        coEvery { messageRepository.findMessageByDbId(messageDbId) } returns flowOf()
        every { networkUtils.isConnected() } returns false
        every { savedState.get<String>(AddAttachmentsActivity.EXTRA_DRAFT_ID) } returns messageId

        viewModel.init()

        coVerifySequence { mockObserver.onChanged(MissingConnectivity) }
    }

    @Test
    fun initLogsWarningAndStopsExecutionIfDraftIdWasNotPassed() = runBlockingTest {
        every { savedState.get<String>(AddAttachmentsActivity.EXTRA_DRAFT_ID) } returns null

        viewModel.init()

        // test warning is logged here
        coVerify(exactly = 0) { messageRepository.findMessageByDbId(any()) }
        coVerify(exactly = 0) { mockObserver.onChanged(any()) }
    }
}
