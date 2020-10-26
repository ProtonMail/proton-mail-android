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
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CreateContactWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var contactsDao: ContactsDao

    private var dispatcherProvider = TestDispatcherProvider

    @InjectMockKs
    private lateinit var worker: CreateContactWorker

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `worker fails when requested contactData database ID parameter is not passed`() {
        runBlockingTest {
            every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID, -1) } answers { -1 }

            val result = worker.doWork()

            assertEquals(ListenableWorker.Result.failure(), result)
        }
    }

    @Test
    fun `worker reads contactData from DB`() {
        runBlockingTest {
            val contactDataDbId = 123L
            every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID, -1) } answers { contactDataDbId }

            val result = worker.doWork()

            verify { contactsDao.findContactDataByDbId(contactDataDbId) }
            assertEquals(ListenableWorker.Result.success(), result)
        }
    }

}
