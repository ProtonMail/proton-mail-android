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
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.core.Constants
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DeleteLabelWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var contactsDb: ContactsDatabase

    @MockK
    private lateinit var messagesDatabase: MessagesDatabase

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var worker: DeleteLabelWorker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        worker = DeleteLabelWorker(
            context,
            parameters,
            api,
            contactsDb,
            messagesDatabase,
            TestDispatcherProvider
        )
    }

    @Test
    fun verifyWorkerFailsWithNoLabelIdProvided() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty label id")
            )

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun verifyWorkerSuccessesWithLabelIdProvided() {
        runBlockingTest {
            // given
            val labelId = "id1"
            val deleteResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            val contactLabel = mockk<ContactLabel>()
            val expected = ListenableWorker.Result.success()

            every { parameters.inputData } returns workDataOf(KEY_INPUT_DATA_LABEL_ID to labelId)
            every { contactsDb.findContactGroupById(labelId) } returns contactLabel
            every { contactsDb.deleteContactGroup(contactLabel) } returns Unit
            every { messagesDatabase.deleteLabelById(labelId) } returns Unit
            coEvery { api.deleteLabel(any()) } returns deleteResponse

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun verifyWorkerFailureWithLabelIdProvidedButBadServerResponse() {
        runBlockingTest {
            // given
            val labelId = "id1"
            val errorCode = 12123
            val deleteResponse = mockk<ResponseBody> {
                every { code } returns errorCode
            }
            val contactLabel = mockk<ContactLabel>()
            val expected = ListenableWorker.Result.failure()

            every { parameters.inputData } returns workDataOf(KEY_INPUT_DATA_LABEL_ID to labelId)
            every { contactsDb.findContactGroupById(labelId) } returns contactLabel
            coEvery { api.deleteLabel(any()) } returns deleteResponse

            // when
            val result = worker.doWork()

            // then
            assertEquals(expected, result)
        }
    }

}
