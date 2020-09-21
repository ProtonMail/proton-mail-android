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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.extensions.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_WORKER_ERROR_DESCRIPTION = "KeyWorkerErrorDescription"
internal const val KEY_INPUT_VALID_MESSAGES_IDS = "KeyInputValidMessageIds"

/**
 * Work Manager Worker responsible for deleting messages.
 *
 *  InputData has to contain non-null values for:
 *  labelId
 *
 * @see androidx.work.WorkManager
 */
class DeleteMessageWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    internal lateinit var api: ProtonMailApiManager

    init {
        context.app.appComponent.inject(this)
    }

    override suspend fun doWork(): Result {

        val validMessageIdList = inputData.getStringArray(KEY_INPUT_VALID_MESSAGES_IDS)
            ?: emptyArray()

        // skip empty input
        if (validMessageIdList.isEmpty()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty valid messages list")
            )
        }

        return withContext(Dispatchers.IO) {
            // delete messages on remote
            val response = api.deleteMessage(IDList(validMessageIdList.toList()))
            if (response.code == Constants.RESPONSE_CODE_OK ||
                response.code == Constants.RESPONSE_CODE_MULTIPLE_OK
            ) {
                Result.success()
            } else {
                Timber.v("ApiException failure response code ${response.code}")
                Result.failure(
                    workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${response.code}")
                )
            }
        }
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {
        fun enqueue(messageIds: List<String>): Operation {
            val networkConstraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val networkWorkRequest = OneTimeWorkRequestBuilder<DeleteMessageWorker>()
                .setConstraints(networkConstraints)
                .setInputData(workDataOf(KEY_INPUT_VALID_MESSAGES_IDS to messageIds.toTypedArray()))
                .build()
            Timber.v("Scheduling delete messages worker for ${messageIds.size} message(s)")
            return workManager.enqueue(networkWorkRequest)
        }
    }
}
