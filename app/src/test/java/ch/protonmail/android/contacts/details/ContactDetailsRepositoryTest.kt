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

package ch.protonmail.android.contacts.details

import app.cash.turbine.test
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import ch.protonmail.android.labels.data.mapper.LabelEntityDomainMapper
import ch.protonmail.android.labels.domain.LabelRepository
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
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.test.kotlin.flowTest
import kotlin.test.Test

class ContactDetailsRepositoryTest {

    private val jobManager: JobManager = mockk(relaxed = true)

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)

    private val labelEntityDomainMapper: LabelEntityDomainMapper = mockk()

    private val labelsRepository: LabelRepository = mockk()

    private val contactRepository: ContactsRepository = mockk()

    private val contactDao: ContactDao = mockk {
        every { deleteContactData(any()) } just Runs
        every { deleteAllContactsEmailsBlocking(any()) } just Runs
        every { saveContactDataBlocking(any()) } returns 0
        coEvery { saveAllContactsEmails(any<List<ContactEmail>>()) } returns mockk()
    }

    private val dispatchers = TestDispatcherProvider()
   
    private val repository = ContactDetailsRepository(
        jobManager, apiManager, contactDao, dispatchers, labelsRepository,
        contactRepository
    )

    @Test
    fun saveContactEmailsSavesAllTheContactEmailsToContactsDb() {
        runTest(dispatchers.Main) {
            val emails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom", lastUsedTime = 111),
                ContactEmail("ID2", "secondary@proton.com", "Mike", lastUsedTime = 112)
            )

            repository.saveContactEmails(emails)

            coVerify { contactDao.saveAllContactsEmails(emails) }
        }
    }

    @Test
    fun updateContactDataWithServerIdReadsContactFromDbAndSavesItBackWithServerId() {
        runTest(dispatchers.Main) {
            val contactDbId = 782L
            val contactData = ContactData("contactDataId", "name").apply { dbId = contactDbId }
            val contactServerId = "serverId"
            every { contactDao.findContactDataByDbId(contactDbId) } returns contactData

            repository.updateContactDataWithServerId(contactData, contactServerId)

            verify { contactDao.findContactDataByDbId(contactDbId) }
            val expectedContactData = contactData.copy(contactId = contactServerId)
            verify { contactDao.saveContactDataBlocking(expectedContactData) }
        }
    }

    @Test
    fun updateAllContactEmailsRemovesAllExistingEmailsFromDbAndSavesServerOnes() {
        runTest(dispatchers.Main) {
            val contactId = "contactId"
            val localContactEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom", lastUsedTime = 111),
                ContactEmail("ID2", "secondary@proton.com", "Mike", lastUsedTime = 112)
            )
            val serverEmails = listOf(
                ContactEmail("ID3", "martin@proton.com", "Martin", lastUsedTime = 111),
                ContactEmail("ID4", "kent@proton.com", "kent", lastUsedTime = 112)
            )
            coEvery { contactDao.findContactEmailsByContactId(contactId) } returns localContactEmails

            repository.updateAllContactEmails(contactId, serverEmails)

            coVerify { contactDao.findContactEmailsByContactId(contactId) }
            verify { contactDao.deleteAllContactsEmailsBlocking(localContactEmails) }
            coVerify { contactDao.saveAllContactsEmails(serverEmails) }
        }
    }

    @Test
    fun deleteContactDataDeletesContactDataFromDb() {
        runTest(dispatchers.Main) {
            val contactData = ContactData("contactDataId", "name").apply { dbId = 2345L }

            repository.deleteContactData(contactData)

            verify { contactDao.deleteContactData(contactData) }
        }
    }

    @Test
    fun saveContactDataSavesDataToDbReturningTheSavedContactDbId() {
        runTest(dispatchers.Main) {
            val contactData = ContactData("1243", "Tyler")
            val expectedDbId = 8945L
            every { contactDao.saveContactDataBlocking(contactData) } returns expectedDbId

            val actualDbId = repository.saveContactData(contactData)

            verify { contactDao.saveContactDataBlocking(contactData) }
            assertEquals(expectedDbId, actualDbId)
        }
    }

    @Test
    fun verifyObservedContactDetailsAreFetchedFromDbSuccessfullyWhenTheyArePresent() = runTest(dispatchers.Main) {
        // given
        val testContactId = "testContactId"
        val mockContact = mockk<FullContactDetails>()
        coEvery { contactDao.observeFullContactDetailsById(testContactId) } returns flowOf(mockContact)

        // when
        repository.observeFullContactDetails(testContactId).test {
            // then
            assertEquals(mockContact, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun verifyObservedContactDetailsAreFetchedFromApiWhenTheyAreNotInTheDbAndThenEmittedFromDb() =
        runTest(dispatchers.Main) {
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
            flowTest(repository.observeFullContactDetails(testContactId)) {
                dbResponseFlow.send(null)

                // then
                coVerify { contactDao.insertFullContactDetails(mockContact) }
                dbResponseFlow.send(mockContact)
                assertEquals(mockContact, awaitItem())
            }
    }

}
