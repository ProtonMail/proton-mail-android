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

package ch.protonmail.android.data.local

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.UnreadLabelCounter
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

class CounterRepositoryImplTest {

    private val counterDao = mockk<CounterDao>()

    private val messageRepository = mockk<MessageRepository>()

    private val repository = CounterRepositoryImpl(
        counterDao,
        messageRepository
    )
    private val userId = UserId("testUserId")
    private val labelId = LabelId("testLabelId")
    private val messageId1 = "messageId1"
    private val message1 = Message(
        messageId = messageId1,
        Unread = false
    )
    private val messageIds = listOf(messageId1)
    private val counterId1 = "UnreadCounter1"
    private val testUnreadCounter = UnreadLabelCounter(counterId1, 1)

    @BeforeTest
    fun setup() {
        coEvery { messageRepository.findMessage(userId, messageId1) } returns message1
        every { counterDao.findUnreadLabelById(labelId.id) } returns testUnreadCounter
        every { counterDao.insertUnreadLabel(any()) } returns Unit
    }

    @Test
    fun verifyThatUpdateMessageLabelCounterIsExecutedCorrectlyForIncrements() = runBlocking {
        // given
        val updateMethod = CounterRepository.CounterModificationMethod.INCREMENT

        // when
        repository.updateMessageLabelCounter(userId, labelId.id, messageIds, updateMethod)

        // then
        verify {
            testUnreadCounter.increment()
            counterDao.insertUnreadLabel(testUnreadCounter)
        }
    }

    @Test
    fun verifyThatUpdateMessageLabelCounterIsExecutedCorrectlyForDecrements() = runBlocking {
        // given
        val updateMethod = CounterRepository.CounterModificationMethod.DECREMENT

        // when
        repository.updateMessageLabelCounter(userId, labelId.id, messageIds, updateMethod)

        // then
        verify {
            testUnreadCounter.decrement()
            counterDao.insertUnreadLabel(testUnreadCounter)
        }
    }
}
