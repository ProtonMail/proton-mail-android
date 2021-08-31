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
import app.cash.turbine.test
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.mapper.LabelsMapper
import com.birbit.android.jobqueue.JobManager
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test

class ContactDetailsRepositoryTest {

    private val workManager: WorkManager = mockk(relaxed = true)

    private val jobManager: JobManager = mockk(relaxed = true)

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val labelsMapper: LabelsMapper = mockk()

    private val labelsRepository: LabelRepository = mockk()

    private val contactDao: ContactDao = mockk {
        every { deleteContactData(any()) } just Runs
        every { deleteAllContactsEmails(any()) } just Runs
        every { saveContactData(any()) } returns 0
        coEvery { saveAllContactsEmails(any<List<ContactEmail>>()) } returns mockk()
    }

    private val repository = ContactDetailsRepository(
        jobManager, apiManager, contactDao, TestDispatcherProvider, labelsMapper, labelsRepository,
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
            coEvery { contactDao.findContactEmailsByContactId(contactId) } returns localContactEmails

            repository.updateAllContactEmails(contactId, serverEmails)

            coVerify { contactDao.findContactEmailsByContactId(contactId) }
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

    @Test
    fun verifyObservedContactDetailsAreFetchedFromDbSuccessfullyWhenTheyArePresent() = runBlockingTest {
        // given
        val testContactId = "testContactId"
        val mockContact = mockk<FullContactDetails>()
        coEvery { contactDao.observeFullContactDetailsById(testContactId) } returns flowOf(mockContact)

        // when
        repository.observeFullContactDetails(testContactId).test {
            // then
            assertEquals(mockContact, expectItem())
            expectComplete()
        }
    }

    @Test
    fun verifyObservedContactDetailsAreFetchedFromApiWhenTheyAreNotInTheDbAndThenEmittedFromDb() = runBlockingTest {
        // given
        val testContactId = "testContactId"
        val mockContact = mockk<FullContactDetails>()
        val apiResponse = mockk<FullContactDetailsResponse> {
            every { contact } returns mockContact
        }
        val dbResponseFlow = Channel<FullContactDetails?>()
        coEvery { contactDao.observeFullContactDetailsById(testContactId) } returns dbResponseFlow.receiveAsFlow()
        coEvery { apiManager.fetchContactDetails(testContactId) } returns apiResponse
        coEvery { contactDao.insertFullContactDetails(mockContact) } just Runs

        // when
        repository.observeFullContactDetails(testContactId).test {
            dbResponseFlow.send(null)

            // then
            coVerify { contactDao.insertFullContactDetails(mockContact) }
            dbResponseFlow.send(mockContact)
            assertEquals(mockContact, expectItem())
        }
    }

}
