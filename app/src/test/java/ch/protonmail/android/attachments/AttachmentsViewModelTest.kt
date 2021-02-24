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

    private lateinit var viewModel: AttachmentsViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        viewModel = AttachmentsViewModel(
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

        viewModel.init(messageId)

        coVerify { messageRepository.findMessageById(messageId) }
    }

    @Test
    fun initObservesMessageRepositoryByMessageDbIdWhenGivenMessageIdIsFound() = runBlockingTest {
        val messageId = "draftId234"
        val messageDbId = 124L
        val message = Message().apply { dbId = messageDbId }
        coEvery { messageRepository.findMessageById(messageId) } returns message

        viewModel.init(messageId)

        coVerify { messageRepository.findMessageByDbId(messageDbId) }
    }

    @Test
    fun initUpdatesViewStateWhenMessageIsUpdatedInDbAsAResultOfDraftCreationCompleting() = runBlockingTest {
        val messageId = "91bbb263-2bf2-43dd-a079-233a305e69df"
        val messageDbId = 124L
        val message = Message().apply { dbId = messageDbId }
        val updatedMessageAttachments = listOf(Attachment(attachmentId = "updatedAttId"))
        val remoteMessage = message.copy(messageId = "Remote message id").apply {
            Attachments = updatedMessageAttachments
        }
        coEvery { messageRepository.findMessageById(messageId) } returns message
        coEvery { messageRepository.findMessageByDbId(messageDbId) } returns flowOf(remoteMessage)

        viewModel.init(messageId)

        val expectedState = UpdateAttachments(updatedMessageAttachments)
        coVerify { mockObserver.onChanged(expectedState) }
    }

    @Test
    fun initDoesNotUpdateViewStateWhenMessageThatWasUpdatedInDbIsNotARemoteMessage() = runBlockingTest {
        val messageId = "91bbb263-2bf2-43dd-a079-233a305e69df"
        val messageDbId = 124L
        val message = Message().apply { dbId = messageDbId }
        val updatedLocalMessage = message.copy(messageId = "82ccc723-2bf2-43dd-f834-233a305e69df")
        coEvery { messageRepository.findMessageById(messageId) } returns message
        coEvery { messageRepository.findMessageByDbId(messageDbId) } returns flowOf(updatedLocalMessage)

        viewModel.init(messageId)

        coVerify(exactly = 0) { mockObserver.onChanged(any()) }
    }

    @Test
    fun initPostsOfflineViewStateWhenThereIsNoConnection() = runBlockingTest {
        val messageId = "91bbb263-2bf2-43dd-a079-233a305e69df"
        val messageDbId = 124L
        val message = Message().apply { dbId = messageDbId }
        coEvery { messageRepository.findMessageById(messageId) } returns message
        coEvery { messageRepository.findMessageByDbId(messageDbId) } returns flowOf()
        every { networkUtils.isConnected() } returns false

        viewModel.init(messageId)

        coVerifySequence { mockObserver.onChanged(MissingConnectivity) }
    }
}
