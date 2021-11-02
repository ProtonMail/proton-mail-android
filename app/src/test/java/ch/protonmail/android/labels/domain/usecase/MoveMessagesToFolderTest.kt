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

package ch.protonmail.android.labels.domain.usecase

import ch.protonmail.android.core.Constants
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.repository.MessageRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

class MoveMessagesToFolderTest {

    private val messageRepository: MessageRepository = mockk()

    private val conversationsRepository: ConversationsRepository = mockk()

    private lateinit var useCase: MoveMessagesToFolder

    private val userId = UserId("TestUser")
    private val messageIds = listOf("id1", "id2")
    private val currentFolderLabelId = "currentFolderId"

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = MoveMessagesToFolder(messageRepository, conversationsRepository)

        coEvery {
            conversationsRepository.updateConvosBasedOnMessagesLocation(
                userId,
                messageIds,
                currentFolderLabelId,
                any()
            )
        } just runs
    }

    @Test
    fun verifyMoveToTrashWorksAsExpected() = runBlocking {

        // given
        val newFolderLocation = Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
        coEvery { messageRepository.moveToTrash(messageIds, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify {
            messageRepository.moveToTrash(messageIds, currentFolderLabelId, userId)
            conversationsRepository.updateConvosBasedOnMessagesLocation(
                userId,
                messageIds,
                currentFolderLabelId,
                newFolderLocation
            )
        }
    }

    @Test
    fun verifyMoveToArchieWorksAsExpected() = runBlocking {

        // given
        val newFolderLocation = Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
        coEvery { messageRepository.moveToArchive(messageIds, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify {
            messageRepository.moveToArchive(messageIds, currentFolderLabelId, userId)
            conversationsRepository.updateConvosBasedOnMessagesLocation(
                userId,
                messageIds,
                currentFolderLabelId,
                newFolderLocation
            )
        }
    }

    @Test
    fun verifyMoveToInboxWorksAsExpected() = runBlocking {

        // given
        val newFolderLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()
        coEvery { messageRepository.moveToInbox(messageIds, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify {
            messageRepository.moveToInbox(messageIds, currentFolderLabelId, userId)
            conversationsRepository.updateConvosBasedOnMessagesLocation(
                userId,
                messageIds,
                currentFolderLabelId,
                newFolderLocation
            )
        }
    }

    @Test
    fun verifyMoveToSpamWorksAsExpected() = runBlocking {

        // given
        val newFolderLocation = Constants.MessageLocationType.SPAM.messageLocationTypeValue.toString()
        coEvery { messageRepository.moveToSpam(messageIds, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify {
            messageRepository.moveToSpam(messageIds, currentFolderLabelId, userId)
            conversationsRepository.updateConvosBasedOnMessagesLocation(
                userId,
                messageIds,
                currentFolderLabelId,
                newFolderLocation
            )
        }
    }

    @Test
    fun verifyMoveToCustomFolderWorksAsExpected() = runBlocking {

        // given
        val newFolderLocation = "newFolderCustomId"
        coEvery { messageRepository.moveToCustomFolderLocation(messageIds, newFolderLocation, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify {
            messageRepository.moveToCustomFolderLocation(
                messageIds,
                newFolderLocation,
                currentFolderLabelId,
                userId
            )
            conversationsRepository.updateConvosBasedOnMessagesLocation(
                userId,
                messageIds,
                currentFolderLabelId,
                newFolderLocation
            )
        }
    }
}
