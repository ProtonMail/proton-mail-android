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
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

const val KEY_UNLABEL_WORKER_CONVERSATION_IDS = "ConversationIds"
const val KEY_UNLABEL_WORKER_LABEL_ID = "LabelId"
const val KEY_UNLABEL_WORKER_USER_ID = "UserId"
const val KEY_UNLABEL_WORKER_ERROR_DESCRIPTION = "ErrorDescription"

/**
 * A worker that handles unlabeling conversations
 */
@HiltWorker
class UnlabelConversationsRemoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val protonMailApiManager: ProtonMailApiManager
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val conversationIds = inputData.getStringArray(KEY_UNLABEL_WORKER_CONVERSATION_IDS)
        val labelId = inputData.getString(KEY_UNLABEL_WORKER_LABEL_ID)
        val userId = inputData.getString(KEY_UNLABEL_WORKER_USER_ID)

        if (conversationIds.isNullOrEmpty() || labelId.isNullOrEmpty() || userId.isNullOrEmpty()) {
            return Result.failure(
                workDataOf(KEY_UNLABEL_WORKER_ERROR_DESCRIPTION to "Input data is not complete")
            )
        }

        val requestBody = ConversationIdsRequestBody(labelId, conversationIds.asList())

        return runCatching {
            protonMailApiManager.unlabelConversations(requestBody, Id(userId))
        }.fold(
            onSuccess = {
                Result.success()
            },
            onFailure = {
                Result.retry()
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(conversationIds: List<String>, labelId: String, userId: UserId): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(
                KEY_UNLABEL_WORKER_CONVERSATION_IDS to conversationIds.toTypedArray(),
                KEY_UNLABEL_WORKER_LABEL_ID to labelId,
                KEY_UNLABEL_WORKER_USER_ID to userId.id
            )

            val request = OneTimeWorkRequestBuilder<UnlabelConversationsRemoteWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            return workManager.enqueue(request)
        }
    }
}
