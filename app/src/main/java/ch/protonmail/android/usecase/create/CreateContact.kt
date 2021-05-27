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

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.work.Data
import androidx.work.WorkInfo
import ch.protonmail.android.contacts.ContactIdGenerator
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.utils.FileHelper
import ch.protonmail.android.utils.extensions.filter
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerErrors
import ch.protonmail.android.worker.CreateContactWorker.Enqueuer
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import me.proton.core.util.kotlin.DispatcherProvider
import java.io.File
import javax.inject.Inject

internal const val VCARD_TEMP_FILE_NAME = "temp_card.vcard"

class CreateContact @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val contactsRepository: ContactDetailsRepository,
    private val createContactScheduler: Enqueuer,
    private val contactIdGenerator: ContactIdGenerator,
    private val connectivityManager: NetworkConnectivityManager,
    private val context: Context,
    private val fileHelper: FileHelper
) {

    suspend operator fun invoke(
        contactName: String,
        contactEmails: List<ContactEmail>,
        encryptedContactData: String,
        signedContactData: String
    ): LiveData<Result> {

        return withContext(dispatcherProvider.Io) {
            val contactData = ContactData(contactIdGenerator.generateRandomId(), contactName)
            val contactDataDbId = contactsRepository.saveContactData(contactData)
            contactData.dbId = contactDataDbId

            contactEmails.forEach { it.contactId = contactData.contactId }
            contactsRepository.saveContactEmails(contactEmails)

            // we have to save encrypted vCard to a file due to worker arguments size limitations
            // they are limited to 10240 bytes, if it is exceeded we get
            // IllegalStateException: Data cannot occupy more than 10240 bytes when serialized
            // but if we add an image to a contact we exceed this value
            // therefore we will just pass to the worker a cached file path
            val vCardFilePath = context.cacheDir.toString() + File.separator + VCARD_TEMP_FILE_NAME
            fileHelper.saveStringToFile(vCardFilePath, encryptedContactData)

            createContactScheduler.enqueue(vCardFilePath, signedContactData)
                .filter { it?.state?.isFinished == true || it?.state == WorkInfo.State.ENQUEUED }
                .switchMap { workInfo: WorkInfo? ->
                    liveData(dispatcherProvider.Io) {
                        if (workInfo?.state == WorkInfo.State.ENQUEUED) {
                            if (!connectivityManager.isInternetConnectionPossible()) {
                                emit(Result.OnlineContactCreationPending)
                            }
                        } else {
                            workInfo?.let { emit(handleWorkResult(workInfo, contactData)) }
                        }
                    }
                }
        }

    }

    private suspend fun handleWorkResult(workInfo: WorkInfo, contactData: ContactData): Result {
        val workSucceeded = workInfo.state == WorkInfo.State.SUCCEEDED

        if (workSucceeded) {
            updateLocalContactData(workInfo.outputData, contactData)
            return Result.Success
        }

        val createContactError = workerErrorToCreateContactResult(workInfo)
        if (createContactError != Result.GenericError) {
            contactsRepository.deleteContactData(contactData)
        }
        return createContactError
    }

    private fun workerErrorToCreateContactResult(workInfo: WorkInfo) =
        when (workerErrorEnum(workInfo)) {
            CreateContactWorkerErrors.ContactAlreadyExistsError -> Result.ContactAlreadyExistsError
            CreateContactWorkerErrors.InvalidEmailError -> Result.InvalidEmailError
            CreateContactWorkerErrors.DuplicatedEmailError -> Result.DuplicatedEmailError
            else -> Result.GenericError
        }

    private fun workerErrorEnum(workInfo: WorkInfo): CreateContactWorkerErrors {
        val errorString = workInfo.outputData.getString(KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM)
        errorString?.let {
            return CreateContactWorkerErrors.valueOf(errorString)
        }
        return CreateContactWorkerErrors.ServerError
    }

    private suspend fun updateLocalContactData(outputData: Data, contactData: ContactData) {
        val contactServerId = outputData.getString(KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID)
        contactServerId?.let {
            contactsRepository.updateContactDataWithServerId(contactData, it)
        }

        val emailsJson = outputData.getString(KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON)
        emailsJson?.let {
            val serverEmails = Json.decodeFromString(
                ListSerializer(ContactEmail.serializer()),
                emailsJson
            )
            contactsRepository.updateAllContactEmails(contactData.contactId, serverEmails)
        }
    }


    sealed class Result {
        object Success : Result()
        object GenericError : Result()
        object ContactAlreadyExistsError : Result()
        object InvalidEmailError : Result()
        object DuplicatedEmailError : Result()
        object OnlineContactCreationPending : Result()
    }
}
