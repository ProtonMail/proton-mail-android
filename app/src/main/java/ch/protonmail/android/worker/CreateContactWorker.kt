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
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.lifecycle.LiveData
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_INVALID_EMAIL
import ch.protonmail.android.core.Constants
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerResult.ContactAlreadyExistsError
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerResult.InvalidEmailError
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerResult.ServerError
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID = "keyCreateContactInputDataContactDataDBId"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED = "keyCreateContactInputDataContactEmails"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_ENCRYPTED_DATA = "keyCreateContactInputDataEncryptedData"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_SIGNED_DATA = "keyCreateContactInputDataSignedData"
internal const val KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_NAME = "keyCreateContactWorkerResultError"
internal const val KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID = "keyCreateContactWorkerResultServerId"
internal const val KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED = "keyCreateContactWorkerResultEmailsSerialised"

private const val INVALID_CONTACT_DATA_DB_ID = -1L

class CreateContactWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiManager: ProtonMailApiManager,
    private val contactsDao: ContactsDao,
    private val gson: Gson,
    private val crypto: UserCrypto
) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        getContactData() ?: return Result.failure()
        getContactEmails() ?: return Result.failure()
        val encryptedDataParam = getInputEncryptedData() ?: return Result.failure()
        val signedDataParam = getInputSignedData() ?: return Result.failure()

        val apiRequest = createContactRequestBody(encryptedDataParam, signedDataParam)
        val apiResponse = apiManager.createContact(apiRequest)

        if (apiResponse?.code != Constants.RESPONSE_CODE_MULTIPLE_OK) {
            return failureWithError(ServerError)
        }

        if (apiResponse.contactId.isNotEmpty()) {
            val outputData = contactIdAndEmailsOutputData(apiResponse)
            return Result.success(outputData)
        }

        if (isContactAlreadyExistsError(apiResponse)) {
            return failureWithError(ContactAlreadyExistsError)
        }

        if (isInvalidEmailError(apiResponse)) {
            return failureWithError(InvalidEmailError)
        }

        return Result.success()
    }

    private fun contactIdAndEmailsOutputData(apiResponse: ContactResponse): Data {
        val contactEmails: List<ContactEmail> = apiResponse.responses.flatMap {
            it.response.contact.emails ?: emptyList()
        }

        val jsonContactEmails = gson.toJson(contactEmails)

        val outputData = workDataOf(
            KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID to apiResponse.contactId,
            KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED to jsonContactEmails
        )
        return outputData
    }

    private fun isInvalidEmailError(apiResponse: ContactResponse) =
        apiResponse.responseErrorCode == RESPONSE_CODE_ERROR_INVALID_EMAIL

    private fun isContactAlreadyExistsError(apiResponse: ContactResponse) =
        apiResponse.responseErrorCode == RESPONSE_CODE_ERROR_EMAIL_EXIST || apiResponse
            .responseErrorCode == RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL

    private fun failureWithError(error: CreateContactWorkerResult): Result {
        val errorData = workDataOf(KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_NAME to error.name)
        return Result.failure(errorData)
    }

    private fun getInputSignedData() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_SIGNED_DATA)

    private fun getInputEncryptedData() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_ENCRYPTED_DATA)

    private fun createContactRequestBody(encryptedDataParam: String, signedDataParam: String): CreateContact {
        val encryptedData = crypto.encrypt(encryptedDataParam, false).armored
        val encryptDataSignature = crypto.sign(encryptedDataParam)
        val signedDataSignature = crypto.sign(signedDataParam)

        val contactEncryptedDataType2 = ContactEncryptedData(signedDataParam, signedDataSignature, Constants.VCardType.SIGNED)
        val contactEncryptedDataType3 = ContactEncryptedData(encryptedData, encryptDataSignature, Constants.VCardType.SIGNED_ENCRYPTED)

        val contactEncryptedDataList = ArrayList<ContactEncryptedData>()
        contactEncryptedDataList.add(contactEncryptedDataType2)
        contactEncryptedDataList.add(contactEncryptedDataType3)

        return CreateContact(contactEncryptedDataList)
    }

    private fun getContactEmails(): List<ContactEmail>? {
        val contactEmailsSerialised = getContactEmailsSerialised() ?: return null
        return deserialize(contactEmailsSerialised)
    }

    private fun getContactData(): ContactData? {
        val contactDataDbId = getContactDataDatabaseId()
        if (contactDataDbId == INVALID_CONTACT_DATA_DB_ID) {
            return null
        }
        return contactsDao.findContactDataByDbId(contactDataDbId)
    }

    private fun getContactEmailsSerialised() =
        inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED)

    private fun getContactDataDatabaseId() =
        inputData.getLong(KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID, INVALID_CONTACT_DATA_DB_ID)

    private fun deserialize(contactEmailsSerialised: String): List<ContactEmail>? {
        val emailListType: java.lang.reflect.Type = TypeToken.getParameterized(
            List::class.java, ContactEmail::class.java
        ).type

        return try {
            gson.fromJson<List<ContactEmail>>(contactEmailsSerialised, emailListType)
        } catch (e: Exception) {
            Timber.w(e)
            null
        }
    }

    enum class CreateContactWorkerResult {
        ServerError,
        ContactAlreadyExistsError,
        InvalidEmailError
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {
        fun enqueue(
            contactData: ContactData,
            contactEmails: List<ContactEmail>
        ): LiveData<WorkInfo> {

            val createContactRequest = OneTimeWorkRequestBuilder<CreateContactWorker>()
                .setInputData(
                    workDataOf(
                        KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID to contactData,
                        KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED to contactEmails
                    )
                ).build()

            workManager.enqueue(createContactRequest)
            return workManager.getWorkInfoByIdLiveData(createContactRequest.id)
        }
    }
}
