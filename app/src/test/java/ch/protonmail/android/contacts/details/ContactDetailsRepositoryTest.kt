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

package ch.protonmail.android.contacts.details

import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import com.birbit.android.jobqueue.JobManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test

class ContactDetailsRepositoryTest {

    private val workManager: WorkManager = mockk(relaxed = true)

    private val jobManager: JobManager = mockk(relaxed = true)

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val contactDao: ContactDao = mockk {
        every { deleteContactData(any()) } just Runs
        every { deleteAllContactsEmails(any()) } just Runs
        every { saveContactData(any()) } returns 0
        coEvery { saveAllContactsEmails(any<List<ContactEmail>>()) } returns mockk()
    }

    private val repository = ContactDetailsRepository(
        workManager, jobManager, apiManager, contactDao, TestDispatcherProvider
    )

    @Test
    fun saveContactEmailsSavesAllTheContactEmailsToContactsDb() {
        runBlockingTest {
            val emails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )

            repository.saveContactEmails(emails)

            coVerify { contactDao.saveAllContactsEmails(emails) }
        }
    }

    @Test
    fun updateContactDataWithServerIdReadsContactFromDbAndSavesItBackWithServerId() {
        runBlockingTest {
            val contactDbId = 782L
            val contactData = ContactData("contactDataId", "name").apply { dbId = contactDbId }
            val contactServerId = "serverId"
            every { contactDao.findContactDataByDbId(contactDbId) } returns contactData

            repository.updateContactDataWithServerId(contactData, contactServerId)

            verify { contactDao.findContactDataByDbId(contactDbId) }
            val expectedContactData = contactData.copy(contactId = contactServerId)
            verify { contactDao.saveContactData(expectedContactData) }
        }
    }

    @Test
    fun updateAllContactEmailsRemovesAllExistingEmailsFromDbAndSavesServerOnes() {
        runBlockingTest {
            val contactId = "contactId"
            val localContactEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )
            val serverEmails = listOf(
                ContactEmail("ID3", "martin@proton.com", "Martin"),
                ContactEmail("ID4", "kent@proton.com", "kent")
            )
            every { contactDao.findContactEmailsByContactIdBlocking(contactId) } returns localContactEmails

            repository.updateAllContactEmails(contactId, serverEmails)

            verify { contactDao.findContactEmailsByContactIdBlocking(contactId) }
            verify { contactDao.deleteAllContactsEmails(localContactEmails) }
            coVerify { contactDao.saveAllContactsEmails(serverEmails) }
        }
    }

    @Test
    fun deleteContactDataDeletesContactDataFromDb() {
        runBlockingTest {
            val contactData = ContactData("contactDataId", "name").apply { dbId = 2345L }

            repository.deleteContactData(contactData)

            verify { contactDao.deleteContactData(contactData) }
        }
    }

    @Test
    fun saveContactDataSavesDataToDbReturningTheSavedContactDbId() {
        runBlockingTest {
            val contactData = ContactData("1243", "Tyler")
            val expectedDbId = 8945L
            every { contactDao.saveContactData(contactData) } returns expectedDbId

            val actualDbId = repository.saveContactData(contactData)

            verify { contactDao.saveContactData(contactData) }
            assertEquals(expectedDbId, actualDbId)
        }
    }
}
