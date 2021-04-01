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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_EMAIL_EXIST
import ch.protonmail.android.api.segments.RESPONSE_CODE_ERROR_INVALID_EMAIL
import ch.protonmail.android.core.Constants
import ch.protonmail.android.crypto.UserCrypto
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerErrors.ContactAlreadyExistsError
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerErrors.DuplicatedEmailError
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerErrors.InvalidEmailError
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerErrors.ServerError
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.proton.core.util.kotlin.DispatcherProvider
import okio.buffer
import okio.source
import java.io.File
import javax.inject.Inject

internal const val KEY_INPUT_DATA_CREATE_CONTACT_ENCRYPTED_DATA_PATH = "keyCreateContactInputDataEncryptedData"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_SIGNED_DATA = "keyCreateContactInputDataSignedData"
internal const val KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM = "keyCreateContactWorkerResultError"
internal const val KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID = "keyCreateContactWorkerResultServerId"
internal const val KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON = "keyCreateContactWorkerResultEmailsSerialised"

class CreateContactWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val apiManager: ProtonMailApiManager,
    private val crypto: UserCrypto,
    private val dispatcherProvider: DispatcherProvider
) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        val request = buildCreateContactRequestBody()
        val response = withContext(dispatcherProvider.Io) { apiManager.createContact(request) }

        if (response?.code != Constants.RESPONSE_CODE_MULTIPLE_OK) {
            return failureWithError(ServerError)
        }

        if (response.contactId.isNotEmpty()) {
            val outputData = contactIdAndEmailsOutputData(response)
            return Result.success(outputData)
        }

        if (isContactAlreadyExistsError(response)) {
            return failureWithError(ContactAlreadyExistsError)
        }

        if (isInvalidEmailError(response)) {
            return failureWithError(InvalidEmailError)
        }

        if (isDuplicatedEmailError(response)) {
            return failureWithError(DuplicatedEmailError)
        }

        return Result.failure()
    }


    private fun isDuplicatedEmailError(apiResponse: ContactResponse) =
        apiResponse.responseErrorCode == RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED

    private fun isInvalidEmailError(apiResponse: ContactResponse) =
        apiResponse.responseErrorCode == RESPONSE_CODE_ERROR_INVALID_EMAIL

    private fun isContactAlreadyExistsError(apiResponse: ContactResponse) =
        apiResponse.responseErrorCode == RESPONSE_CODE_ERROR_EMAIL_EXIST || apiResponse
            .responseErrorCode == RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL

    private fun contactIdAndEmailsOutputData(apiResponse: ContactResponse): Data {
        val contactEmails: List<ContactEmail> = apiResponse.responses.flatMap {
            it.response.contact.emails ?: emptyList()
        }

        return workDataOf(
            KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID to apiResponse.contactId,
            KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON to Json.encodeToString(
                ListSerializer(ContactEmail.serializer()),
                contactEmails
            )
        )
    }

    private fun failureWithError(error: CreateContactWorkerErrors): Result {
        val errorData = workDataOf(KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM to error.name)
        return Result.failure(errorData)
    }

    private fun buildCreateContactRequestBody(): CreateContact {
        val encryptedDataParamPath = getInputEncryptedData()
        val signedDataParam = getInputSignedData()

        val encryptedDataParam = File(encryptedDataParamPath).source().buffer().readUtf8()

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

    private fun getInputSignedData() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_SIGNED_DATA) ?: ""

    private fun getInputEncryptedData() = inputData.getString(KEY_INPUT_DATA_CREATE_CONTACT_ENCRYPTED_DATA_PATH) ?: ""

    enum class CreateContactWorkerErrors {
        ServerError,
        ContactAlreadyExistsError,
        InvalidEmailError,
        DuplicatedEmailError
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(encryptedContactDataPath: String, signedContactData: String): LiveData<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val createContactRequest = OneTimeWorkRequestBuilder<CreateContactWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_DATA_CREATE_CONTACT_ENCRYPTED_DATA_PATH to encryptedContactDataPath,
                        KEY_INPUT_DATA_CREATE_CONTACT_SIGNED_DATA to signedContactData
                    )
                ).build()

            workManager.enqueue(createContactRequest)
            return workManager.getWorkInfoByIdLiveData(createContactRequest.id)
        }

    }
}
