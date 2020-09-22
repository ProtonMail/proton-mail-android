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

package ch.protonmail.android.worker

import androidx.work.ListenableWorker
import assert4k.`is`
import assert4k.assert
import assert4k.equals
import assert4k.that
import assert4k.times
import assert4k.type
import assert4k.unaryPlus
import io.mockk.mockk
import me.proton.core.util.android.workmanager.toWorkData
import kotlin.test.Test

class WorkerErrorTest {

    @Test
    fun `can create a WorkerError`() {
        val mockkWorker = mockk<ListenableWorker>()
        val workerError = mockkWorker.failure(IllegalStateException("The work has failed"))
        assert that workerError * {
            it `is` type<ListenableWorker.Result.Failure>()
            +errorData equals """{"message":"The work has failed"}"""
        }
    }

    @Test
    fun `can get a WorkerError`() {
        val workerError = WorkerError("The work has failed")
        val data = workerError.toWorkData()
        assert that data.error() equals workerError
    }

    private val ListenableWorker.Result.errorData get() =
        (this as ListenableWorker.Result.Failure).outputData.keyValueMap.values.first()
}
