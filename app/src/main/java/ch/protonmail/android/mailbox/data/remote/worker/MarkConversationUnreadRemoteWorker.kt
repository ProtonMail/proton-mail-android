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
import ch.protonmail.android.mailbox.data.remote.model.ConversationIdsRequestBody
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import javax.inject.Inject

const val KEY_MARK_UNREAD_WORKER_CONVERSATION_IDS = "ConversationIds"
const val KEY_MARK_UNREAD_WORKER_UNDO_TOKEN = "UndoToken"
const val KEY_MARK_UNREAD_WORKER_VALID_UNTIL = "ValidUntil"
const val KEY_MARK_UNREAD_WORKER_ERROR_DESCRIPTION = "ErrorDescription"

/**
 * A worker that handles marking a conversation as unread
 */
@HiltWorker
class MarkConversationsUnreadRemoteWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val protonMailApiManager: ProtonMailApiManager
) : CoroutineWorker(context, workerParameters) {

    override suspend fun doWork(): Result {
        val conversationIds = inputData.getStringArray(KEY_MARK_UNREAD_WORKER_CONVERSATION_IDS)
            ?: return Result.failure(
                workDataOf(KEY_MARK_UNREAD_WORKER_ERROR_DESCRIPTION to "Conversation ids list is null")
            )

        val requestBody = ConversationIdsRequestBody(conversationIds.asList())

        return runCatching {
            protonMailApiManager.markConversationsUnread(requestBody)
        }.fold(
            onSuccess = { response ->
                Result.success(
                    workDataOf(
                        KEY_MARK_UNREAD_WORKER_UNDO_TOKEN to response.undoToken?.token,
                        KEY_MARK_UNREAD_WORKER_VALID_UNTIL to response.undoToken?.validUntil
                    )
                )
            },
            onFailure = {
                Result.retry()
            }
        )
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(conversationIds: List<String>): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val data = workDataOf(KEY_MARK_UNREAD_WORKER_CONVERSATION_IDS to conversationIds.toTypedArray())

            val request = OneTimeWorkRequestBuilder<MarkConversationsUnreadRemoteWorker>()
                .setConstraints(constraints)
                .setInputData(data)
                .build()

            return workManager.enqueue(request)
        }
    }
}
