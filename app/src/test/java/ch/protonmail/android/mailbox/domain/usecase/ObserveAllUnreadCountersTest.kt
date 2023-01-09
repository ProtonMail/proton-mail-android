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
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.AllUnreadCounters
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.repository.MessageRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveAllUnreadCountersTest : CoroutinesTest by CoroutinesTest() {

    private val testUserId = UserId("one")

    private val messagesRepository: MessageRepository = mockk {
        every { getUnreadCounters(testUserId) } returns flowOf(DataResult.Success(ResponseSource.Local, emptyList()))
    }
    private val conversationsRepository: ConversationsRepository = mockk {
        every { getUnreadCounters(testUserId) } returns flowOf(DataResult.Success(ResponseSource.Local, emptyList()))
    }
    private val observeAllUnreadCounters = ObserveAllUnreadCounters(
        messagesRepository = messagesRepository,
        conversationsRepository = conversationsRepository
    )

    @Test
    fun emitsDataFromMessagesRepository() = runBlockingTest {
        // given
        val messagesCounters = listOf(
            UnreadCounter("inbox", 15),
            UnreadCounter("sent", 3)
        )
        every { messagesRepository.getUnreadCounters(testUserId) } returns
            flowOf(DataResult.Success(ResponseSource.Local, messagesCounters))

        val allCounters = AllUnreadCounters(
            messagesCounters = messagesCounters,
            conversationsCounters = emptyList()
        )
        val expected = DataResult.Success(ResponseSource.Local, allCounters)

        // when
        observeAllUnreadCounters(testUserId).test {

            // then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun emitsErrorsFromMessagesRepository() = runBlockingTest {
        // given
        val expectedMessage = "Some wrong happened"
        val expectedException = IllegalStateException(expectedMessage)
        val expectedError = DataResult.Error.Remote(expectedMessage, expectedException)
        every { messagesRepository.getUnreadCounters(testUserId) } returns flowOf(expectedError)

        // when
        observeAllUnreadCounters(testUserId).test {

            // then
            // An Item is emitted from conversationRepository which is mocked to return a success
            awaitItem()
            assertEquals(expectedError, awaitItem())
        }
    }

    @Test
    fun emitsDataFromConversationsRepository() = runBlockingTest {
        // given
        val conversationsCounters = listOf(
            UnreadCounter("inbox", 15),
            UnreadCounter("sent", 3)
        )
        every { conversationsRepository.getUnreadCounters(testUserId) } returns
            flowOf(DataResult.Success(ResponseSource.Local, conversationsCounters))

        val allCounters = AllUnreadCounters(
            messagesCounters = emptyList(),
            conversationsCounters = conversationsCounters
        )
        val expected = DataResult.Success(ResponseSource.Local, allCounters)

        // when
        observeAllUnreadCounters(testUserId).test {

            // then
            assertEquals(expected, awaitItem())
        }
    }

    @Test
    fun emitsErrorsFromConversationsRepository() = runBlockingTest {
        // given
        val expectedMessage = "Some wrong happened"
        val expectedException = IllegalStateException(expectedMessage)
        val expectedError = DataResult.Error.Remote(expectedMessage, expectedException)
        every { conversationsRepository.getUnreadCounters(testUserId) } returns flowOf(expectedError)

        // when
        observeAllUnreadCounters(testUserId).test {

            // then
            // An Item is emitted from messageRepository which is mocked to return a success
            awaitItem()
            assertEquals(expectedError, awaitItem())
        }
    }

    @Test
    fun emitsDataFromMessagesAndConversationsRepositories() = runBlockingTest {
        // given
        val messagesCounters = listOf(
            UnreadCounter("inbox", 15),
            UnreadCounter("sent", 3)
        )
        val conversationsCounters = listOf(
            UnreadCounter("inbox", 3),
            UnreadCounter("sent", 2)
        )
        every { messagesRepository.getUnreadCounters(testUserId) } returns
            flowOf(DataResult.Success(ResponseSource.Local, messagesCounters))
        every { conversationsRepository.getUnreadCounters(testUserId) } returns
            flowOf(DataResult.Success(ResponseSource.Local, conversationsCounters))

        val allCounters = AllUnreadCounters(
            messagesCounters = messagesCounters,
            conversationsCounters = conversationsCounters
        )
        val expected = DataResult.Success(ResponseSource.Local, allCounters)

        // when
        observeAllUnreadCounters(testUserId).test {

            // then
            assertEquals(expected, awaitItem())
        }
    }
}
