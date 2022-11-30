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

package ch.protonmail.android.worker

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
import ch.protonmail.android.testdata.UserTestData.userId
import ch.protonmail.android.utils.FileHelper
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerErrors
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Assert.assertEquals
import kotlin.test.Test

class CreateContactWorkerTest {

    private val apiManager: ProtonMailApiManager = mockk(relaxed = true)
    private val apiResponse: ContactResponse = mockk(relaxed = true)
    private val crypto: UserCrypto = mockk(relaxed = true)
    private val cryptoFactory: UserCrypto.AssistedFactory = mockk {
        every { create(any()) } returns crypto
    }
    private val fileHelper: FileHelper = mockk {
        coEvery { readStringFromFilePath(any()) } returns "vCardString"
    }
    private val parameters: WorkerParameters = mockk(relaxed = true)
    private val workManager: WorkManager = mockk(relaxed = true)
    private val dispatchers = TestDispatcherProvider()

    private val worker = CreateContactWorker(
        apiManager = apiManager,
        context = mockk(),
        cryptoFactory = cryptoFactory,
        dispatcherProvider = dispatchers,
        fileHelper = fileHelper,
        params = parameters
    )

    @Test
    fun enqueuerSchedulesCreateContactWorkSettingTheInputParamsCorrectly() {
        val encryptedData = "encrypted contact data"
        val signedData = "signed contact data"
        val requestSlot = slot<OneTimeWorkRequest>()
        every { workManager.enqueue(capture(requestSlot)) } answers { mockk() }

        CreateContactWorker.Enqueuer(workManager).enqueue(userId, encryptedData, signedData)

        val constraints = requestSlot.captured.workSpec.constraints
        val inputData = requestSlot.captured.workSpec.input
        val actualEncryptedData = inputData.getString(KEY_IN_CREATE_CONTACT_ENC_DATA_PATH)
        val actualSignedData = inputData.getString(KEY_IN_CREATE_CONTACT_SIGNED_DATA)
        assertEquals(encryptedData, actualEncryptedData)
        assertEquals(signedData, actualSignedData)
        assertEquals(NetworkType.CONNECTED, constraints.requiredNetworkType)
        verify { workManager.getWorkInfoByIdLiveData(any()) }
    }

    @Test
    fun workerInvokesCreateContactEndpointWithEncryptedContactData() {
        runTest(dispatchers.Main) {
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
        runTest(dispatchers.Main) {
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns 500
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val error = CreateContactWorkerErrors.ServerError
            val expectedFailure = Result.failure(
                Data.Builder().putString(KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerReturnsServerContactIdAndAllResponsesContactEmailsSerialisedWhenApiCallSucceedsRetuningNonEmptyContactId() {
        runTest(dispatchers.Main) {
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
                    .putString(KEY_OUT_CREATE_CONTACT_SERVER_ID, contactId)
                    .putString(KEY_OUT_CREATE_CONTACT_EMAILS_JSON, contactEmailsOutputJson)
                    .build()
            )

            assertEquals(expectedResult, result)
        }
    }

    @Test
    fun workerReturnsContactAlreadyExistsErrorWhenApiReturnsEmptyServerContactIdAndContactAlreadyExistsErrorCode() {
        runTest(dispatchers.Main) {
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns Constants.RESPONSE_CODE_MULTIPLE_OK
            every { apiResponse.responseErrorCode } returns RESPONSE_CODE_ERROR_EMAIL_EXIST
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val error = CreateContactWorkerErrors.ContactAlreadyExistsError
            val expectedFailure = Result.failure(
                Data.Builder().putString(KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerReturnsInvalidEmailErrorWhenApiReturnsEmptyServerContactIdAndInvalidEmailErrorCode() {
        runTest(dispatchers.Main) {
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns Constants.RESPONSE_CODE_MULTIPLE_OK
            every { apiResponse.responseErrorCode } returns RESPONSE_CODE_ERROR_INVALID_EMAIL
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val error = CreateContactWorkerErrors.InvalidEmailError
            val expectedFailure = Result.failure(
                Data.Builder().putString(KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }

    @Test
    fun workerReturnsDuplicatedEmailErrorWhenApiReturnsEmptyServerContactIdAndDuplicatedEmailErrorCode() {
        runTest(dispatchers.Main) {
            givenAllInputParametersAreValid()
            every { apiResponse.code } returns Constants.RESPONSE_CODE_MULTIPLE_OK
            every { apiResponse.responseErrorCode } returns RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED
            coEvery { apiManager.createContact(any()) } answers { apiResponse }

            val result = worker.doWork()

            val error = CreateContactWorkerErrors.DuplicatedEmailError
            val expectedFailure = Result.failure(
                Data.Builder().putString(KEY_OUT_CREATE_CONTACT_RESULT_ERROR_ENUM, error.name).build()
            )
            assertEquals(expectedFailure, result)
        }
    }


    private fun givenAllInputParametersAreValid() {
        givenEncryptedContactDataParamsIsValid()
        givenSignedContactDataParamsIsValid()
    }

    private fun givenEncryptedContactDataParamsIsValid(encryptedContactData: String? = "encrypted-data") {
        every { parameters.inputData.getString(KEY_IN_CREATE_CONTACT_ENC_DATA_PATH) } answers { encryptedContactData!! }
    }

    private fun givenSignedContactDataParamsIsValid(signedContactData: String? = "signed-data") {
        every { parameters.inputData.getString(KEY_IN_CREATE_CONTACT_SIGNED_DATA) } answers { signedContactData!! }
    }

    private fun readTextFileContent(fileName: String): String {
        val inputStream = javaClass.classLoader?.getResourceAsStream(fileName)!!
        return inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().replace("\\s".toRegex(), "") }
    }

}
