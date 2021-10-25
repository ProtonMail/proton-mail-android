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

package ch.protonmail.android.usecase.message

import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

/**
 * Tests the behaviour of [ChangeMessagesStarredStatus]
 */
class ChangeMessagesStarredStatusTest {

    private val messageRepository: MessageRepository = mockk()

    private val conversationsRepository: ConversationsRepository = mockk()

    private val changeMessagesStarredStatus = ChangeMessagesStarredStatus(messageRepository, conversationsRepository)

    @Test
    fun verifyCorrectMethodsAreCalledWhenActionIsStar() = runBlockingTest {
        // given
        val messageIds = listOf("messageId1", "messageId2")
        val action = ChangeMessagesStarredStatus.Action.ACTION_STAR
        val userId = UserId("userId")
        coEvery {
            messageRepository.starMessages(messageIds)
        } just runs
        coEvery {
            conversationsRepository.updateConversationsAfterChangingMessagesStarredStatus(
                messageIds,
                action,
                userId
            )
        } just runs

        // when
        changeMessagesStarredStatus(messageIds, action, userId)

        // then
        coVerify {
            messageRepository.starMessages(messageIds)
            conversationsRepository.updateConversationsAfterChangingMessagesStarredStatus(
                messageIds,
                action,
                userId
            )
        }
    }

    @Test
    fun verifyCorrectMethodsAreCalledWhenActionIsUnstar() = runBlockingTest {
        // given
        val messageIds = listOf("messageId1", "messageId2")
        val action = ChangeMessagesStarredStatus.Action.ACTION_UNSTAR
        val userId = UserId("userId")
        coEvery {
            messageRepository.unStarMessages(messageIds)
        } just runs
        coEvery {
            conversationsRepository.updateConversationsAfterChangingMessagesStarredStatus(
                messageIds,
                action,
                userId
            )
        } just runs

        // when
        changeMessagesStarredStatus(messageIds, action, userId)

        // then
        coVerify {
            messageRepository.unStarMessages(messageIds)
            conversationsRepository.updateConversationsAfterChangingMessagesStarredStatus(
                messageIds,
                action,
                userId
            )
        }
    }
}
