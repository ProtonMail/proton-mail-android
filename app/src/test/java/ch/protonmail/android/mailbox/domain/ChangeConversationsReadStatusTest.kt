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

package ch.protonmail.android.mailbox.domain

import ch.protonmail.android.core.Constants
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

class ChangeConversationsReadStatusTest {

    private val conversationsRepository = mockk<ConversationsRepository>()

    private val changeConversationsReadStatus = ChangeConversationsReadStatus(conversationsRepository)

    @Test
    fun verifyMarkReadIsCalledWhenReadActionIsReceived() {
        runBlockingTest {
            // given
            coEvery {
                conversationsRepository.markRead(TestData.Conversation.ids, TestData.User.id)
            } returns ConversationsActionResult.Success

            // when
            changeConversationsReadStatus(
                TestData.Conversation.ids,
                ChangeConversationsReadStatus.Action.ACTION_MARK_READ,
                TestData.User.id,
                TestData.Conversation.locationId
            )

            // then
            coVerify {
                conversationsRepository.markRead(TestData.Conversation.ids, TestData.User.id)
            }
        }
    }

    @Test
    fun verifyMarkUnreadIsCalledWhenUnreadActionIsReceived() {
        runBlockingTest {
            // given
            coEvery {
                conversationsRepository.markUnread(
                    TestData.Conversation.ids,
                    TestData.User.id,
                    TestData.Conversation.locationId
                )
            } returns ConversationsActionResult.Success

            // when
            changeConversationsReadStatus(
                TestData.Conversation.ids,
                ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                TestData.User.id,
                TestData.Conversation.locationId
            )

            // then
            coVerify {
                conversationsRepository.markUnread(
                    TestData.Conversation.ids,
                    TestData.User.id,
                    TestData.Conversation.locationId
                )
            }
        }
    }

    @Test
    fun verifyUseCaseReturnsSuccessResultWhenRepositoryReturnsSuccessResult() {
        runBlockingTest {
            // given
            val expectedResult = ConversationsActionResult.Success
            coEvery {
                conversationsRepository.markRead(TestData.Conversation.ids, TestData.User.id)
            } returns ConversationsActionResult.Success

            // when
            val result = changeConversationsReadStatus(
                TestData.Conversation.ids,
                ChangeConversationsReadStatus.Action.ACTION_MARK_READ,
                TestData.User.id,
                TestData.Conversation.locationId
            )

            // then
            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun verifyUseCaseReturnsErrorResultWhenRepositoryReturnsErrorResult() {
        runBlockingTest {
            // given
            val expectedResult = ConversationsActionResult.Error
            coEvery {
                conversationsRepository.markUnread(
                    TestData.Conversation.ids,
                    TestData.User.id,
                    TestData.Conversation.locationId
                )
            } returns ConversationsActionResult.Error

            // when
            val result = changeConversationsReadStatus(
                TestData.Conversation.ids,
                ChangeConversationsReadStatus.Action.ACTION_MARK_UNREAD,
                TestData.User.id,
                TestData.Conversation.locationId
            )

            // then
            assertEquals(expectedResult, result)
        }
    }
}

private object TestData {
    object User {
        val id = UserId("id")
    }
    object Conversation {
        const val ID_1 = "conversation1"
        const val ID_2 = "conversation2"
        val ids = listOf(ID_1, ID_2)
        val locationId = Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
    }
}
