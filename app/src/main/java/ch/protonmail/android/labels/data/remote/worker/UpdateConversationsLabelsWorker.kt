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
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.Operation
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.labels.domain.usecase.UpdateConversationsLabels
import ch.protonmail.android.mailbox.domain.model.ConversationsActionResult
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

const val KEY_UPDATE_LABELS_CONVERSATION_IDS = "ConversationIds"
const val KEY_UPDATE_LABELS_USER_ID = "UserId"
const val KEY_UPDATE_LABELS_SELECTED_LABELS = "SelectedLabels"
const val KEY_UPDATE_LABELS_UNSELECTED_LABELS = "UnselectedLabels"
const val KEY_UPDATE_LABELS_ERROR_DESCRIPTION = "ErrorDescription"

/**
 * This worker calls the UpdateConversationsLabels use case
 */
@HiltWorker
class UpdateConversationsLabelsWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val updateConversationsLabels: UpdateConversationsLabels
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val conversationIds = inputData.getStringArray(KEY_UPDATE_LABELS_CONVERSATION_IDS)?.toList()
        val userId = inputData.getString(KEY_UPDATE_LABELS_USER_ID)
        val selectedLabels = inputData.getStringArray(KEY_UPDATE_LABELS_SELECTED_LABELS)?.toList()

        if (conversationIds.isNullOrEmpty() || userId.isNullOrEmpty() ||
            selectedLabels == null
        ) {
            return Result.failure(
                workDataOf(
                    KEY_UPDATE_LABELS_ERROR_DESCRIPTION to "Input data is incomplete"
                )
            )
        }

        val result = updateConversationsLabels(
            conversationIds,
            UserId(userId),
            selectedLabels
        )
        if (result is ConversationsActionResult.Error) {
            return Result.failure(
                workDataOf(
                    KEY_UPDATE_LABELS_ERROR_DESCRIPTION to "Could not complete the action"
                )
            )
        }
        return Result.success()
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            conversationIds: List<String>,
            userId: UserId,
            selectedLabels: List<String>,
        ): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_UPDATE_LABELS_CONVERSATION_IDS to conversationIds.toTypedArray(),
                KEY_UPDATE_LABELS_USER_ID to userId.id,
                KEY_UPDATE_LABELS_SELECTED_LABELS to selectedLabels.toTypedArray(),
            )

            val request = OneTimeWorkRequestBuilder<UpdateConversationsLabelsWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            return workManager.enqueue(request)
        }
    }
}
