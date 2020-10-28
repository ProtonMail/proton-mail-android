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

package ch.protonmail.android.usecase.create

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.work.WorkInfo
import androidx.work.workDataOf
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.worker.CreateContactWorker
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.verify
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID

class CreateContactTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var createContactScheduler: CreateContactWorker.Enqueuer

    @RelaxedMockK
    private lateinit var contactsDao: ContactsDao

    @RelaxedMockK
    private lateinit var mockObserver: Observer<CreateContact.CreateContactResult>

    @InjectMockKs
    private lateinit var createContact: CreateContact

    private val dispatcherProvider = TestDispatcherProvider

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun createContactSavesContactEmailsWithContactIdInDatabase() {
        runBlockingTest {
            val contactData = ContactData("contactDataId", "name")
            val contactEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )

            createContact(contactData, contactEmails)

            val emailWithContactId = ContactEmail("ID1", "email@proton.com", "Tom", contactId = "contactDataId")
            val secondaryEmailWithContactId = ContactEmail("ID2", "secondary@proton.com", "Mike", contactId = "contactDataId")
            val expectedContactEmails = listOf(emailWithContactId, secondaryEmailWithContactId)
            verify { contactsDao.saveAllContactsEmails(expectedContactEmails) }
        }
    }

    @Test
    fun createContactScheduleWorkerToCreateContactsThroughNetwork() {
        runBlockingTest {
            val contactData = ContactData("contactDataId", "name")
            val contactEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )

            createContact(contactData, contactEmails)

            val emailWithContactId = ContactEmail("ID1", "email@proton.com", "Tom", contactId = "contactDataId")
            val secondaryEmailWithContactId = ContactEmail("ID2", "secondary@proton.com", "Mike", contactId = "contactDataId")
            val expectedContactEmails = listOf(emailWithContactId, secondaryEmailWithContactId)
            verify { createContactScheduler.enqueue(contactData, expectedContactEmails) }
        }
    }

    @Test
    fun createContactReturnsSuccessWhenContactCreationThroughNetworkSucceeds() {
        runBlockingTest {
            val contactData = ContactData("contactDataId", "name")
            val contactEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.SUCCEEDED)
            every { createContactScheduler.enqueue(any(), any()) } returns workerStatusLiveData

            val result = createContact(contactData, contactEmails)
            result.observeForever { }

            assertEquals(CreateContact.CreateContactResult.Success, result.value)
        }
    }

    @Test
    fun createContactReturnsErrorWhenContactCreationThroughNetworkFails() {
        runBlockingTest {
            val contactData = ContactData("contactDataId", "name")
            val contactEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED)
            every { createContactScheduler.enqueue(any(), any()) } returns workerStatusLiveData

            val result = createContact(contactData, contactEmails)
            result.observeForever { }

            assertEquals(CreateContact.CreateContactResult.Error, result.value)
        }
    }

    @Test
    fun createContactDoesNotEmitAnyValuesWhenContactCreationThroughNetworkIsPending() {
        runBlockingTest {
            val contactData = ContactData("contactDataId", "name")
            val contactEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom"),
                ContactEmail("ID2", "secondary@proton.com", "Mike")
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.ENQUEUED)
            every { createContactScheduler.enqueue(any(), any()) } returns workerStatusLiveData

            val result = createContact(contactData, contactEmails)
            result.observeForever { }

            assertEquals(null, result.value)
        }
    }

    private fun buildCreateContactWorkerResponse(endState: WorkInfo.State): MutableLiveData<WorkInfo> {
        val outputData = workDataOf()
        val workInfo = WorkInfo(
            UUID.randomUUID(),
            endState,
            outputData,
            emptyList(),
            outputData,
            0
        )
        val workerStatusLiveData = MutableLiveData<WorkInfo>()
        workerStatusLiveData.value = workInfo
        return workerStatusLiveData
    }
}
