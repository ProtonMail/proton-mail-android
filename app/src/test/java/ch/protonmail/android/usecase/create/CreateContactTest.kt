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

package ch.protonmail.android.usecase.create

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import androidx.work.Data
import androidx.work.WorkInfo
import androidx.work.workDataOf
import ch.protonmail.android.contacts.ContactIdGenerator
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.testdata.UserTestData.userId
import ch.protonmail.android.usecase.create.CreateContact.Result
import ch.protonmail.android.utils.FileHelper
import ch.protonmail.android.worker.CreateContactWorker
import ch.protonmail.android.worker.KEY_OUT_CREATE_CONTACT_EMAILS_JSON
import ch.protonmail.android.worker.KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM
import ch.protonmail.android.worker.KEY_OUT_CREATE_CONTACT_SERVER_ID
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Rule
import java.io.File
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test

class CreateContactTest : CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @RelaxedMockK
    private lateinit var createContactScheduler: CreateContactWorker.Enqueuer

    @RelaxedMockK
    private lateinit var contactsRepository: ContactDetailsRepository

    @RelaxedMockK
    private lateinit var contactIdGenerator: ContactIdGenerator

    @RelaxedMockK
    private lateinit var networkConnectivityManager: NetworkConnectivityManager

    @MockK
    private lateinit var context: Context

    @MockK
    private lateinit var fileHelper: FileHelper

    @InjectMockKs
    private lateinit var createContact: CreateContact

    private val encryptedData = "encryptedContactData"
    private val signedData = "signedContactData"
    private val contactEmails = listOf(
        ContactEmail("ID1", "email@proton.com", "Tom", lastUsedTime = 111),
        ContactEmail("ID2", "secondary@proton.com", "Mike", lastUsedTime = 112)
    )

    private val cacheDirPath = "CacheDirPath"
    private val encryptedVCardFilePath = cacheDirPath + File.separator + VCARD_TEMP_FILE_NAME

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        coEvery { fileHelper.saveStringToFile(any(), any()) } returns mockk()
        every { context.cacheDir } returns File(cacheDirPath)
        coEvery { contactsRepository.saveContactData(any()) } returns 324L
    }

    @Test
    fun createContactSavesContactEmailsWithContactIdToRepository() {
        runTest {
            every { contactIdGenerator.generateRandomId() } returns "contactDataId"

            createContact(userId, "Mike", contactEmails, encryptedData, signedData)

            val emailWithContactId =
                ContactEmail("ID1", "email@proton.com", "Tom", contactId = "contactDataId", lastUsedTime = 111)
            val secondaryEmailWithContactId =
                ContactEmail("ID2", "secondary@proton.com", "Mike", contactId = "contactDataId", lastUsedTime = 112)
            val expectedContactEmails = listOf(emailWithContactId, secondaryEmailWithContactId)
            coVerify { contactsRepository.saveContactEmails(expectedContactEmails) }
        }
    }

    @Test
    fun createContactScheduleWorkerToCreateContactsThroughNetwork() {
        runTest {
            createContact(userId, "Mark", contactEmails, encryptedData, signedData)

            verify { createContactScheduler.enqueue(userId, encryptedVCardFilePath, signedData) }
        }
    }

    @Test
    fun createContactReturnsErrorWhenContactCreationThroughNetworkFails() {
        runTest {
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED)
            coEvery { contactsRepository.saveContactData(any()) } returns 324L
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData

            val result = createContact(userId, "Name", contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(Result.GenericError, result.value)
        }
    }

    @Test
    fun createContactReturnsOnlineContactCreationPendingWhenContactCreationThroughNetworkIsPending() {
        runTest {
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.ENQUEUED)
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData

            val result = createContact(userId, "Alex", contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(Result.OnlineContactCreationPending, result.value)
        }
    }

    @Test
    fun createContactDoesNothingWhenContactCreationThroughNetworkIsPendingButConnectivityIsAvailable() {
        runTest {
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.ENQUEUED)
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData
            every { networkConnectivityManager.isInternetConnectionPossible() } returns true

            val result = createContact(userId, "Alex", contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(null, result.value)
        }
    }

    @Test
    fun createContactSavesContactDataWithServerIdWhenContactCreationThroughNetworkSucceeds() {
        runTest {
            val contactServerId = "contactServerId"
            val workOutputData = workDataOf(
                KEY_OUT_CREATE_CONTACT_SERVER_ID to contactServerId,
                KEY_OUT_CREATE_CONTACT_EMAILS_JSON to "[]"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData
            every { contactIdGenerator.generateRandomId() } returns "723"

            val result = createContact(userId, "FooName", contactEmails, encryptedData, signedData)
            result.observeForever { }

            val expectedContactData = ContactData("723", "FooName")
            assertEquals(Result.Success, result.value)
            coVerify { contactsRepository.updateContactDataWithServerId(expectedContactData, contactServerId) }
        }
    }

    @Test
    fun createContactReplacesContactEmailsWithServerOnesWhenContactCreationThroughNetworkSucceeds() {
        runTest {
            val contactServerEmails = listOf(
                ContactEmail(
                    "VyCrmhybZZ8A-I6w==",
                    "email@proton.com",
                    "Tom",
                    contactId = "jB_6lbgFc7QA12w==",
                    order = 1,
                    defaults = 1,
                    type = mutableListOf("email"),
                    labelIds = emptyList(),
                    lastUsedTime = 111
                ),
                ContactEmail(
                    "HsdksdkjnZ8A-I6w==",
                    "secondary@proton.com",
                    "Mike",
                    contactId = "jB_6lbgFc7QA12w==",
                    labelIds = emptyList(),
                    lastUsedTime = 112
                )
            )
            val serverEmailsJson = """
                [
                    {
                        "contactEmailId": "VyCrmhybZZ8A-I6w==",
                        "contactId": "jB_6lbgFc7QA12w==",
                        "defaults": 1,
                        "email": "email@proton.com",
                        "isPGP": false,
                        "labelIds": [],
                        "name": "Tom",
                        "order": 1,
                        "pgpDescription": 0,
                        "pgpIcon": 0,
                        "pgpIconColor": 0,
                        "selected": false,
                        "type": [
                            "email"
                        ],
                        "lastUsedTime": "111"
                    },
                    {
                        "contactEmailId": "HsdksdkjnZ8A-I6w==",
                        "contactId": "jB_6lbgFc7QA12w==",
                        "defaults": 0,
                        "email": "secondary@proton.com",
                        "isPGP": false,
                        "labelIds": [],
                        "name": "Mike",
                        "order": 0,
                        "pgpDescription": 0,
                        "pgpIcon": 0,
                        "pgpIconColor": 0,
                        "selected": false,
                        "type": null,
                        "lastUsedTime": "112"
                    }
                ]

            """.trimIndent()

            val workOutputData = workDataOf(
                KEY_OUT_CREATE_CONTACT_SERVER_ID to "ID",
                KEY_OUT_CREATE_CONTACT_EMAILS_JSON to serverEmailsJson
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.SUCCEEDED, workOutputData)
            every { createContactScheduler.enqueue(userId, encryptedVCardFilePath, signedData) } returns workerStatusLiveData
            every { contactIdGenerator.generateRandomId() } returns "8234823"

            val result = createContact(userId, "Bogdan", contactEmails, encryptedData, signedData)
            result.observeForever { }

            assertEquals(Result.Success, result.value)
            coVerify { contactsRepository.updateAllContactEmails("8234823", contactServerEmails) }
        }
    }

    @Test
    fun createContactMapsWorkerErrorToCreateContactResultWhenWorkerFailsWithAnError() {
        runTest {
            val workOutputData = workDataOf(
                KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM to "ContactAlreadyExistsError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData
            every { contactIdGenerator.generateRandomId() } returns "92394823"

            val result = createContact(userId, "Mino", contactEmails, encryptedData, signedData)
            result.observeForever { }

            val expectedContactData = ContactData("92394823", "Mino")
            coVerify { contactsRepository.deleteContactData(expectedContactData) }
            assertEquals(Result.ContactAlreadyExistsError, result.value)
        }
    }

    @Test
    fun createContactMapsInvalidEmailWorkerErrorToCreateContactResultWhenWorkerFailsWithInvalidEmailError() {
        runTest {
            val workOutputData = workDataOf(
                KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM to "InvalidEmailError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData
            every { contactIdGenerator.generateRandomId() } returns "92394823"

            val result = createContact(userId, "Dan", contactEmails, encryptedData, signedData)
            result.observeForever { }

            val expectedContactData = ContactData("92394823", "Dan")
            coVerify { contactsRepository.deleteContactData(expectedContactData) }
            assertEquals(Result.InvalidEmailError, result.value)
        }
    }

    @Test
    fun createContactMapsDuplicatedEmailWorkerErrorToCreateContactResultWhenWorkerFailsWithDuplicatedEmailError() {
        runTest {
            val workOutputData = workDataOf(
                KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM to "DuplicatedEmailError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData
            every { contactIdGenerator.generateRandomId() } returns "2398238"

            val result = createContact(userId, "Test Name", contactEmails, encryptedData, signedData)
            result.observeForever { }

            val expectedContactData = ContactData("2398238", "Test Name")
            coVerify { contactsRepository.deleteContactData(expectedContactData) }
            assertEquals(Result.DuplicatedEmailError, result.value)
        }
    }

    @Test
    fun createContactDoesNotDeleteLocalContactDataWhenApiRequestFailsWithServerError() {
        runTest {
            val workOutputData = workDataOf(
                KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM to "ServerError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData

            val result = createContact(userId, "Bar", contactEmails, encryptedData, signedData)
            result.observeForever { }

            coVerify(exactly = 0) { contactsRepository.deleteContactData(any()) }
            assertEquals(Result.GenericError, result.value)
        }
    }

    @Test
    fun createContactSavesContactDataWithGivenContactNameToRepository() {
        runTest {
            val contactName = "Contact Name"
            every { contactIdGenerator.generateRandomId() } returns "1233"

            createContact(userId, contactName, contactEmails, encryptedData, signedData)

            val expected = ContactData("1233", contactName)
            coVerify { contactsRepository.saveContactData(expected) }
        }
    }

    @Test
    fun createContactSetDbIdToCreatedContactAfterSavingItToRepository() {
        // In this test, we need to mock the worker to fail and verify the call to `deleteContactData` as
        // that was the first usage of `contactData` where we could verify that dbId had the right value
        runTest {
            val contactName = "Contact Name"
            val contactData = ContactData("8238", contactName)
            val dbId = 7347L
            every { contactIdGenerator.generateRandomId() } returns "8238"
            coEvery { contactsRepository.saveContactData(contactData) } returns dbId
            val workOutputData = workDataOf(
                KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM to "InvalidEmailError"
            )
            val workerStatusLiveData = buildCreateContactWorkerResponse(WorkInfo.State.FAILED, workOutputData)
            every {
                createContactScheduler.enqueue(
                    userId, encryptedVCardFilePath, signedData
                )
            } returns workerStatusLiveData

            val result = createContact(userId, contactName, contactEmails, encryptedData, signedData)
            result.observeForever { }

            val contactDataSlot = slot<ContactData>()
            coVerify { contactsRepository.deleteContactData(capture(contactDataSlot)) }
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
