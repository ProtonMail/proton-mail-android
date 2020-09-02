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
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.api.segments.RESPONSE_CODE_ATTACHMENT_DELETE_ID_INVALID
import ch.protonmail.android.attachments.KEY_INPUT_DATA_ATTACHMENT_ID_STRING
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.extensions.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_WORKER_ERROR_DESCRIPTION = "KeyWorkerErrorDescription"

/**
 * Work Manager Worker responsible for attachments removal through the API and from
 * the local database.
 *
 *  InputData has to contain non-null values for:
 *  attachmentId
 *
 * @see androidx.work.WorkManager
 */
class DeleteAttachmentWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    @Inject
    internal lateinit var api: ProtonMailApiManager

    @Inject
    internal lateinit var messagesDatabase: MessagesDao

    init {
        context.app.appComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        val attachmentId = inputData.getString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING) ?: ""

        // skip empty attachment ids
        if (attachmentId.isBlank()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot delete attachment with an empty id")
            )
        }

        Timber.v("Delete attachmentId ID: $attachmentId")

        return withContext(Dispatchers.IO) {
            val response = api.deleteAttachment(attachmentId)

            if (response.code == Constants.RESPONSE_CODE_OK ||
                response.code == RESPONSE_CODE_ATTACHMENT_DELETE_ID_INVALID
            ) {
                val attachment = messagesDatabase.findAttachmentById(attachmentId)
                attachment?.let {
                    messagesDatabase.deleteAttachment(it)
                    return@withContext Result.success()
                }
            }
            Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${response.code}")
            )
        }
    }

    class Enqueuer(private val workManager: WorkManager) {
        fun enqueue(attachmentId: String): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val deleteAttachmentWorkRequest = OneTimeWorkRequestBuilder<DeleteAttachmentWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_INPUT_DATA_ATTACHMENT_ID_STRING to attachmentId))
                .build()
            return workManager.enqueue(deleteAttachmentWorkRequest)
        }
    }

}
