/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.labels.data.remote.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.worker.KEY_WORKER_ERROR_DESCRIPTION
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.network.domain.ApiResult
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_INPUT_DATA_LABEL_IDS = "KeyInputDataLabelIds"
private const val WORKER_TAG = "DeleteLabelWorkerTag"

/**
 * Work Manager Worker responsible for labels removal.
 *
 *  InputData has to contain non-null values for:
 *  labelId
 *
 * @see androidx.work.WorkManager
 */
@HiltWorker
class DeleteLabelsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    private val dispatchers: DispatcherProvider,
    private val accountManager: AccountManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {

        val labelIds = inputData.getStringArray(KEY_INPUT_DATA_LABEL_IDS) ?: emptyArray()

        // skip empty input
        if (labelIds.isEmpty()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty label id")
            )
        }

        return withContext(dispatchers.Io) {
            var result = Result.success()
            val userId = requireNotNull(accountManager.getPrimaryUserId().first())

            labelIds.forEach { labelId ->
                val apiResponse = api.deleteLabel(userId, labelId)
                Timber.v("Deleting label $labelId response $apiResponse")
                if (apiResponse is ApiResult.Error.Http) {
                    result = Result.failure(
                        workDataOf(
                            KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${apiResponse.proton?.code}"
                        )
                    )
                } else if (apiResponse is ApiResult.Error) {
                    result = Result.failure()
                }
            }
            result
        }
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {
        fun enqueue(labelIds: List<LabelId>): LiveData<WorkInfo> {
            val labelIdsArray = labelIds.map { it.id }.toTypedArray()
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<DeleteLabelsWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_INPUT_DATA_LABEL_IDS to labelIdsArray))
                .addTag(WORKER_TAG)
                .build()
            workManager.enqueue(workRequest)

            return workManager.getWorkInfoByIdLiveData(workRequest.id)
        }
    }
}
