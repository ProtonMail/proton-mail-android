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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.api.segments.RESPONSE_CODE_INVALID_ID
import ch.protonmail.android.attachments.KEY_INPUT_DATA_ATTACHMENT_ID_STRING
import ch.protonmail.android.core.Constants
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Named

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
class DeleteAttachmentWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    @Named("messages") var messagesDatabase: MessagesDatabase,
    private val dispatchers: DispatcherProvider
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val attachmentId = inputData.getString(KEY_INPUT_DATA_ATTACHMENT_ID_STRING) ?: ""

        // skip empty attachment ids
        if (attachmentId.isBlank()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot delete attachment with an empty id")
            )
        }

        Timber.v("Delete attachmentId ID: $attachmentId")

        return withContext(dispatchers.Io) {
            val response = api.deleteAttachment(attachmentId)

            if (response.code == Constants.RESPONSE_CODE_OK ||
                response.code == RESPONSE_CODE_INVALID_ID
            ) {
                Timber.v("Attachment ID: $attachmentId deleted on remote")
                val attachment = messagesDatabase.findAttachmentById(attachmentId)
                attachment?.let {
                    messagesDatabase.deleteAttachment(it)
                    return@withContext Result.success()
                }
            }
            Timber.i("Delete Attachment on remote failure response: ${response.code} ${response.error}")
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
