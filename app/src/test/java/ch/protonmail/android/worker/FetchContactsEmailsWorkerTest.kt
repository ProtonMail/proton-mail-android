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
import ch.protonmail.android.core.UserManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.util.android.workmanager.toWorkData
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactsEmailsWorkerTest {

    private val context: Context = mockk(relaxed = true)

    private val parameters: WorkerParameters = mockk(relaxed = true)

    private val userManager: UserManager = mockk(relaxed = true)

    private val contactEmailsManager: ContactEmailsManager = mockk()

    private val worker = FetchContactsEmailsWorker(
        context,
        parameters,
        userManager,
        contactEmailsManagerFactory = mockk {
            every { create(any()) } returns contactEmailsManager
        }
    )

    @Test
    fun verityThatInNormalConditionSuccessResultIsReturned() =
        runBlockingTest {
            // given
            coEvery { contactEmailsManager.refresh() } returns Unit
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
            coEvery { contactEmailsManager.refresh() } throws testException
            val expected = ListenableWorker.Result.failure(WorkerError(exceptionMessage).toWorkData())

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(expected, operationResult)
        }
}
