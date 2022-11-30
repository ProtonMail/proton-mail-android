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

package ch.protonmail.android.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.DeleteResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteContactWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var databaseProvider: DatabaseProvider

    @RelaxedMockK
    private lateinit var contactDatabase: ContactDatabase

    @MockK
    private lateinit var contactDao: ContactDao

    @MockK
    private lateinit var api: ProtonMailApiManager

    private val dispatchers = TestDispatcherProvider()

    private lateinit var worker: DeleteContactWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        every { userManager.requireCurrentUserId() } returns mockk()
        every { databaseProvider.provideContactDatabase(any()) } returns contactDatabase
        every { databaseProvider.provideContactDao(any()) } returns contactDao

        worker = DeleteContactWorker(
            context,
            parameters,
            api,
            userManager,
            databaseProvider,
            dispatchers
        )
    }


    @Test
    fun verifyWorkerFailsWithNoContactIdsProvided() {
        runTest(dispatchers.Main) {
            // given
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty contacts list")
            )

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifySuccessResultIsGeneratedWithRequiredParameters() {
        runTest(dispatchers.Main) {
            // given
            val contactId = "id111"
            val deleteResponse = mockk<DeleteResponse> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            val contactData = mockk<ContactData>()
            val contactEmail = mockk<ContactEmail>()
            val expected = ListenableWorker.Result.success()

            every { contactDao.findContactDataByIdBlocking(contactId) } returns contactData
            every { contactDao.findContactEmailsByContactIdBlocking(contactId) } returns listOf(contactEmail)
            every { contactDao.deleteAllContactsEmailsBlocking(any()) } returns mockk()
            every { contactDao.deleteContactData(any()) } returns mockk()
            every { parameters.inputData } returns workDataOf(KEY_INPUT_DATA_CONTACT_IDS to arrayOf(contactId))
            coEvery { api.deleteContact(any()) } returns deleteResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifyFailureResultIsGeneratedWithRequiredParametersButWrongBackendResponse() {
        runTest(dispatchers.Main) {
            // given
            val contactId = "id111"
            val randomErrorCode = 11212
            val deleteResponse = mockk<DeleteResponse> {
                every { code } returns randomErrorCode
            }
            val contactData = mockk<ContactData>()
            val contactEmail = mockk<ContactEmail>()
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code $randomErrorCode")
            )

            every { contactDao.findContactDataByIdBlocking(contactId) } returns contactData
            every { contactDao.findContactEmailsByContactIdBlocking(contactId) } returns listOf(contactEmail)
            every { contactDao.deleteAllContactsEmailsBlocking(any()) } returns mockk()
            every { contactDao.deleteContactData(any()) } returns mockk()
            every { parameters.inputData } returns workDataOf(KEY_INPUT_DATA_CONTACT_IDS to arrayOf(contactId))
            coEvery { api.deleteContact(any()) } returns deleteResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }
}
