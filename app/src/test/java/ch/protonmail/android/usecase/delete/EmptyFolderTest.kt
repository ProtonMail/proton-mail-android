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

package ch.protonmail.android.usecase.delete

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
 * Tests the behaviour of [EmptyFolder]
 */
class EmptyFolderTest {

    private val messageRepository: MessageRepository = mockk {
        coEvery { emptyFolder(any(), any()) } just runs
    }
    private val conversationsRepository: ConversationsRepository = mockk {
        coEvery { updateConversationsWhenEmptyingFolder(any(), any()) } just runs
    }

    private val emptyFolder = EmptyFolder(messageRepository, conversationsRepository)

    private val testUserId = UserId("userId")
    private val testLabelId = "labelId"

    @Test
    fun `should call the appropriate methods from the repositories for emptying the folder`() = runBlockingTest {
        // when
        emptyFolder(testUserId, testLabelId)

        // then
        coVerify {
            conversationsRepository.updateConversationsWhenEmptyingFolder(testUserId, testLabelId)
            messageRepository.emptyFolder(testUserId, testLabelId)
        }
    }
}
