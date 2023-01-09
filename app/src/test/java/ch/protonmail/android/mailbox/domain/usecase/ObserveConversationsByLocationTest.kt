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

package ch.protonmail.android.mailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.domain.loadMoreFlowOf
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.GetAllConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.LabelContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveConversationsByLocationTest : CoroutinesTest by CoroutinesTest() {

    private var userId = UserId("id")

    private val conversationRepository: ConversationsRepository = mockk()

    private val observeConversationsByLocation = ObserveConversationsByLocation(conversationRepository)
    
    @Test
    fun getConversationsCallsRepositoryMappingInputToGetConversationParameters() = runBlockingTest {
        // given
        val params = params(
            userId = userId,
            locationType = MessageLocationType.ARCHIVE
        )
        coEvery { conversationRepository.observeConversations(any()) } returns loadMoreFlowOf()

        // when
        observeConversationsByLocation(params)

        // then
        coVerify { conversationRepository.observeConversations(params,) }
    }

    @Test
    fun emitsApiRefreshWhenRepositoryEmitsRemoteData() = runBlockingTest {
        // given
        val conversations = listOf(buildConversation())
        val dataResult = DataResult.Success(ResponseSource.Remote, conversations)
        coEvery { conversationRepository.observeConversations(any()) } returns loadMoreFlowOf(dataResult)

        val expected = GetConversationsResult.DataRefresh(conversations)

        // when
        observeConversationsByLocation(params(userId, MessageLocationType.INBOX)).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun getConversationsReturnsErrorWhenRepositoryFailsGettingConversations() = runBlockingTest {
        // given
        coEvery { conversationRepository.observeConversations(any()) } returns
            loadMoreFlowOf(DataResult.Error.Local(null, null))

        // when
        val actual = observeConversationsByLocation(params(userId, MessageLocationType.INBOX))

        // then
        val error = GetConversationsResult.Error()
        assertEquals(error, actual.first())
    }

    @Test
    fun getConversationsCallsRepositoryPassingNullAsLastMessageTimeWhenInputWasNull() = runBlockingTest {
        // given
        val labelId = MessageLocationType.ARCHIVE.asLabelId()
        val params = GetAllConversationsParameters(
            userId = userId,
            labelId = labelId
        )
        coEvery { conversationRepository.observeConversations(any(),) } returns loadMoreFlowOf()

        // when
        observeConversationsByLocation(params)

        // then
        coVerify { conversationRepository.observeConversations(params,) }
    }

    companion object TestData {

        fun params(userId: UserId, locationType: MessageLocationType) = GetAllConversationsParameters(
            userId = userId,
            labelId = locationType.asLabelId()
        )

        private fun inboxLabelContext() = LabelContext(
            id = MessageLocationType.INBOX.asLabelIdString(),
            contextNumUnread = 0,
            contextNumMessages = 0,
            contextTime = 0L,
            contextSize = 0,
            contextNumAttachments = 0
        )

        private fun buildConversation() = Conversation(
            id = UUID.randomUUID().toString(),
            subject = "Conversation subject",
            senders = listOf(),
            receivers = listOf(),
            messagesCount = 5,
            unreadCount = 2,
            attachmentsCount = 1,
            expirationTime = 0,
            labels = listOf(inboxLabelContext()),
            messages = listOf()
        )
    }
}
