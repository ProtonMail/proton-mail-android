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

package ch.protonmail.android.mailbox.data.remote.worker

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
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import timber.log.Timber
import javax.inject.Inject

const val KEY_POST_WORKER_MESSAGE_ID = "KeyPostWorkerMessageId"
const val KEY_POST_WORKER_LOCATION_ID = "KeyPostWorkerLocationId"
const val KEY_POST_WORKER_CUSTOM_LOCATION_ID = "KeyPostWorkerCustomLocationId"
private const val MAX_RUN_ATTEMPTS = 3

@HiltWorker
class PostToLocationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val protonMailApiManager: ProtonMailApiManager
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val ids = inputData.getStringArray(KEY_POST_WORKER_MESSAGE_ID)
        val locationId = inputData.getInt(KEY_POST_WORKER_LOCATION_ID, -1)
        val customLocationId = inputData.getString(KEY_POST_WORKER_CUSTOM_LOCATION_ID)

        if (ids.isNullOrEmpty() || locationId < 0 && customLocationId.isNullOrEmpty()) {
            return Result.failure(
                workDataOf(KEY_LABEL_WORKER_ERROR_DESCRIPTION to "Input data is not complete")
            )
        }

        Timber.v("PostToLocationWorker location: $locationId, ids: $ids")

        val locationIdString = if (!customLocationId.isNullOrEmpty()) {
            customLocationId
        } else {
            locationId.toString()
        }

        return runCatching {
            protonMailApiManager.labelMessages(
                IDList(
                    locationIdString,
                    ids.asList()
                )
            )
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                if (runAttemptCount > MAX_RUN_ATTEMPTS) {
                    Result.failure(
                        workDataOf(KEY_LABEL_WORKER_ERROR_DESCRIPTION to "Run attempts exceeded the limit")
                    )
                } else {
                    Result.retry()
                }
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            ids: List<String>,
            newLocation: Constants.MessageLocationType? = null,
            newCustomLocation: String? = null
        ): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_POST_WORKER_MESSAGE_ID to ids.toTypedArray(),
                KEY_POST_WORKER_LOCATION_ID to newLocation?.messageLocationTypeValue,
                KEY_POST_WORKER_CUSTOM_LOCATION_ID to newCustomLocation,
            )

            val request = OneTimeWorkRequestBuilder<PostToLocationWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            return workManager.enqueue(request)
        }
    }
}
