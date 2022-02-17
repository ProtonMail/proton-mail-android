/*
 * Copyright (c) 2022 Proton Technologies AG
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

class ObserveConversationsByLocationTest : CoroutinesTest {

    private var userId = UserId("id")

    private val conversationRepository: ConversationsRepository = mockk()

    private val observeConversationsByLocation = ObserveConversationsByLocation(conversationRepository)
    
    @Test
    fun getConversationsCallsRepositoryMappingInputToGetConversationParameters() = runBlockingTest {
        // given
        val location = MessageLocationType.ARCHIVE.asLabelIdString()
        coEvery { conversationRepository.observeConversations(any()) } returns loadMoreFlowOf()

        // when
        observeConversationsByLocation(userId, location)

        // then
        val params = GetAllConversationsParameters(
            userId = userId,
            labelId = location
        )
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
        observeConversationsByLocation(userId, MessageLocationType.INBOX.asLabelIdString()).test {

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
        val actual = observeConversationsByLocation.invoke(userId, MessageLocationType.INBOX.asLabelIdString())

        // then
        val error = GetConversationsResult.Error()
        assertEquals(error, actual.first())
    }

    @Test
    fun getConversationsCallsRepositoryPassingNullAsLastMessageTimeWhenInputWasNull() = runBlockingTest {
        // given
        val location = MessageLocationType.ARCHIVE.asLabelIdString()
        coEvery { conversationRepository.observeConversations(any(),) } returns loadMoreFlowOf()

        // when
        observeConversationsByLocation.invoke(userId, location)

        // then
        val params = GetAllConversationsParameters(
            userId = userId,
            labelId = location
        )
        coVerify { conversationRepository.observeConversations(params,) }
    }

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
