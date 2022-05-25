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

package ch.protonmail.android.usecase.delete

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.work.WorkInfo
import androidx.work.workDataOf
import app.cash.turbine.test
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.usecase.DeleteLabels
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Rule
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteLabelsTest {

    @get:Rule
    val archRule = InstantTaskExecutorRule()

    private val labelRepository: LabelRepository = mockk()

    private val deleteLabel = DeleteLabels(labelRepository)

    @Test
    fun verifyThatMessageIsSuccessfullyDeleted() {
        runBlockingTest {
            // given
            val testLabelId = LabelId("Id1")
            val contactLabel = mockk<Label> {
                every { id } returns testLabelId
            }
            val finishState = WorkInfo.State.SUCCEEDED
            val outputData = workDataOf("a" to "b")
            val workInfo = WorkInfo(
                UUID.randomUUID(),
                finishState,
                outputData,
                emptyList(),
                outputData,
                0
            )
            val expected = true

            coEvery { labelRepository.findLabel(testLabelId) } returns contactLabel
            coEvery { labelRepository.scheduleDeleteLabels(listOf(testLabelId)) } returns flowOf(workInfo)

            // when
            deleteLabel(listOf(testLabelId)).test {

                // then
                assertEquals(expected, awaitItem())
                awaitComplete()
            }
        }
    }

    @Test
    fun verifyThatMessageDeleteHasFailed() {
        runBlockingTest {
            // given
            val testLabelId = LabelId("Id1")
            val contactLabel = mockk<Label> {
                every { id } returns testLabelId
            }
            val finishState = WorkInfo.State.FAILED
            val outputData = workDataOf()
            val workInfo = WorkInfo(
                UUID.randomUUID(),
                finishState,
                outputData,
                emptyList(),
                outputData,
                0
            )
            val expected = false

            coEvery { labelRepository.findLabel(testLabelId) } returns contactLabel
            coEvery { labelRepository.scheduleDeleteLabels(listOf(testLabelId)) } returns flowOf(workInfo)

            // when
            deleteLabel(listOf(testLabelId)).test {

                // then
                assertEquals(expected, awaitItem())
                awaitComplete()
            }

        }
    }

    @Test
    fun verifyThatMessageIsEnqueuedThereIsNoValueEmitted() {
        runBlockingTest {
            // given
            val testLabelId = LabelId("Id1")
            val contactLabel = mockk<Label> {
                every { id } returns testLabelId
            }
            val finishState = WorkInfo.State.ENQUEUED
            val outputData = workDataOf("a" to "b")
            val workInfo = WorkInfo(
                UUID.randomUUID(),
                finishState,
                outputData,
                emptyList(),
                outputData,
                0
            )

            coEvery { labelRepository.findLabel(testLabelId) } returns contactLabel
            coEvery { labelRepository.scheduleDeleteLabels(listOf(testLabelId)) } returns flowOf(workInfo)

            // when
            deleteLabel(listOf(testLabelId)).test {

                // then
                awaitComplete()
            }
        }
    }

}
