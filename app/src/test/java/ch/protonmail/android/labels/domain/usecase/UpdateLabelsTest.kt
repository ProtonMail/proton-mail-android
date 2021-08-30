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

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.worker.ApplyLabelWorker
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test

class UpdateLabelsTest {

    @MockK
    private lateinit var applyLabelWorker: ApplyLabelWorker.Enqueuer

    @MockK
    private lateinit var messageRepository: MessageRepository

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var labelRepository: LabelRepository

    private lateinit var useCase: UpdateLabels

    private val testUserId = UserId("testUserId")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        every { applyLabelWorker.enqueue(any(), any()) } returns mockk()
        every { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        useCase = UpdateLabels(messageRepository, accountManager, labelRepository, applyLabelWorker)
    }

    @Test
    fun verifyThatLabelsUpdateIsExecuted() = runBlockingTest {

        // given
        val testMessageId = "id123"
        val testLabelId1 = "testLabelId1"
        val message = mockk<Message> {
            every { messageId } returns testMessageId
            every { labelIDsNotIncludingLocations } returns listOf(testLabelId1)
            every { addLabels(any()) } just Runs
            every { removeLabels(any()) } just Runs
        }
        val label = mockk<LabelEntity> {
            every { id } returns LabelId(testLabelId1)
        }
        coEvery { messageRepository.findMessage(testUserId, testMessageId) } returns message
        val existingLabels = listOf(label)
        coEvery { labelRepository.findAllLabels(testUserId,) } returns existingLabels
        val checkedLabelIds = listOf(testLabelId1)
        coEvery {
            messageRepository.saveMessage(
                testUserId,
                message
            )
        } returns message

        // when
        useCase.invoke(testMessageId, checkedLabelIds)

        // then
        coVerify {
            messageRepository.saveMessage(
                testUserId,
                message
            )
        }
    }
}

