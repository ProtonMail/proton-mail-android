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
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.factories.SendPreferencesFactory
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.segments.TEN_SECONDS
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.compose.SaveDraft
import ch.protonmail.android.usecase.compose.SaveDraftResult
import ch.protonmail.android.utils.extensions.deserialize
import ch.protonmail.android.utils.extensions.serialize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal const val KEY_INPUT_SEND_MESSAGE_MSG_DB_ID = "keySendMessageMessageDbId"
internal const val KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS = "keySendMessageAttachmentIds"
internal const val KEY_INPUT_SEND_MESSAGE_MESSAGE_ID = "keySendMessageMessageLocalId"
internal const val KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID = "keySendMessageMessageParentId"
internal const val KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_SERIALIZED = "keySendMessageMessageActionTypeSerialized"
internal const val KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID = "keySendMessagePreviousSenderAddressId"

internal const val KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM = "keySendMessageErrorResult"

private const val INPUT_MESSAGE_DB_ID_NOT_FOUND = -1L

class SendMessageWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val saveDraft: SaveDraft,
    private val sendPreferencesFactory: SendPreferencesFactory
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val message = messageDetailsRepository.findMessageByMessageDbId(getInputMessageDbId())
            ?: return failureWithError(SendMessageWorkerError.MessageNotFound)
        val previousSenderAddressId = requireNotNull(getInputPreviousSenderAddressId())

        return when (saveDraft(message, previousSenderAddressId)) {
            is SaveDraftResult.Success -> {
                fetchMissingSendPreferences(message)
                Result.failure()
            }
            else -> failureWithError(SendMessageWorkerError.DraftCreationFailed)
        }

    }

    private fun fetchMissingSendPreferences(message: Message): List<SendPreference> {
        val emailSet = mutableSetOf<String>()
        message.toListString
            .split(Constants.EMAIL_DELIMITER)
            .filter { it.isNotBlank() }
            .map { emailSet.add(it) }

        message.ccListString
            .split(Constants.EMAIL_DELIMITER)
            .filter { it.isNotBlank() }
            .map { emailSet.add(it) }

        message.bccListString
            .split(Constants.EMAIL_DELIMITER)
            .filter { it.isNotBlank() }
            .map { emailSet.add(it) }

        val sendPreferences = sendPreferencesFactory.fetch(emailSet.toList())
        return sendPreferences.values.toList()
    }

    private suspend fun saveDraft(message: Message, previousSenderAddressId: String): SaveDraftResult {
        return this.saveDraft(
            SaveDraft.SaveDraftParameters(
                message,
                getInputAttachmentIds(),
                getInputParentId(),
                getInputActionType(),
                previousSenderAddressId
            )
        ).first()
    }

    private fun failureWithError(error: SendMessageWorkerError): Result {
        val errorData = workDataOf(KEY_OUTPUT_RESULT_SEND_MESSAGE_ERROR_ENUM to error.name)
        return Result.failure(errorData)
    }

    private fun getInputActionType(): Constants.MessageActionType =
        inputData
            .getString(KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_SERIALIZED)?.deserialize()
            ?: Constants.MessageActionType.NONE

    private fun getInputPreviousSenderAddressId() =
        inputData.getString(KEY_INPUT_SEND_MESSAGE_PREV_SENDER_ADDR_ID)

    private fun getInputParentId() = inputData.getString(KEY_INPUT_SEND_MESSAGE_MSG_PARENT_ID)

    private fun getInputMessageDbId() =
        inputData.getLong(KEY_INPUT_SEND_MESSAGE_MSG_DB_ID, INPUT_MESSAGE_DB_ID_NOT_FOUND)

    private fun getInputAttachmentIds() =
        inputData.getStringArray(KEY_INPUT_SEND_MESSAGE_ATTACHMENT_IDS)?.asList().orEmpty()

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
                        KEY_INPUT_SEND_MESSAGE_ACTION_TYPE_SERIALIZED to actionType.serialize(),
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
