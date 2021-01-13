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
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import com.birbit.android.jobqueue.JobManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class ContactDetailsRepositoryTest {

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @RelaxedMockK
    private lateinit var jobManager: JobManager

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var contactsDao: ContactsDao

    @InjectMockKs
    private lateinit var repository: ContactDetailsRepository

    private val dispatcherProvider = TestDispatcherProvider

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun saveContactEmailsSavesAllTheContactEmailsToContactsDb() {
        runBlockingTest {
            val emails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )

            repository.saveContactEmails(emails)

            verify { contactsDao.saveAllContactsEmails(emails) }
        }
    }

    @Test
    fun updateContactDataWithServerIdReadsContactFromDbAndSavesItBackWithServerId() {
        runBlockingTest {
            val contactDbId = 782L
            val contactData = ContactData("contactDataId", "name").apply { dbId = contactDbId }
            val contactServerId = "serverId"
            every { contactsDao.findContactDataByDbId(contactDbId) } returns contactData

            repository.updateContactDataWithServerId(contactData, contactServerId)

            verify { contactsDao.findContactDataByDbId(contactDbId) }
            val expectedContactData = contactData.copy(contactId = contactServerId)
            verify { contactsDao.saveContactData(expectedContactData) }
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
            every { contactsDao.findContactEmailsByContactId(contactId) } returns localContactEmails

            repository.updateAllContactEmails(contactId, serverEmails)

            verify { contactsDao.findContactEmailsByContactId(contactId) }
            verify { contactsDao.deleteAllContactsEmails(localContactEmails) }
            verify { contactsDao.saveAllContactsEmails(serverEmails) }
        }
    }

    @Test
    fun deleteContactDataDeletesContactDataFromDb() {
        runBlockingTest {
            val contactData = ContactData("contactDataId", "name").apply { dbId = 2345L }

            repository.deleteContactData(contactData)

            verify { contactsDao.deleteContactData(contactData) }
        }
    }

    @Test
    fun saveContactDataSavesDataToDbReturningTheSavedContactDbId() {
        runBlockingTest {
            val contactData = ContactData("1243", "Tyler")
            val expectedDbId = 8945L
            every { contactsDao.saveContactData(contactData) } returns expectedDbId

            val actualDbId = repository.saveContactData(contactData)

            verify { contactsDao.saveContactData(contactData) }
            assertEquals(expectedDbId, actualDbId)
        }
    }
}
