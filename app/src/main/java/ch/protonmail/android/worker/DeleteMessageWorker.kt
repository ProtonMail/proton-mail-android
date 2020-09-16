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
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.extensions.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_WORKER_ERROR_DESCRIPTION = "KeyWorkerErrorDescription"
internal const val KEY_INPUT_DATA_MESSAGE_IDS = "KeyInputDataMessageIds"
internal const val KEY_INVALID_MESSAGE_IDS_RESULT = "KeyInvalidMessageIdsResult"
private const val WORKER_TAG = "PostDeleteWorkerTag"

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

    @Inject
    internal lateinit var pendingActionsDatabase: PendingActionsDao

    @Inject
    internal lateinit var messageDetailsRepository: MessageDetailsRepository

    init {
        context.app.appComponent.inject(this)
    }

    override suspend fun doWork(): Result {
        val messageIds = inputData.getStringArray(KEY_INPUT_DATA_MESSAGE_IDS)
            ?: emptyArray()

        // skip empty input
        if (messageIds.isEmpty()) {
            return Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Cannot proceed with empty messages list")
            )
        }

        val validAndInvalidMessagesPair = getValidAndInvalidMessages(messageIds)
        val validMessageIdList = validAndInvalidMessagesPair.first
        val invalidMessageIdList = validAndInvalidMessagesPair.second

        return withContext(Dispatchers.IO) {
            // delete messages on remote
            if (validMessageIdList.isNotEmpty()) {
                val response = api.deleteMessage(IDList(validMessageIdList))
                if (response.code == Constants.RESPONSE_CODE_OK ||
                    response.code == Constants.RESPONSE_CODE_MULTIPLE_OK
                ) {
                    Timber.v("Response success code ${response.code}")
                    updateDb(validMessageIdList)
                    getInvalidMessagesResult(invalidMessageIdList)
                } else {
                    Timber.v("ApiException failure response code ${response.code}")
                    Result.failure(
                        workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "ApiException response code ${response.code}")
                    )
                }
            }
            getInvalidMessagesResult(invalidMessageIdList)
        }
    }

    private fun getInvalidMessagesResult(invalidMessageIdList: List<String>): Result {
        return if (invalidMessageIdList.isNotEmpty()) {
            Timber.v("ApiException failure invalidMessageIdList not empty")
            Result.failure(
                workDataOf(KEY_INVALID_MESSAGE_IDS_RESULT to invalidMessageIdList)
            )
        } else {
            Result.success()
        }
    }

    private fun updateDb(validMessageIdList: List<String>) {
        for (id in validMessageIdList) {

            val message = messageDetailsRepository.findMessageById(id)
            val searchMessage = messageDetailsRepository.findSearchMessageById(id)

            if (message != null) {
                message.deleted = true
                messageDetailsRepository.saveMessageInDB(message)
            }
            if (searchMessage != null) {
                searchMessage.deleted = true
                messageDetailsRepository.saveSearchMessageInDB(searchMessage)
            }
        }
    }

    private fun getValidAndInvalidMessages(messageIds: Array<String>): Pair<List<String>, List<String>> {
        val validMessageIdList = mutableListOf<String>()
        val invalidMessageIdList = mutableListOf<String>()

        for (id in messageIds) {
            if (id.isEmpty()) {
                continue
            }
            val pendingUploads = pendingActionsDatabase.findPendingUploadByMessageId(id)
            val pendingForSending = pendingActionsDatabase.findPendingSendByMessageId(id)

            if (pendingUploads == null && (pendingForSending == null || pendingForSending.sent == false)) {
                // do the logic below if there is no pending upload and not pending send for the message
                // trying to be deleted
                // or if there is a failed pending send expressed by value `false` in the nullable Sent
                // property of the PendingSend class
                validMessageIdList.add(id)
            } else {
                invalidMessageIdList.add(id)
            }
        }

        return validMessageIdList to invalidMessageIdList
    }

    class Enqueuer(private val workManager: WorkManager) {
        fun enqueue(messageIds: List<String>): Operation {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<DeleteMessageWorker>()
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_INPUT_DATA_MESSAGE_IDS to messageIds.toTypedArray()))
                .addTag(WORKER_TAG)
                .build()
            Timber.v("Scheduling PostDeleteWorker")
            return workManager.enqueue(workRequest)
        }

        fun getWorkStatusLiveData() = workManager.getWorkInfosByTagLiveData(WORKER_TAG)
    }
}
