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

package ch.protonmail.android.details.domain.usecase

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.testdata.MessageTestData
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test

class MarkMessageAsReadIfNeededTest {

    private val messageRepositoryMock = mockk<MessageRepository> {
        every { markRead(any()) } just runs
    }
    private val markMessageAsReadIfNeeded = MarkMessageAsReadIfNeeded(messageRepositoryMock)

    @Test
    fun `should not mark as read if message not visible to the user`() {
        // given
        val unread = true
        val visibleToTheUser = false
        val messageId = MessageTestData.MESSAGE_ID_RAW
        val message = Message(messageId = messageId, Unread = unread)

        // when
        markMessageAsReadIfNeeded.invoke(message, messageVisibleToTheUser = visibleToTheUser)

        // then
        verify(exactly = 0) { messageRepositoryMock.markRead(any()) }
    }

    @Test
    fun `should not mark as read if message is already read`() {
        // given
        val unread = false
        val visibleToTheUser = true
        val messageId = MessageTestData.MESSAGE_ID_RAW
        val message = Message(messageId = messageId, Unread = unread)

        // when
        markMessageAsReadIfNeeded.invoke(message, messageVisibleToTheUser = visibleToTheUser)

        // then
        verify(exactly = 0) { messageRepositoryMock.markRead(any()) }
    }

    @Test
    fun `should not mark as read if message id is null`() {
        // given
        val unread = true
        val visibleToTheUser = true
        val messageId = null
        val message = Message(messageId = messageId, Unread = unread)

        // when
        markMessageAsReadIfNeeded.invoke(message, messageVisibleToTheUser = visibleToTheUser)

        // then
        verify(exactly = 0) { messageRepositoryMock.markRead(any()) }
    }

    @Test
    fun `should mark as read if message not read, visible to the user, and message id is not null`() {
        // given
        val unread = true
        val visibleToTheUser = true
        val messageId = MessageTestData.MESSAGE_ID_RAW
        val message = Message(messageId = messageId, Unread = unread)

        // when
        markMessageAsReadIfNeeded.invoke(message, messageVisibleToTheUser = visibleToTheUser)

        // then
        verify { messageRepositoryMock.markRead(listOf(messageId)) }
    }
}
