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

package ch.protonmail.android.mailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import me.proton.core.domain.entity.UserId
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMessagesByLocationTest {

    private val mailboxRepository: MessageRepository = mockk()

    private val useCase = ObserveMessagesByLocation(mailboxRepository)

    private val userId = UserId("user")

    @Test
    fun verifyThatInboxDataModelIsReturnedNormally() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.INBOX
        val labelId = ""
        val message1 = mockk<Message>(relaxed = true)
        val message2 = mockk<Message>(relaxed = true)
        val messages = listOf(message1, message2)
        val flowOfMessages = flowOf(messages)
        coEvery {
            mailboxRepository.observeMessagesByLocation(
                userId,
                mailboxLocation
            )
        } returns flowOfMessages
        val expected = GetMessagesResult.Success(messages)

        // when
        val resultsList = useCase.invoke(mailboxLocation, labelId, userId).take(1).toList()

        // then
        assertEquals(expected, resultsList[0])
    }

    @Test
    fun verifyThatLabelDataModelIsReturnedNormally() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.LABEL
        val labelId = "label1"
        val message1 = mockk<Message>(relaxed = true)
        val message2 = mockk<Message>(relaxed = true)
        val messages = listOf(message1, message2)
        val expected = GetMessagesResult.Success(messages)
        val flowOfMessages = flowOf(messages)
        coEvery {
            mailboxRepository.observeMessagesByLabelId(labelId, userId)
        } returns flowOfMessages

        // when
        val resultsList = useCase.invoke(mailboxLocation, labelId, userId).take(1).toList()

        // then
        assertEquals(expected, resultsList[0])
    }

    @Test
    fun verifyThatStarsDataModelIsReturnedNormally() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.STARRED
        val labelId = "label1"
        val message1 = mockk<Message>(relaxed = true)
        val message2 = mockk<Message>(relaxed = true)
        val messages = listOf(message1, message2)
        val expected = GetMessagesResult.Success(messages)
        val flowOfMessages = flowOf(messages)
        coEvery {
            mailboxRepository.observeMessagesByLocation(userId, mailboxLocation)
        } returns flowOfMessages

        // when
        val resultsList = useCase.invoke(mailboxLocation, labelId, userId).take(1).toList()

        // then
        assertEquals(expected, resultsList[0])
    }

    @Test
    fun verifyThatAllMailDataModelIsReturnedNormally() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.ALL_MAIL
        val labelId = "label1"
        val message1 = mockk<Message>(relaxed = true)
        val message2 = mockk<Message>(relaxed = true)
        val messages = listOf(message1, message2)
        val expected = GetMessagesResult.Success(messages)
        val flowOfMessages = flowOf(messages)
        coEvery {
            mailboxRepository.observeMessagesByLocation(userId, mailboxLocation)
        } returns flowOfMessages

        // when
        val resultsList = useCase.invoke(mailboxLocation, labelId, userId).take(1).toList()

        // then
        assertEquals(expected, resultsList[0])
    }

    @Test
    fun verifyThatInboxDataExceptionCausesAnErrorResponseBeingReturned() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.INBOX
        val labelId = ""
        val message1 = mockk<Message>(relaxed = true)
        val message2 = mockk<Message>(relaxed = true)
        val testException = Exception("Olala exception!")
        val messagesResponseChannel = Channel<List<Message>>()
        coEvery {
            mailboxRepository.observeMessagesByLocation(
                userId,
                mailboxLocation
            )
        } returns messagesResponseChannel.receiveAsFlow()
        val expected = GetMessagesResult.Error(testException)

        // when
        useCase.invoke(mailboxLocation, labelId, userId).test {
            // then
            messagesResponseChannel.close(testException)
            assertEquals(expected, expectItem())
            expectComplete()
        }
    }

}
