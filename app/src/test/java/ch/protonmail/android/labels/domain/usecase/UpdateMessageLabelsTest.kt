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

package ch.protonmail.android.labels.domain.usecase

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.repository.MessageRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class UpdateMessageLabelsTest {

    @MockK
    private lateinit var messageRepository: MessageRepository

    @MockK
    private lateinit var conversationsRepository: ConversationsRepository

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var labelRepository: LabelRepository

    private lateinit var useCase: UpdateMessageLabels

    private val dispatchers = TestDispatcherProvider()

    private val testUserId = UserId("testUserId")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        useCase = UpdateMessageLabels(
            messageRepository,
            conversationsRepository,
            accountManager,
            labelRepository,
            dispatchers
        )
    }

    @Test
    fun verifyThatLabelsUpdateIsExecuted() = runTest(dispatchers.Main) {

        // given
        val testMessageId = "id123"
        val testLabelId1 = "testLabelId1"
        val message = mockk<Message> {
            every { messageId } returns testMessageId
            every { labelIDsNotIncludingLocations } returns listOf(testLabelId1)
            every { addLabels(any()) } just Runs
            every { removeLabels(any()) } just Runs
        }
        val label = mockk<Label> {
            every { id } returns LabelId(testLabelId1)
            every { type } returns LabelType.MESSAGE_LABEL
        }
        coEvery { messageRepository.findMessage(testUserId, testMessageId) } returns message
        val existingLabels = listOf(label)
        coEvery { labelRepository.findAllLabels(testUserId) } returns existingLabels
        val checkedLabelIds = listOf(testLabelId1)
        coEvery {
            messageRepository.saveMessage(
                testUserId,
                message
            )
        } returns message
        coEvery {
            conversationsRepository.updateConversationBasedOnMessageLabels(
                testUserId,
                testMessageId,
                any(),
                any()
            )
        } just runs

        // when
        useCase.invoke(testMessageId, checkedLabelIds)

        // then
        coVerify {
            messageRepository.saveMessage(
                testUserId,
                message
            )
            conversationsRepository.updateConversationBasedOnMessageLabels(
                testUserId,
                testMessageId,
                any(),
                any()
            )
        }
    }
}

