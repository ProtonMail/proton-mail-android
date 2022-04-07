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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.LoadMoreFlow
import ch.protonmail.android.domain.asLoadMoreFlow
import ch.protonmail.android.domain.loadMoreFlowOf
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.mailbox.domain.model.GetAllMessagesParameters
import ch.protonmail.android.mailbox.domain.model.GetMessagesResult
import ch.protonmail.android.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class ObserveMessagesByLocationTest {

    private val userId = UserId("user")
    private val message1 = mockk<Message>(relaxed = true) { every { messageId } returns "1" }
    private val message2 = mockk<Message>(relaxed = true) { every { messageId } returns "2" }
    private val allMessages = listOf(message1, message2)
    private val allMessagesLoadMoreFlow: LoadMoreFlow<DataResult<List<Message>>> =
        loadMoreFlowOf(DataResult.Success(ResponseSource.Local, allMessages))

    private val mailboxRepository: MessageRepository = mockk {
        every { observeMessages(any()) } returns allMessagesLoadMoreFlow
    }
    private val useCase = ObserveMessagesByLocation(mailboxRepository)

    @Test
    fun verifyThatInboxDataModelIsReturnedNormally() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.INBOX
        val expected = GetMessagesResult.Success(allMessages)

        // when
        useCase(params(userId, mailboxLocation.asLabelId())).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun verifyThatLabelDataModelIsReturnedNormally() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.LABEL
        val expected = GetMessagesResult.Success(allMessages)

        // when
        useCase(params(userId, mailboxLocation.asLabelId())).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun verifyThatStarsDataModelIsReturnedNormally() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.STARRED
        val expected = GetMessagesResult.Success(allMessages)

        // when
        useCase(params(userId, mailboxLocation.asLabelId())).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun verifyThatAllMailDataModelIsReturnedNormally() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.ALL_MAIL
        val expected = GetMessagesResult.Success(allMessages)

        // when
        useCase(params(userId, mailboxLocation.asLabelId())).test {

            // then
            assertEquals(expected, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun verifyThatInboxDataExceptionCausesAnErrorResponseBeingReturned() = runBlockingTest {
        // given
        val mailboxLocation = Constants.MessageLocationType.INBOX
        val messagesResponseChannel = Channel<DataResult<List<Message>>>()
        val params = GetAllMessagesParameters(
            userId,
            labelId = mailboxLocation.asLabelId()
        )
        coEvery { mailboxRepository.observeMessages(params) } returns
            messagesResponseChannel.receiveAsFlow().asLoadMoreFlow()

        val testExceptionMessage = "An exception!"
        val testException = IllegalStateException(testExceptionMessage)
        val expectedExceptionType = IllegalStateException::class

        // when
        useCase(params).test {

            // then
            messagesResponseChannel.close(testException)
            val actualError = awaitItem() as GetMessagesResult.Error
            val actualException = checkNotNull(actualError.throwable)
            assertEquals(testExceptionMessage, actualException.message)
            assertEquals(expectedExceptionType, actualException::class)
            awaitComplete()
        }
    }

    companion object TestData {

        fun params(userId: UserId, labelId: LabelId) = GetAllMessagesParameters(
            userId = userId,
            labelId = labelId
        )
    }

}
