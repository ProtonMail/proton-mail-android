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
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.data.local.ContactDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber

internal const val KEY_INPUT_DATA_CONTACT_GROUP_ID = "KeyInputDataContactGroupId"
internal const val KEY_INPUT_DATA_CONTACT_GROUP_NAME = "KeyInputDataContactGroupName"
internal const val KEY_INPUT_DATA_MEMBERS_LIST = "KeyInputDataMembersList"

/**
 * Work Manager Worker responsible for contact groups members removal.
 *
 *  InputData has to contain non-null values for:
 *  - contactGroupId
 *  - contactGroupName
 *  - membersList
 *
 * @see androidx.work.WorkManager
 */
@HiltWorker
class RemoveMembersFromContactGroupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    private val contactDao: ContactDao,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        var id = inputData.getString(KEY_INPUT_DATA_CONTACT_GROUP_ID) ?: ""
        val contactGroupName = inputData.getString(KEY_INPUT_DATA_CONTACT_GROUP_NAME)
        val membersList = inputData.getStringArray(KEY_INPUT_DATA_MEMBERS_LIST) ?: emptyArray()

        Timber.v("Remove group Members $membersList")

        if (id.isEmpty() && contactGroupName?.isNotEmpty() == true) {
            val contactLabel = withContext(dispatchers.Io) {
                contactDao.findContactGroupByName(contactGroupName)
            }
            id = contactLabel?.ID ?: ""
        }

        if (id.isEmpty()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty contacts group id")
            )
        }

        if (membersList.isEmpty()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty members list")
            )
        }

        val labelContactsBody = LabelContactsBody(id, membersList.asList())

        // make network call
        return withContext(dispatchers.Io) {
            api.unlabelContactEmails(labelContactsBody)
            Result.success()
        }
    }

    class Enqueuer(private val workManager: WorkManager) {
        fun enqueue(
            contactGroupId: String,
            contactGroupName: String,
            membersList: List<String>
        ): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<RemoveMembersFromContactGroupWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_DATA_CONTACT_GROUP_ID to contactGroupId,
                        KEY_INPUT_DATA_CONTACT_GROUP_NAME to contactGroupName,
                        KEY_INPUT_DATA_MEMBERS_LIST to membersList
                    )
                )
                .build()
            return workManager.enqueue(workRequest)
        }
    }

}
