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

package ch.protonmail.android.utils.extensions

import androidx.work.WorkInfo
import androidx.work.workDataOf
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

class WorkInfoExtensionsKtTest {

    @Test
    fun verifyThatReducedWorkWithFirstSuccessReturnsTrue() {
        // given
        val emptyData = workDataOf()
        val workInfoEnqueued = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.ENQUEUED,
            emptyData,
            emptyList(),
            emptyData,
            0
        )
        val workInfoRunning = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.RUNNING,
            emptyData,
            emptyList(),
            emptyData,
            0
        )
        val outputData = workDataOf("a" to "b")
        val workInfoSuceeded = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.SUCCEEDED,
            outputData,
            emptyList(),
            outputData,
            0
        )
        val workInfoFailed = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.FAILED,
            emptyData,
            emptyList(),
            emptyData,
            0
        )
        val listOfWorkInfo = listOf(
            workInfoEnqueued,
            workInfoRunning,
            workInfoSuceeded,
            workInfoFailed
        )
        val expected = true

        // when
        val result = listOfWorkInfo.reduceWorkInfoToBoolean()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatReducedWorkWithFirstFailureReturnsFalse() {
        // given
        val emptyData = workDataOf()
        val workInfoEnqueued = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.ENQUEUED,
            emptyData,
            emptyList(),
            emptyData,
            0
        )
        val workInfoRunning = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.RUNNING,
            emptyData,
            emptyList(),
            emptyData,
            0
        )
        val outputData = workDataOf("a" to "b")
        val workInfoSuceeded = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.SUCCEEDED,
            outputData,
            emptyList(),
            outputData,
            0
        )
        val workInfoFailed = WorkInfo(
            UUID.randomUUID(),
            WorkInfo.State.FAILED,
            emptyData,
            emptyList(),
            emptyData,
            0
        )
        val listOfWorkInfo = listOf(
            workInfoEnqueued,
            workInfoRunning,
            workInfoFailed,
            workInfoSuceeded
        )
        val expected = false

        // when
        val result = listOfWorkInfo.reduceWorkInfoToBoolean()

        // then
        assertEquals(expected, result)
    }
}
