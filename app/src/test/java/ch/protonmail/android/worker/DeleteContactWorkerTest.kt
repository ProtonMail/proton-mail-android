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
import ch.protonmail.android.api.models.DeleteContactResponse
import ch.protonmail.android.core.Constants
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
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeleteContactWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var contactDatabase: ContactDatabase

    @MockK
    private lateinit var contactDao: ContactDao

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var worker: DeleteContactWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        worker = DeleteContactWorker(
            context,
            parameters,
            api,
            contactDao,
            contactDatabase,
            TestDispatcherProvider
        )
    }


    @Test
    fun verifyWorkerFailsWithNoContactIdsProvided() {
        runBlockingTest {
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
        runBlockingTest {
            // given
            val contactId = "id111"
            val deleteResponse = mockk<DeleteContactResponse> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            val contactData = mockk<ContactData>()
            val contactEmail= mockk<ContactEmail>()
            val expected = ListenableWorker.Result.success()

            every { contactDao.findContactDataById(contactId) } returns contactData
            every { contactDao.findContactEmailsByContactId(contactId) } returns listOf(contactEmail)
            every { contactDao.deleteAllContactsEmails(any()) } returns mockk()
            every { contactDao.deleteContactData(any()) } returns mockk()
            every { parameters.inputData } returns
                workDataOf(KEY_INPUT_DATA_CONTACT_IDS to arrayOf(contactId))
            coEvery { api.deleteContact(any()) } returns deleteResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

    @Test
    fun verifyFailureResultIsGeneratedWithRequiredParametersButWrongBackendResponse() {
        runBlockingTest {
            // given
            val contactId = "id111"
            val randomErrorCode = 11212
            val deleteResponse = mockk<DeleteContactResponse> {
                every { code } returns randomErrorCode
            }
            val contactData = mockk<ContactData>()
            val contactEmail= mockk<ContactEmail>()
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code $randomErrorCode")
            )

            every { contactDao.findContactDataById(contactId) } returns contactData
            every { contactDao.findContactEmailsByContactId(contactId) } returns listOf(contactEmail)
            every { contactDao.deleteAllContactsEmails(any()) } returns mockk()
            every { contactDao.deleteContactData(any()) } returns mockk()
            every { parameters.inputData } returns
                workDataOf(KEY_INPUT_DATA_CONTACT_IDS to arrayOf(contactId))
            coEvery { api.deleteContact(any()) } returns deleteResponse

            // when
            val operationResult = worker.doWork()

            // then
            assertEquals(operationResult, expected)
        }
    }

}
