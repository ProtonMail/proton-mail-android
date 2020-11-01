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
import ch.protonmail.android.contacts.ContactIdGenerator
import ch.protonmail.android.contacts.details.ContactDetailsRepository
import ch.protonmail.android.usecase.create.CreateContact.CreateContactResult
import ch.protonmail.android.worker.CreateContactWorker
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.slot
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

    @RelaxedMockK
    private lateinit var contactIdGenerator: ContactIdGenerator

    @InjectMockKs
    private lateinit var createContact: CreateContact

    private val dispatcherProvider = TestDispatcherProvider

    private val encryptedData = "encryptedContactData"
    private val signedData = "signedContactData"
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
            every { contactIdGenerator.generateRandomId() } returns "contactDataId"

            createContact("Mike", contactEmails, encryptedData, signedData)

            val emailWithContactId = ContactEmail("ID1", "email@proton.com", "Tom", contactId = "contactDataId")
            val secondaryEmailWithContactId = ContactEmail("ID2", "secondary@proton.com", "Mike", contactId = "contactDataId")
            val expectedContactEmails = listOf(emailWithContactId, secondaryEmailWithContactId)
            verify { contactsRepository.saveContactEmails(expectedContactEmails) }
        }
    }

    @Test
    fun createContactScheduleWorkerToCreateContactsThroughNetwork() {
        runBlockingTest {
            createContact("Mark", contactEmails, encryptedData, signedData)

            verify { createContactScheduler.enqueue(encryptedData, signedData) }
        }
    }

    @Test
    fun createContactReturnsErrorWhenContactCreationThroughNetworkFails() {
        runBlockingTest {
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData

            val result = createContact("Name", contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(CreateContactResult.GenericError, result.value)
        }
    }

    @Test
    fun createContactReturnsOnlineContactCreationPendingWhenContactCreationThroughNetworkIsPending() {
        runBlockingTest {
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.ENQUEUED)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData

            val result = createContact("Alex", contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(CreateContactResult.OnlineContactCreationPending, result.value)
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
            every { contactIdGenerator.generateRandomId() } returns "723"

            val result = createContact("FooName", contactEmails, encryptedData, signedData)
            result.observeForever { }

            val expectedContactData = ContactData("723", "FooName")
            assertEquals(CreateContactResult.Success, result.value)
            verify { contactsRepository.updateContactDataWithServerId(expectedContactData, contactServerId) }
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
            every { contactIdGenerator.generateRandomId() } returns "8234823"

            val result = createContact("Bogdan", contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(CreateContactResult.Success, result.value)

            verify { gson.fromJson(serverEmailsJson, emailListType) }
            verify { contactsRepository.updateAllContactEmails("8234823", contactServerEmails) }
        }
    }

    @Test
    fun createContactMapsWorkerErrorToCreateContactResultWhenWorkerFailsWithAnError() {
        runBlockingTest {
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM to "ContactAlreadyExistsError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData
            every { contactIdGenerator.generateRandomId() } returns "92394823"

            val result = createContact("Mino", contactEmails, encryptedData, signedData)
            result.observeForever { }

            val expectedContactData = ContactData("92394823", "Mino")
            verify { contactsRepository.deleteContactData(expectedContactData) }
            assertEquals(CreateContactResult.ContactAlreadyExistsError, result.value)
        }
    }

    @Test
    fun createContactMapsInvalidEmailWorkerErrorToCreateContactResultWhenWorkerFailsWithInvalidEmailError() {
        runBlockingTest {
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM to "InvalidEmailError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData
            every { contactIdGenerator.generateRandomId() } returns "92394823"

            val result = createContact("Dan", contactEmails, encryptedData, signedData)
            result.observeForever { }

            val expectedContactData = ContactData("92394823", "Dan")
            verify { contactsRepository.deleteContactData(expectedContactData) }
            assertEquals(CreateContactResult.InvalidEmailError, result.value)
        }
    }

    @Test
    fun createContactMapsDuplicatedEmailWorkerErrorToCreateContactResultWhenWorkerFailsWithDuplicatedEmailError() {
        runBlockingTest {
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM to "DuplicatedEmailError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData
            every { contactIdGenerator.generateRandomId() } returns "2398238"

            val result = createContact("Test Name", contactEmails, encryptedData, signedData)
            result.observeForever { }

            val expectedContactData = ContactData("2398238", "Test Name")
            verify { contactsRepository.deleteContactData(expectedContactData) }
            assertEquals(CreateContactResult.DuplicatedEmailError, result.value)
        }
    }

    @Test
    fun createContactDoesNotDeleteLocalContactDataWhenApiRequestFailsWithServerError() {
        runBlockingTest {
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM to "ServerError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData

            val result = createContact("Bar", contactEmails, encryptedData, signedData)
            result.observeForever { }

            verify(exactly = 0) { contactsRepository.deleteContactData(any()) }
            assertEquals(CreateContactResult.GenericError, result.value)
        }
    }

    @Test
    fun createContactSavesContactDataWithGivenContactNameToRepository() {
        runBlockingTest {
            val contactName = "Contact Name"
            every { contactIdGenerator.generateRandomId() } returns "1233"

            createContact(contactName, contactEmails, encryptedData, signedData)

            val expected = ContactData("1233", contactName)
            verify { contactsRepository.saveContactData(expected) }
        }
    }

    @Test
    fun createContactSetDbIdToCreatedContactAfterSavingItToRepository() {
        // In this test, we need to mock the worker to fail and verify the call to `deleteContactData` as
        // that was the first usage of `contactData` where we could verify that dbId had the right value
        runBlockingTest {
            val contactName = "Contact Name"
            val contactData = ContactData("8238", contactName)
            val dbId = 7347L
            every { contactIdGenerator.generateRandomId() } returns "8238"
            every { contactsRepository.saveContactData(contactData) } returns dbId
            val workOutputData = workDataOf(
                KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM to "InvalidEmailError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every { createContactScheduler.enqueue(encryptedData, signedData) } returns workerStatusLiveData

            val result = createContact(contactName, contactEmails, encryptedData, signedData)
            result.observeForever { }

            val contactDataSlot = slot<ContactData>()
            verify { contactsRepository.deleteContactData(capture(contactDataSlot)) }
            assertEquals(dbId, contactDataSlot.captured.dbId)
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
