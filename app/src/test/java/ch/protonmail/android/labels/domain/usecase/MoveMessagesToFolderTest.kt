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
import ch.protonmail.android.repository.MessageRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

class MoveMessagesToFolderTest {

    @MockK
    private lateinit var repository: MessageRepository

    private lateinit var useCase: MoveMessagesToFolder

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = MoveMessagesToFolder(repository)
    }

    @Test
    fun verifyMoveToTrashWorksAsExpected() {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = Constants.MessageLocationType.TRASH.messageLocationTypeValue.toString()
        val currentFolderLabelId = "currentFolderId"
        every { repository.moveToTrash(messageIds, currentFolderLabelId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId)

        // then
        verify { repository.moveToTrash(messageIds, currentFolderLabelId) }
    }

    @Test
    fun verifyMoveToArchieWorksAsExpected() {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = Constants.MessageLocationType.ARCHIVE.messageLocationTypeValue.toString()
        val currentFolderLabelId = "currentFolderId"
        every { repository.moveToArchive(messageIds, currentFolderLabelId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId)

        // then
        verify { repository.moveToArchive(messageIds, currentFolderLabelId) }
    }

    @Test
    fun verifyMoveToInboxWorksAsExpected() {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = Constants.MessageLocationType.INBOX.messageLocationTypeValue.toString()
        val currentFolderLabelId = "currentFolderId"
        every { repository.moveToInbox(messageIds, currentFolderLabelId) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId)

        // then
        verify { repository.moveToInbox(messageIds, currentFolderLabelId) }
    }

    @Test
    fun verifyMoveToSpamWorksAsExpected() {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = Constants.MessageLocationType.SPAM.messageLocationTypeValue.toString()
        val currentFolderLabelId = "currentFolderId"
        every { repository.moveToSpam(messageIds) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId)

        // then
        verify { repository.moveToSpam(messageIds) }
    }

    @Test
    fun verifyMoveToCustomFolderWorksAsExpected() {

        // given
        val messageIds = listOf("id1", "id2")
        val newFolderLocation = "newFolderCustomId"
        val currentFolderLabelId = "currentFolderId"
        every { repository.moveToCustomFolderLocation(messageIds, newFolderLocation) } just Runs

        // when
        useCase.invoke(messageIds, newFolderLocation, currentFolderLabelId)

        // then
        verify { repository.moveToCustomFolderLocation(messageIds, newFolderLocation) }
    }
}
