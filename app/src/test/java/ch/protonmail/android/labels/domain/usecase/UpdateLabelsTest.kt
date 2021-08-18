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

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.data.local.model.LabelEntity
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.repository.MessageRepository
import io.mockk.MockKAnnotations
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class UpdateLabelsTest {

    @MockK
    private lateinit var repository: MessageDetailsRepository

    @MockK
    private lateinit var newRepository: MessageRepository

    private lateinit var useCase: UpdateLabels

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        useCase = UpdateLabels(repository, newRepository)
    }

    @Test
    fun verifyThatLabelsUpdateIsExecuted() = runBlockingTest {

        // given
        val testMessageId = "id123"
        val testLabelId1 = "testLabelId1"
        val message = mockk<Message> {
            every { messageId } returns testMessageId
            every { labelIDsNotIncludingLocations } returns listOf(testLabelId1)
        }
        val label = mockk<LabelEntity> {
            every { id } returns testLabelId1
        }
        coEvery { newRepository.findMessageById(testMessageId) } returns message
        val existingLabels = listOf(label)
        coEvery { repository.getAllLabels() } returns existingLabels
        val checkedLabelIds = listOf(testLabelId1)
        coEvery {
            repository.findAllLabelsWithIds(
                message,
                checkedLabelIds,
                any()
            )
        } just Runs

        // when
        useCase.invoke(testMessageId, checkedLabelIds)

        // then
        coVerify {
            repository.findAllLabelsWithIds(
                message,
                checkedLabelIds,
                existingLabels
            )
        }
    }
}

