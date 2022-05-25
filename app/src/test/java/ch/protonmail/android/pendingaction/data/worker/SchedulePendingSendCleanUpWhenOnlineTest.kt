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

package ch.protonmail.android.pendingaction.data.worker

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkContinuation
import androidx.work.WorkManager
import ch.protonmail.android.testdata.MessageTestData
import ch.protonmail.android.testdata.WorkerTestData.UNIQUE_WORK_NAME
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.hours

internal class SchedulePendingSendCleanUpWhenOnlineTest {

    private val provideUniqueNameMock = mockk<CleanUpPendingSendWorker.ProvideUniqueName> {
        every { this@mockk.invoke(MessageTestData.MESSAGE_DATABASE_ID) } returns UNIQUE_WORK_NAME
    }
    private val workManagerMock = mockk<WorkManager>()
    private val schedulePendingSendCleanUpWhenOnline = SchedulePendingSendCleanUpWhenOnline(
        provideUniqueNameMock,
        workManagerMock
    )

    @Test
    fun `should schedule the trigger when online followed by the clean up with delay`() {
        // given
        val workContinuationMock = mockk<WorkContinuation> {
            every { then(any<OneTimeWorkRequest>()) } returns mockk(relaxed = true)
        }
        every {
            workManagerMock.beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, any<OneTimeWorkRequest>())
        } returns workContinuationMock

        // when
        schedulePendingSendCleanUpWhenOnline(
            MessageTestData.MESSAGE_ID_RAW,
            MessageTestData.MESSAGE_SUBJECT,
            MessageTestData.MESSAGE_DATABASE_ID
        )

        // then
        val workRequestCaptor = slot<OneTimeWorkRequest>()
        verify {
            workManagerMock.beginUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, capture(workRequestCaptor))
        }
        assertSchedulePendingSendsCleanUpWorkerRequest(workRequestCaptor.captured)

        verify { workContinuationMock.then(capture(workRequestCaptor)) }
        assertCleanUpPendingSendWorkerRequest(workRequestCaptor.captured)
    }

    private fun assertSchedulePendingSendsCleanUpWorkerRequest(actualWorkRequest: OneTimeWorkRequest) {
        val actualInputData = actualWorkRequest.workSpec.input
        assertEquals(NetworkType.CONNECTED, actualWorkRequest.workSpec.constraints.requiredNetworkType)
        assertEquals(MessageTestData.MESSAGE_ID_RAW, actualInputData.getString(KEY_INPUT_MESSAGE_ID))
        assertEquals(MessageTestData.MESSAGE_SUBJECT, actualInputData.getString(KEY_INPUT_MESSAGE_SUBJECT))
        assertEquals(MessageTestData.MESSAGE_DATABASE_ID, actualInputData.getLong(KEY_INPUT_MESSAGE_DATABASE_ID, -1))
        assertEquals(SchedulePendingSendsCleanUpWorker::class.java.name, actualWorkRequest.workSpec.workerClassName)
    }

    private fun assertCleanUpPendingSendWorkerRequest(actualWorkRequest: OneTimeWorkRequest) {
        val expectedCleanUpDelay = 2.hours.inWholeMilliseconds
        assertEquals(expectedCleanUpDelay, actualWorkRequest.workSpec.initialDelay)
        assertEquals(CleanUpPendingSendWorker::class.java.name, actualWorkRequest.workSpec.workerClassName)
    }
}
