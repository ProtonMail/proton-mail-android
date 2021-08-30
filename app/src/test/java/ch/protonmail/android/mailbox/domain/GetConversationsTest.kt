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

package ch.protonmail.android.mailbox.domain

import ch.protonmail.android.core.Constants.MessageLocationType
import ch.protonmail.android.domain.loadMoreFlowOf
import ch.protonmail.android.mailbox.domain.model.Conversation
import ch.protonmail.android.mailbox.domain.model.GetConversationsParameters
import ch.protonmail.android.mailbox.domain.model.GetConversationsResult
import ch.protonmail.android.mailbox.domain.model.LabelContext
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private const val NO_MORE_CONVERSATIONS_ERROR_CODE = 723478

class GetConversationsTest : CoroutinesTest {

    private var userId = UserId("id")

    @RelaxedMockK
    private lateinit var conversationRepository: ConversationsRepository

    private lateinit var getConversations: GetConversations

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        getConversations = GetConversations(
            conversationRepository
        )
    }

    @Test
    fun getConversationsCallsRepositoryMappingInputToGetConversationParameters() = runBlockingTest {
        val location = MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
        coEvery { conversationRepository.getConversations(any()) } returns loadMoreFlowOf()

        getConversations.invoke(userId, location)

        val params = GetConversationsParameters(
            locationId = location,
            userId = userId,
            oldestConversationTimestamp = null
        )
        coVerify { conversationRepository.getConversations(params) }
    }

    @Test
    fun getConversationsReturnsConversationsFlowWhenRepositoryRequestSucceeds() = runBlockingTest {
        val conversations = listOf(buildRandomConversation())
        val dataResult = DataResult.Success(ResponseSource.Remote, conversations)
        coEvery { conversationRepository.getConversations(any()) } returns loadMoreFlowOf(dataResult)

        val actual = getConversations.invoke(userId, MessageLocationType.INBOX.messageLocationTypeValue.toString())

        val expected = GetConversationsResult.Success(conversations)
        assertEquals(expected, actual.first())
    }

    @Test
    fun getConversationsReturnsErrorWhenRepositoryFailsGettingConversations() = runBlockingTest {
        coEvery { conversationRepository.getConversations(any()) } returns loadMoreFlowOf(DataResult.Error.Local(null, null))

        val actual = getConversations.invoke(userId, MessageLocationType.INBOX.messageLocationTypeValue.toString())

        val error = GetConversationsResult.Error()
        assertEquals(error, actual.first())
    }

    @Test
    fun getConversationsCallsRepositoryPassingNullAsLastMessageTimeWhenInputWasNull() = runBlockingTest {
        val location = MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
        coEvery { conversationRepository.getConversations(any()) } returns loadMoreFlowOf()

        getConversations.invoke(userId, location)

        val params = GetConversationsParameters(
            locationId = location,
            userId = userId,
            oldestConversationTimestamp = null
        )
        coVerify { conversationRepository.getConversations(params) }
    }

    @Test
    fun getConversationsWithInputLocationArchiveReturnsOnlyConversationsThatAreArchived() = runBlockingTest {
        val archivedConversation = buildRandomConversation().copy(
            id = "archivedConversationID123423",
            labels = listOf(inboxLabelContext(), archiveLabelContext())
        )
        val inboxConversation = buildRandomConversation().copy(
            labels = listOf(inboxLabelContext())
        )
        val conversations = listOf(inboxConversation, archivedConversation)
        val dataResult = DataResult.Success(ResponseSource.Local, conversations)
        coEvery { conversationRepository.getConversations(any()) } returns loadMoreFlowOf(dataResult)

        val result: GetConversationsResult = getConversations.invoke(
            userId, MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
        ).first()

        val actualConversations = (result as GetConversationsResult.Success).conversations
        assertEquals(listOf(archivedConversation), actualConversations)
    }

    @Test
    fun getConversationsWithInputLocationLabelReturnsOnlyConversationsWithTheCustomLabelApplied() = runBlockingTest {
        val customLabelId = "82384828348"
        val customLabelConversation = buildRandomConversation().copy(
            id = "conversationWithCustomLabel283842",
            labels = listOf(inboxLabelContext(), customLabelContext(customLabelId))
        )
        val inboxConversation = buildRandomConversation().copy(
            labels = listOf(inboxLabelContext(), archiveLabelContext())
        )
        val conversations = listOf(inboxConversation, customLabelConversation)
        val dataResult = DataResult.Success(ResponseSource.Local, conversations)
        coEvery { conversationRepository.getConversations(any()) } returns loadMoreFlowOf(dataResult)

        val result: GetConversationsResult = getConversations.invoke(
            userId,
            customLabelId
        ).first()

        val actualConversations = (result as GetConversationsResult.Success).conversations
        assertEquals(listOf(customLabelConversation), actualConversations)
    }

    @Test
    fun getConversationsReturnsNoConversationsFoundWhenRepositoryReturnsNoConversations() = runBlockingTest {
        coEvery { conversationRepository.getConversations(any()) } returns loadMoreFlowOf(
            DataResult.Error.Remote("any", null, NO_MORE_CONVERSATIONS_ERROR_CODE)
        )

        val actual = getConversations.invoke(userId, MessageLocationType.INBOX.messageLocationTypeValue.toString())

        val error = GetConversationsResult.NoConversationsFound
        assertEquals(error, actual.first())
    }

    private fun customLabelContext(labelId: String) = LabelContext(labelId, 0, 0, 0L, 0, 0)

    private fun inboxLabelContext() =
        LabelContext(MessageLocationType.INBOX.messageLocationTypeValue.toString(), 0, 0, 0L, 0, 0)

    private fun archiveLabelContext() =
        LabelContext(MessageLocationType.ARCHIVE.messageLocationTypeValue.toString(), 0, 0, 0L, 0, 0)

    private fun buildRandomConversation(): Conversation {
        return Conversation(
            UUID.randomUUID().toString(),
            "Conversation subject",
            listOf(),
            listOf(),
            5,
            2,
            1,
            0,
            listOf(inboxLabelContext()),
            listOf()
        )
    }
}
