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
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.extensions.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_INPUT_DATA_LABEL_ID = "KeyInputDataLabelId"
internal const val KEY_WORKER_ERROR_DESCRIPTION = "KeyWorkerErrorDescription"
private const val WORKER_TAG = "DeleteLabelWorkerTag"

/**
 * Work Manager Worker responsible for labels removal.
 *
 *  InputData has to contain non-null values for:
 *  labelId
 *
 * @see androidx.work.WorkManager
 */
class DeleteLabelWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    internal lateinit var api: ProtonMailApiManager

    @Inject
    internal lateinit var contactsDatabase: ContactsDatabase

    @Inject
    internal lateinit var messagesDatabase: MessagesDao

    init {
        context.app.appComponent.inject(this)
    }

    override suspend fun doWork(): Result {

        val labelId = inputData.getString(KEY_INPUT_DATA_LABEL_ID) ?: ""

        // skip empty input
        if (labelId.isEmpty()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty label id")
            )
        }

        Timber.v("Deleting label $labelId")
        return withContext(Dispatchers.IO) {
            val responseBody: ResponseBody = api.deleteLabel(labelId)
            if (responseBody.code == Constants.RESPONSE_CODE_OK) {
                updateDb(labelId)
                Result.success()
            } else {
                Result.failure()
            }
        }
    }

    private fun updateDb(labelId: String) {
        val contactLabel = contactsDatabase.findContactGroupById(labelId)
        contactLabel?.let {
            contactsDatabase.deleteContactGroup(it)
        }
        messagesDatabase.deleteLabelById(labelId)
    }

    class Enqueuer(private val workManager: WorkManager) {
        fun enqueue(labelId: String): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<DeleteLabelWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_INPUT_DATA_LABEL_ID to labelId))
                .addTag(WORKER_TAG)
                .build()
            return workManager.enqueue(workRequest)
        }

        fun getWorkStatusLiveData() = workManager.getWorkInfosByTagLiveData(WORKER_TAG)
    }
}
