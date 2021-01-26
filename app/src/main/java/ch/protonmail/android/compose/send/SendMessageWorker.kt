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

package ch.protonmail.android.compose.send

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.lifecycle.asFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.extensions.serialize
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val KEY_INPUT_SEND_MESSAGE_MSG_DB_ID = "keySendMessageMessageDbId"
internal const val KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS = "keySendMessageAttachmentIds"
internal const val KEY_INPUT_SEND_MESSAGE_MESSAGE_ID = "keySendMessageMessageLocalId"
internal const val KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID = "keySendMessageMessageParentId"
internal const val KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_JSON = "keySendMessageMessageActionTypeSerialized"
internal const val KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID = "keySendMessagePreviousSenderAddressId"

class SendMessageWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        TODO("Not yet implemented")
    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {

        fun enqueue(
            message: Message,
            attachmentIds: List<String>,
            parentId: String?,
            actionType: Constants.MessageActionType,
            previousSenderAddressId: String
        ): Flow<WorkInfo?> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val sendMessageRequest = OneTimeWorkRequestBuilder<SendMessageWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_SEND_MESSAGE_MSG_DB_ID to message.dbId,
                        KEY_INPUT_SEND_MESSAGE_MESSAGE_ID to message.messageId,
                        KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS to attachmentIds.toTypedArray(),
                        KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID to parentId,
                        KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_JSON to actionType.serialize(),
                        KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID to previousSenderAddressId
                    )
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 2 * TEN_SECONDS, TimeUnit.SECONDS)
                .build()

            workManager.enqueueUniqueWork(
                requireNotNull(message.messageId),
                ExistingWorkPolicy.REPLACE,
                sendMessageRequest
            )
            return workManager.getWorkInfoByIdLiveData(sendMessageRequest.id).asFlow()
        }
    }

}
