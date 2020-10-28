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
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.core.QueueNetworkUtil
import com.google.gson.Gson
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

    @RelaxedMockK
    private lateinit var gson: Gson

    @RelaxedMockK
    private lateinit var networkUtils: QueueNetworkUtil

    @InjectMockKs
    private lateinit var worker: CreateContactWorker

    private var dispatcherProvider = TestDispatcherProvider

    private val contactEmailsJson = """
                [{"selected":false,"pgpIcon":0,"pgpIconColor":0,"pgpDescription":0,"isPGP":false,"ID":"ID1","Email":"email@proton.com","Defaults":0,"Order":0},
                {"selected":false,"pgpIcon":0,"pgpIconColor":0,"pgpDescription":0,"isPGP":false,"ID":"ID2","Email":"secondary@proton.com","Defaults":0,"Order":0}]
        """.trimIndent()

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `worker fails when requested contactData database ID parameter is not passed`() {
        runBlockingTest {
            every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID, -1) } answers { -1 }

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun `worker fails when requested contactEmails serialised parameter is not passed`() {
        runBlockingTest {
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED) } answers { null }

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun `worker fails when contactEmails are not deserializable from json to List of ContactEmail`() {
        runBlockingTest {
            every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED) } answers { "{ invalid json }" }

            val result = worker.doWork()

            assertEquals(Result.failure(), result)
        }
    }

    @Test
    fun `worker reads contactData from DB using ID passed as parameter`() {
        runBlockingTest {
            val contactDataDbId = 123L
            every { parameters.inputData.getLong(KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID, -1) } answers { contactDataDbId }

            worker.doWork()

            verify { contactsDao.findContactDataByDbId(contactDataDbId) }
        }
    }

}
