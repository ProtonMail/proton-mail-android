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
import ch.protonmail.android.api.ProtonMailApi
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.data.local.CounterRepository
import ch.protonmail.android.worker.KEY_WORKER_ERROR_DESCRIPTION
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import timber.log.Timber
import java.util.concurrent.CancellationException
import javax.inject.Inject

internal const val KEY_INPUT_DATA_MESSAGES_IDS = "KeyInputDataMessagesIds"

@HiltWorker
internal class ApplyMessageLabelWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val accountManager: AccountManager,
    private val counterRepository: CounterRepository,
    private val protonMailApi: ProtonMailApi
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val messageIds = requireNotNull(inputData.getStringArray(KEY_INPUT_DATA_MESSAGES_IDS)) {
            "Message IDs are missing, cannot apply labels without it!"
        }
        val labelId = inputData.getString(KEY_INPUT_DATA_LABEL_ID)
        val userId = accountManager.getPrimaryUserId().filterNotNull().first()
        Timber.v("Apply label $labelId for messages: $messageIds")

        if (messageIds.isEmpty() || labelId == null) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty label id or message ids")
            )
        }

        val idList = IDList(labelId, messageIds.asList())

        return runCatching {
            protonMailApi.labelMessages(idList)
        }.fold(
            onSuccess = {
                counterRepository.updateMessageLabelCounter(
                    userId,
                    labelId,
                    messageIds.asList(),
                    CounterRepository.CounterModificationMethod.INCREMENT
                )
                Result.success()
            },
            onFailure = { throwable ->
                if (throwable is CancellationException) {
                    throw throwable
                }
                Result.failure(
                    workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${throwable.message}")
                )
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            messageIds: List<String>,
            labelId: String
        ): LiveData<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<ApplyMessageLabelWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_DATA_LABEL_ID to labelId,
                        KEY_INPUT_DATA_MESSAGES_IDS to messageIds.toTypedArray()
                    )
                )
                .build()
            workManager.enqueue(workRequest)

            return workManager.getWorkInfoByIdLiveData(workRequest.id)
        }
    }

}
