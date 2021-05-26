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
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.ContactDatabase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber

internal const val KEY_INPUT_DATA_CONTACT_IDS = "KeyInputDataContactDds"

/**
 * Work Manager Worker responsible for contactIs removal.
 *
 *  InputData has to contain non-null values for:
 *  contactIds
 *
 * @see androidx.work.WorkManager
 */
@HiltWorker
class DeleteContactWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    private val contactDao: ContactDao,
    private val contactDatabase: ContactDatabase,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val contactIds = inputData.getStringArray(KEY_INPUT_DATA_CONTACT_IDS)
            ?: emptyArray()

        // skip empty input
        if (contactIds.isEmpty()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty contacts list")
            )
        }

        Timber.v("Deleting ${contactIds.size} contacts")

        return withContext(dispatchers.Io) {
            // clean remote store
            val response = api.deleteContact(IDList(contactIds.toList()))

            if (
                response.code == Constants.RESPONSE_CODE_OK ||
                response.code == Constants.RESPONSE_CODE_MULTIPLE_OK
            ) {
                updateDb(contactIds)
                Result.success()
            } else {
                Result.failure(
                    workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${response.code}")
                )
            }
        }
    }

    private fun updateDb(contactIds: Array<String>) {
        for (contactId in contactIds) {
            val contactData = contactDao.findContactDataById(contactId)

            contactData?.let { contact ->
                contactDatabase.runInTransaction {
                    contact.contactId?.let {
                        val contactEmails = contactDao.findContactEmailsByContactIdBlocking(it)
                        contactDao.deleteAllContactsEmails(contactEmails)
                    }
                    contactDao.deleteContactData(contact)
                }
            }
        }
    }

    class Enqueuer(private val workManager: WorkManager) {
        fun enqueue(contactIds: List<String>): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val deleteContactWorkRequest = OneTimeWorkRequestBuilder<DeleteContactWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_INPUT_DATA_CONTACT_IDS to contactIds.toTypedArray()))
                .build()
            return workManager.enqueue(deleteContactWorkRequest)
        }
    }

}
