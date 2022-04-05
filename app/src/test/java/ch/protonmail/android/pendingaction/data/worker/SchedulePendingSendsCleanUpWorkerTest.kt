/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.pendingaction.data.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.testdata.MessageTestData
import io.mockk.every
import io.mockk.mockk
import junit.framework.Assert.assertTrue
import org.junit.Test
import kotlin.test.assertEquals

internal class SchedulePendingSendsCleanUpWorkerTest {

    private val contextMock = mockk<Context>()
    private val workParametersMock = mockk<WorkerParameters> {
        every { inputData } returns INPUT_DATA
    }
    private val schedulePendingSendsCleanUpWorker = SchedulePendingSendsCleanUpWorker(
        contextMock,
        workParametersMock
    )

    @Test
    fun `should just return a success passing on the inputs in result`() {
        // when
        val result = schedulePendingSendsCleanUpWorker.doWork()

        // then
        assertTrue(result is ListenableWorker.Result.Success)
        assertEquals(INPUT_DATA, result.outputData)
    }

    private companion object TestData {

        val INPUT_DATA = workDataOf(
            KEY_INPUT_MESSAGE_ID to MessageTestData.MESSAGE_ID_RAW,
            KEY_INPUT_MESSAGE_SUBJECT to MessageTestData.MESSAGE_SUBJECT,
            KEY_INPUT_MESSAGE_DATABASE_ID to MessageTestData.MESSAGE_DATABASE_ID
        )
    }
}
