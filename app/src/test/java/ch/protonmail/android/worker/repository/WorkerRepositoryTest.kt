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

package ch.protonmail.android.worker.repository

import androidx.work.Operation
import androidx.work.WorkManager
import ch.protonmail.android.testdata.WorkerTestData
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

internal class WorkerRepositoryTest {

    private val workManagerMock = mockk<WorkManager>()
    private val workerRepository = WorkerRepository(workManagerMock)

    @Test
    fun `should cancel unique work`() {
        // given
        val expectedOperation = mockk<Operation>()
        every { workManagerMock.cancelUniqueWork(WorkerTestData.UNIQUE_WORK_NAME) } returns expectedOperation

        // when
        val actualOperation = workerRepository.cancelUniqueWork(WorkerTestData.UNIQUE_WORK_NAME)

        // then
        assertEquals(expectedOperation, actualOperation)
    }
}
