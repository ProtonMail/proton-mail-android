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

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.work.Data
import androidx.work.WorkInfo
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.contacts.ContactIdGenerator
import ch.protonmail.android.contacts.details.ContactDetailsRepository
import ch.protonmail.android.utils.extensions.filter
import ch.protonmail.android.worker.CreateContactWorker.CreateContactWorkerErrors
import ch.protonmail.android.worker.CreateContactWorker.Enqueuer
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_EMAILS_JSON
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_RESULT_ERROR_ENUM
import ch.protonmail.android.worker.KEY_OUTPUT_DATA_CREATE_CONTACT_SERVER_ID
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import java.lang.reflect.Type
import javax.inject.Inject

class CreateContact @Inject constructor(
    private val dispatcherProvider: DispatcherProvider,
    private val contactsRepository: ContactDetailsRepository,
    private val createContactScheduler: Enqueuer,
    private val gson: Gson,
    private val contactIdGenerator: ContactIdGenerator
) {

    suspend operator fun invoke(
        contactName: String,
        contactEmails: List<ContactEmail>,
        encryptedContactData: String,
        signedContactData: String
    ): LiveData<CreateContactResult> {

        return withContext(dispatcherProvider.Io) {
            val contactData = ContactData(contactIdGenerator.generateRandomId(), contactName)
            val contactDataDbId = contactsRepository.saveContactData(contactData)
            contactData.dbId = contactDataDbId

            contactEmails.forEach { it.contactId = contactData.contactId }
            contactsRepository.saveContactEmails(contactEmails)

            createContactScheduler.enqueue(encryptedContactData, signedContactData)
                .filter { it?.state?.isFinished == true || it?.state == WorkInfo.State.ENQUEUED }
                .switchMap { workInfo: WorkInfo? ->
                    liveData(dispatcherProvider.Io) {
                        if (workInfo?.state == WorkInfo.State.ENQUEUED) {
                            emit(CreateContactResult.OnlineContactCreationPending)
                        } else {
                            workInfo?.let { emit(handleWorkResult(workInfo, contactData)) }
                        }
                    }
                }
        }

    }

    private suspend fun handleWorkResult(workInfo: WorkInfo, contactData: ContactData): CreateContactResult {
        val workSucceeded = workInfo.state == WorkInfo.State.SUCCEEDED

        if (workSucceeded) {
            updateLocalContactData(workInfo.outputData, contactData)
            return CreateContactResult.Success
        }

        val createContactError = workerErrorToCreateContactResult(workInfo)
        if (createContactError != CreateContactResult.GenericError) {
            contactsRepository.deleteContactData(contactData)
        }
        return createContactError
    }

    private fun workerErrorToCreateContactResult(workInfo: WorkInfo) =
        when (workerErrorEnum(workInfo)) {
            CreateContactWorkerErrors.ContactAlreadyExistsError -> CreateContactResult.ContactAlreadyExistsError
            CreateContactWorkerErrors.InvalidEmailError -> CreateContactResult.InvalidEmailError
            CreateContactWorkerErrors.DuplicatedEmailError -> CreateContactResult.DuplicatedEmailError
            else -> CreateContactResult.GenericError
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
            val serverEmails = gson.fromJson<List<ContactEmail>>(it, emailsListGsonType())
            contactsRepository.updateAllContactEmails(contactData.contactId, serverEmails)
        }
    }

    private fun emailsListGsonType(): Type = TypeToken.getParameterized(List::class.java, ContactEmail::class.java).type


    sealed class CreateContactResult {
        object Success : CreateContactResult()
        object GenericError : CreateContactResult()
        object ContactAlreadyExistsError : CreateContactResult()
        object InvalidEmailError : CreateContactResult()
        object DuplicatedEmailError : CreateContactResult()
        object OnlineContactCreationPending : CreateContactResult()
    }
}
