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
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.workDataOf
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.contacts.details.ContactDetailsRepository
import ch.protonmail.android.worker.CreateContactWorker
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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
import java.lang.reflect.Type
import java.util.UUID

class CreateContactTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var createContactScheduler: CreateContactWorker.Enqueuer

    @RelaxedMockK
    private lateinit var contactsRepository: ContactDetailsRepository

    @RelaxedMockK
    private lateinit var gson: Gson

    @InjectMockKs
    private lateinit var createContact: CreateContact

    private val dispatcherProvider = TestDispatcherProvider

    private val encryptedData = "encryptedContactData"
    private val signedData = "signedContactData"
    private val contactData = ContactData("contactDataId", "name")
    private val contactEmails = listOf(
        ContactEmail("ID1", "email@proton.com", "Tom"),
        ContactEmail("ID2", "secondary@proton.com", "Mike")
    )

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun createContactSavesContactEmailsWithContactIdToRepository() {
        runBlockingTest {
            createContact(contactData, contactEmails, encryptedData, signedData)

            val emailWithContactId = ContactEmail("ID1", "email@proton.com", "Tom", contactId = "contactDataId")
            val secondaryEmailWithContactId = ContactEmail("ID2", "secondary@proton.com", "Mike", contactId = "contactDataId")
            val expectedContactEmails = listOf(emailWithContactId, secondaryEmailWithContactId)
            verify { contactsRepository.saveContactEmails(expectedContactEmails) }
        }
    }

    @Test
    fun createContactScheduleWorkerToCreateContactsThroughNetwork() {
        runBlockingTest {
            createContact(contactData, contactEmails, encryptedData, signedData)

            verify { createContactScheduler.enqueue(encryptedData, signedData) }
        }
    }

    @Test
    fun createContactReturnsErrorWhenContactCreationThroughNetworkFails() {
        runBlockingTest {
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData

            val result = createContact(contactData, contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(CreateContact.CreateContactResult.Error, result.value)
        }
    }

    @Test
    fun createContactDoesNotEmitAnyValuesWhenContactCreationThroughNetworkIsPending() {
        runBlockingTest {
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.ENQUEUED)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData

            val result = createContact(contactData, contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(null, result.value)
        }
    }

    @Test
    fun createContactSavesContactDataWithServerIdWhenContactCreationThroughNetworkSucceeds() {
        runBlockingTest {
            val contactServerId = "contactServerId"
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID to contactServerId,
                KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON to "{}"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData
            every { gson.fromJson<List<ContactEmail>>(any<String>(), any<Type>()) } answers { emptyList() }

            val result = createContact(contactData, contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(CreateContact.CreateContactResult.Success, result.value)
            verify { contactsRepository.updateContactDataWithServerId(contactData, contactServerId) }
        }
    }

    @Test
    fun createContactReplacesContactEmailsWithServerOnesWhenContactCreationThroughNetworkSucceeds() {
        runBlockingTest {
            val emailListType = TypeToken.getParameterized(List::class.java, ContactEmail::class.java).type
            val contactServerEmails = listOf(
                ContactEmail("ID1", "email@proton.com", "Tom", contactId = "contactDataId"),
                ContactEmail("ID2", "secondary@proton.com", "Mike", contactId = "contactDataId")
            )
            val serverEmailsJson = """
                [{"selected":false,"pgpIcon":0,"pgpIconColor":0,"pgpDescription":0,"isPGP":false,"ID":"ID1","Email":"email@proton.com","Name":"Tom","Defaults":0,"Order":0},
                {"selected":false,"pgpIcon":0,"pgpIconColor":0,"pgpDescription":0,"isPGP":false,"ID":"ID2","Email":"secondary@proton.com","Name":"Mike","Defaults":0,"Order":0}
             """.trimIndent()

            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID to "ID",
                KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON to serverEmailsJson
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData
            every { gson.fromJson<List<ContactEmail>>(serverEmailsJson, emailListType) } returns contactServerEmails

            val result = createContact(contactData, contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(CreateContact.CreateContactResult.Success, result.value)

            verify { gson.fromJson(serverEmailsJson, emailListType) }
            verify { contactsRepository.updateAllContactEmails(contactData.contactId, contactServerEmails) }
        }
    }

    private fun buildCreateContactWorkerResponse(
        endState: WorkInfo.State,
        outputData: Data? = workDataOf()
    ): MutableLiveData<WorkInfo> {
        val workInfo = WorkInfo(
            UUID.randomUUID(),
            endState,
            outputData!!,
            emptyList(),
            outputData,
            0
        )
        val workerStatusLiveData = MutableLiveData<WorkInfo>()
        workerStatusLiveData.value = workInfo
        return workerStatusLiveData
    }
}
