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
import ch.protonmail.android.api.interceptors.UserIdTag
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

const val KEY_EMPTY_FOLDER_LABEL_ID = "LabelId"
const val KEY_EMPTY_FOLDER_USER_ID = "UserId"
const val KEY_EMPTY_FOLDER_ERROR_DESCRIPTION = "ErrorDescription"
const val EMPTY_FOLDER_MAX_RUN_ATTEMPTS = 5

/**
 * A worker that handles the API call for emptying a folder
 */
@HiltWorker
class EmptyFolderRemoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val protonMailApiManager: ProtonMailApiManager
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val labelId = inputData.getString(KEY_EMPTY_FOLDER_LABEL_ID)
        val userId = inputData.getString(KEY_EMPTY_FOLDER_USER_ID)

        if (labelId.isNullOrEmpty() || userId.isNullOrEmpty()) {
            return Result.failure(
                workDataOf(KEY_EMPTY_FOLDER_ERROR_DESCRIPTION to "Input data is not complete")
            )
        }

        return runCatching {
            protonMailApiManager.emptyFolder(UserIdTag(UserId(userId)), labelId)
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                if (runAttemptCount > EMPTY_FOLDER_MAX_RUN_ATTEMPTS) {
                    Result.failure(
                        workDataOf(KEY_EMPTY_FOLDER_ERROR_DESCRIPTION to "Run attempts exceeded the limit")
                    )
                } else {
                    Result.retry()
                }
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(userId: UserId, labelId: String): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_EMPTY_FOLDER_LABEL_ID to labelId,
                KEY_EMPTY_FOLDER_USER_ID to userId.id
            )

            val request = OneTimeWorkRequestBuilder<EmptyFolderRemoteWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            return workManager.enqueue(request)
        }
    }
}
