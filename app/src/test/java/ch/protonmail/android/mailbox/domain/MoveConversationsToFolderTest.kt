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

import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the behaviour of [MoveConversationsToFolder]
 */
class MoveConversationsToFolderTest {

    private val conversationsRepository = mockk<ConversationsRepository>()

    private val moveConversationsToFolder = MoveConversationsToFolder(
        conversationsRepository
    )

    @Test
    fun verifyMoveToFolderMethodFromRepositoryIsCalled() {
        runBlockingTest {
            // given
            val conversationIds = listOf("conversationId1", "conversationId2")
            val userId = UserId("userId")
            val folderId = "folderId"
            coEvery {
                conversationsRepository.moveToFolder(conversationIds, userId, folderId)
            } returns ConversationsActionResult.Success

            // when
            moveConversationsToFolder(conversationIds, userId, folderId)

            // then
            coVerify {
                conversationsRepository.moveToFolder(conversationIds, userId, folderId)
            }
        }
    }

    @Test
    fun verifyUseCaseReturnsErrorResultWhenRepositoryReturnsErrorResult() {
        runBlockingTest {
            // given
            val conversationIds = listOf("conversationId1", "conversationId2")
            val userId = UserId("userId")
            val folderId = "folderId"
            val expectedResult = ConversationsActionResult.Error
            coEvery {
                conversationsRepository.moveToFolder(conversationIds, userId, folderId)
            } returns ConversationsActionResult.Error

            // when
            val result = moveConversationsToFolder(conversationIds, userId, folderId)

            // then
            assertEquals(expectedResult, result)
        }
    }
}
