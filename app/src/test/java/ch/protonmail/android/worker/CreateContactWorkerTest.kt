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
import androidx.work.Data
import androidx.work.ListenableWorker.Result
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_INVALID_EMAIL
import ch.protonmail.android.core.Constants
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.utils.FileHelper
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerErrors
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
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test

class CreateContactWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @RelaxedMockK
    private lateinit var apiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var crypto: UserCrypto

    @RelaxedMockK
    private lateinit var apiResponse: ContactResponse

    @RelaxedMockK
    private lateinit var workManager: WorkManager

    @MockK
    private lateinit var fileHelper: FileHelper

    @InjectMockKs
    private lateinit var worker: CreateContactWorker

    private var dispatcherProvider = TestDispatcherProvider

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        coEvery { fileHelper.readStringFromFilePath(any()) } returns "vCardString"
    }

    @Test
    fun enqueuerSchedulesCreateContactWorkSettingTheInputParamsCorrectly() {
        val encryptedData = "encrypted contact data"
        val signedData = "signed contact data"
        val requestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(requestSlot)) } answers { mockk() }

        CreateContactWorker.Enqueuer(workManager).enqueue(encryptedData, signedData)

        val constraints = requestSlot.captured.workSpec.constraints
        val inputData = requestSlot.captured.workSpec.input
        val actualEncryptedData = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_ENCRYPTED_DATA_PATH)
        val actualSignedData = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_SIGNED_DATA)
        assertEquals(encryptedData, actualEncryptedData)
        assertEquals(signedData, actualSignedData)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        verify { workManager.getWorkInfoByIdLiveData(any()) }
    }

    @Test
    fun workerInvokesCreateContactEndpointWithEncryptedContactData() {
        runBlockingTest {
            val encryptedContactData = "encrypted-data"
            val signedContactData = "signed-data"
            givenEncryptedContactDataParamsIsValid(encryptedContactData)
            givenSignedContactDataParamsIsValid(signedContactData)
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            worker.doWork()

            val encryptedData = crypto.encrypt(encryptedContactData, false).armored
            val encryptDataSignature = crypto.sign(encryptedContactData)
            val signedDataSignature = crypto.sign(signedContactData)
            val contactEncryptedDataType2 = ContactEncryptedData(signedContactData, signedDataSignature, Constants.VCardType.SIGNED)
            val contactEncryptedDataType3 = ContactEncryptedData(encryptedData, encryptDataSignature, Constants.VCardType.SIGNED_ENCRYPTED)
            val contactEncryptedDataList = listOf(contactEncryptedDataType2, contactEncryptedDataType3)
            val createContactRequest = CreateContact(contactEncryptedDataList)
            coVerify { apiManager.createContact(createContactRequest) }
        }
    }

    @Test
    fun workerReturnsErrorWhenContactCreationOnApiFails() {
        runBlockingTest {
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns 500
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val error = CreateContactWorkerErrors.ServerError
            val expectedFailure = Result.failure(
                Data.Builder().putString(KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerReturnsServerContactIdAndAllResponsesContactEmailsSerialisedWhenApiCallSucceedsRetuningNonEmptyContactId() {
        runBlockingTest {
            val contactId = "serverContactId"
            val serverContactEmails =
                listOf(ContactEmail("emailId", "first@pm.me", "firstcontact", lastUsedTime = 111))
            val serverContactEmails1 = listOf(
                ContactEmail("emailId1", "second@pm.me", "secondcontact", lastUsedTime = 113),
                ContactEmail("emailId2", "third@pm.me", "thirdcontact", lastUsedTime = 112)
            )
            val responses = mockk<ContactResponse.Responses> {
                every { response.contact.emails } returns serverContactEmails
            }
            val responses1 = mockk<ContactResponse.Responses> {
                every { response.contact.emails } returns serverContactEmails1
            }
            val responsesList = listOf(responses, responses1)
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns Constants.RESPONSE_CODE_MULTIPLE_OK
            every { apiResponse.contactId } returns contactId
            every { apiResponse.responses } returns responsesList
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val contactEmailsOutputJson = readTextFileContent("contact-emails-output.json")
            val expectedResult = Result.success(
                Data.Builder()
                    .putString(KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID, contactId)
                    .putString(KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON, contactEmailsOutputJson)
                    .build()
            )

            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun workerReturnsContactAlreadyExistsErrorWhenApiReturnsEmptyServerContactIdAndContactAlreadyExistsErrorCode() {
        runBlockingTest {
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns Constants.RESPONSE_CODE_MULTIPLE_OK
            every { apiResponse.responseErrorCode } returns RESPONSE_CODE_ERROR_EMAIL_EXIST
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val error = CreateContactWorkerErrors.ContactAlreadyExistsError
            val expectedFailure = Result.failure(
                Data.Builder().putString(KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerReturnsInvalidEmailErrorWhenApiReturnsEmptyServerContactIdAndInvalidEmailErrorCode() {
        runBlockingTest {
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns Constants.RESPONSE_CODE_MULTIPLE_OK
            every { apiResponse.responseErrorCode } returns RESPONSE_CODE_ERROR_INVALID_EMAIL
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val error = CreateContactWorkerErrors.InvalidEmailError
            val expectedFailure = Result.failure(
                Data.Builder().putString(KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerReturnsDuplicatedEmailErrorWhenApiReturnsEmptyServerContactIdAndDuplicatedEmailErrorCode() {
        runBlockingTest {
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns Constants.RESPONSE_CODE_MULTIPLE_OK
            every { apiResponse.responseErrorCode } returns RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val error = CreateContactWorkerErrors.DuplicatedEmailError
            val expectedFailure = Result.failure(
                Data.Builder().putString(KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }


    private fun givenAllInputParametersAreValid() {
        givenEncryptedContactDataParamsIsValid()
        givenSignedContactDataParamsIsValid()
    }

    private fun givenEncryptedContactDataParamsIsValid(encryptedContactData: String? = "encrypted-data") {
        every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_ENCRYPTED_DATA_PATH) } answers { encryptedContactData!! }
    }

    private fun givenSignedContactDataParamsIsValid(signedContactData: String? = "signed-data") {
        every { parameters.inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_SIGNED_DATA) } answers { signedContactData!! }
    }

    private fun readTextFileContent(fileName: String): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream(fileName)!!
        return inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().replace("\\s".toRegex(), "") }
    }

}
