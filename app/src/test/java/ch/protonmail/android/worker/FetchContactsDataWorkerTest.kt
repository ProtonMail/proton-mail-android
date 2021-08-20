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
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactsDataResponse
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactData
import me.proton.core.domain.entity.UserId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.assertIs
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchContactsDataWorkerTest {

    private val context: Context = mockk(relaxed = true)

    private val parameters: WorkerParameters = mockk(relaxed = true)

    private val contactDao: ContactDao = mockk()
    private val databaseProvider: DatabaseProvider = mockk {
        every { provideContactDao(any()) } returns contactDao
    }

    private val api: ProtonMailApiManager = mockk()

    private val userManager: UserManager = mockk {
        every { currentUserId } returns UserId("id")
    }

    private lateinit var worker: FetchContactsDataWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        worker = FetchContactsDataWorker(
            context = context,
            params = parameters,
            api = api,
            databaseProvider = databaseProvider,
            userManager = userManager
        )
    }

    @Test
    fun verityThatInNormalConditionSuccessResultIsReturned() =
        runBlockingTest {
            // given
            val contact = mockk<ContactData>(relaxed = true)
            val contactsList = listOf(contact)
            val response = mockk<ContactsDataResponse> {
                every { contacts } returns contactsList
                every { total } returns contactsList.size
            }
            coEvery { api.fetchContacts(0, Constants.CONTACTS_PAGE_SIZE) } returns response
            coEvery { contactDao.saveAllContactsData(contactsList) } returns listOf(1)
            val expected = ListenableWorker.Result.success()

            // when
            val operationResult = worker.doWork()

            // then
            coVerify { contactDao.saveAllContactsData(contactsList) }
            assertEquals(expected, operationResult)
        }

    @Test
    fun properErrorIsReturnedIfNoLoggedInUser() = runBlockingTest {
        // given
        every { userManager.currentUserId } returns null

        // when
        val operationResult = worker.doWork()

        // then
        assertIs<ListenableWorker.Result.Failure>(operationResult)
        val failure = operationResult as ListenableWorker.Result.Failure
        assertEquals("Can't fetch contacts without a logged in user", failure.outputData.keyValueMap.values.first())
    }

    @Test
    fun verityThatWhenExceptionIsThrownJustOnceRetryResultIsReturned() =
        runBlockingTest {
            // given
            val exceptionMessage = "testException"
            val testException = Exception(exceptionMessage)
            coEvery { api.fetchContacts(0, Constants.CONTACTS_PAGE_SIZE) } throws testException
            val expected = ListenableWorker.Result.retry()

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(expected, operationResult)
        }

    @Test
    fun verityThatWhenExceptionIsThrownFalseResultIsReturnedAfterMoreThanThreeFailures() =
        runBlockingTest {
            // given
            val exceptionMessage = "testException"
            val testException = Exception(exceptionMessage)
            val retryCount = 3
            coEvery { api.fetchContacts(0, Constants.CONTACTS_PAGE_SIZE) } throws testException
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code $exceptionMessage")
            )
            every { parameters.runAttemptCount } returns retryCount

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(expected, operationResult)
        }

    @Test
    fun verityThatWhenExceptionIsThrownRetryResultIsReturnedAfterTwoFailures() =
        runBlockingTest {
            // given
            val exceptionMessage = "testException"
            val testException = Exception(exceptionMessage)
            val retryCount = 2
            coEvery { api.fetchContacts(0, Constants.CONTACTS_PAGE_SIZE) } throws testException
            val expected = ListenableWorker.Result.retry()
            every { parameters.runAttemptCount } returns retryCount

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(expected, operationResult)
        }
}
