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
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import ch.protonmail.android.utils.extensions.app
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import javax.inject.Inject

private const val KEY_INPUT_DATA_CONTACT_IDS = "KEY_INPUT_DATA_CONTACT_IDS"
internal const val KEY_WORKER_ERROR_DESCRIPTION = "KEY_WORKER_ERROR_DESCRIPTION"

/**
 * Work Manager Worker responsible for contactIs removal.
 *
 *  InputData has to contain non-null values for:
 *  contactIds
 *
 * @see androidx.work.WorkManager
 */
class DeleteContactWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    @Inject
    internal lateinit var api: ProtonMailApiManager

    @Inject
    internal lateinit var contactsDatabaseFactory: ContactsDatabaseFactory

    @Inject
    internal lateinit var contactsDatabase: ContactsDatabase

    init {
        context.app.appComponent.inject(this)
    }

    override suspend fun doWork(): Result = coroutineScope {

        val contactIds = inputData.getStringArray(KEY_INPUT_DATA_CONTACT_IDS)
            ?: emptyArray()

        // skip empty input
        if (contactIds.isEmpty()) {
            return@coroutineScope Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty contacts list")
            )
        }

        Timber.v("Deleting ${contactIds.size} contacts")

        updateDb(contactIds)

        // clean remote store
        api.deleteContact(IDList(contactIds.toList()))

        Result.success()
    }

    private fun updateDb(contactIds: Array<String>) {
        for (contactId in contactIds) {
            val contactData = contactsDatabase.findContactDataById(contactId)

            contactData?.let { contact ->
                contactsDatabaseFactory.runInTransaction {
                    contact.contactId?.let {
                        val contactEmails = contactsDatabase.findContactEmailsByContactId(it)
                        contactsDatabase.deleteAllContactsEmails(contactEmails)
                    }
                    contactsDatabase.deleteContactData(contact)
                }
            }
        }
    }

    class Enqueuer {
        fun enqueue(workManager: WorkManager, contactIds: List<String>): Operation {
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
