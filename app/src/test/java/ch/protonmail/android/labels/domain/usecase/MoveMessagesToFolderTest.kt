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
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.repository.MessageRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.just
import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

class MoveMessagesToFolderTest {

    @MockK
    private lateinit var repository: MessageRepository

    private lateinit var useCase: MoveMessagesToFolder

    private val userId = UserId("TestUser")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = MoveMessagesToFolder(repository)
    }

    @Test
    fun verifyMoveToTrashWorksAsExpected() = runBlocking {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
        val currentFolderLabelId = "currentFolderId"
        coEvery { repository.moveToTrash(messageIds, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify { repository.moveToTrash(messageIds, currentFolderLabelId, userId) }
    }

    @Test
    fun verifyMoveToArchieWorksAsExpected() = runBlocking {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
        val currentFolderLabelId = "currentFolderId"
        coEvery { repository.moveToArchive(messageIds, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify { repository.moveToArchive(messageIds, currentFolderLabelId, userId) }
    }

    @Test
    fun verifyMoveToInboxWorksAsExpected() = runBlocking {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()
        val currentFolderLabelId = "currentFolderId"
        coEvery { repository.moveToInbox(messageIds, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify { repository.moveToInbox(messageIds, currentFolderLabelId, userId) }
    }

    @Test
    fun verifyMoveToSpamWorksAsExpected() = runBlocking {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = Constants.MessageLocationType.SPAM.messageLocationTypeValue.toString()
        val currentFolderLabelId = "currentFolderId"
        coEvery { repository.moveToSpam(messageIds, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify { repository.moveToSpam(messageIds, currentFolderLabelId, userId) }
    }

    @Test
    fun verifyMoveToCustomFolderWorksAsExpected() = runBlocking {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = "newFolderCustomId"
        val currentFolderLabelId = "currentFolderId"
        coEvery { repository.moveToCustomFolderLocation(messageIds, newFolderLocation, currentFolderLabelId, userId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId, userId)

        // then
        coVerify { repository.moveToCustomFolderLocation(messageIds, newFolderLocation, currentFolderLabelId, userId) }
    }
}
