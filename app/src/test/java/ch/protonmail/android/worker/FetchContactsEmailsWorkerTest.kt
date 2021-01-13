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

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.api.segments.contact.ContactEmailsManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.util.android.workmanager.toWorkData
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactsEmailsWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var contactEmailsManager: ContactEmailsManager

    private lateinit var worker: FetchContactsEmailsWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        worker = FetchContactsEmailsWorker(context, parameters, contactEmailsManager)
    }

    @Test
    fun verityThatInNormalConditionSuccessResultIsReturned() =
        runBlockingTest {
            // given
            every { contactEmailsManager.refresh() } returns Unit
            val expected = ListenableWorker.Result.success()

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(expected, operationResult)
        }


    @Test
    fun verityThatWhenExceptionIsThrownFalseResultIsReturned() =
        runBlockingTest {
            // given
            val exceptionMessage = "testException"
            val testException = Exception(exceptionMessage)
            every { contactEmailsManager.refresh() } throws testException
            val expected = ListenableWorker.Result.failure(WorkerError(exceptionMessage).toWorkData())

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(expected, operationResult)
        }
}
