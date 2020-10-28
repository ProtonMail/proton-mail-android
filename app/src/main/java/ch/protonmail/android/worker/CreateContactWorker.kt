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
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_INPUT_DATA_CREATE_CONTACT_DATA_DB_ID = "keyCreateContactInputDataContactDataDBId"
internal const val KEY_INPUT_DATA_CREATE_CONTACT_EMAILS_SERIALISED = "keyCreateContactInputDataContactEmails"

private const val INVALID_CONTACT_DATA_DB_ID = -1L

class CreateContactWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val contactsDao: ContactsDao,
    private val gson: Gson
) : CoroutineWorker(context, params) {


    override suspend fun doWork(): Result {
        val contactData = getContactData() ?: return Result.failure()
        val contactEmails = getContactEmails() ?: return Result.failure()

        return Result.success()
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
