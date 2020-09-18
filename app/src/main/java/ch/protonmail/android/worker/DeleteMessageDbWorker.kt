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
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.utils.extensions.app
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

internal const val KEY_INVALID_MESSAGE_IDS_RESULT = "KeyInvalidMessageIdsResult"
internal const val KEY_INPUT_VALID_MESSAGES_IDS = "KeyInputValidMessagesIds"

/**
 * Work Manager Worker responsible for deleting messages from messages database.
 * To be used chained with [DeleteMessageWorker].
 *
 *  InputData has to contain non-null values for:
 *  labelId
 *
 * @see androidx.work.WorkManager
 */
class DeleteMessageDbWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    @Inject
    internal lateinit var messageDetailsRepository: MessageDetailsRepository

    @Inject
    internal lateinit var pendingActionsDatabase: PendingActionsDao

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

            val workData = Data.Builder().put(KEY_INPUT_VALID_MESSAGES_IDS, validMessageIdList)

            if (invalidMessageIdList.isNotEmpty()) {
                Timber.d("InvalidMessageIdList is not empty!")
                workData.put(KEY_INVALID_MESSAGE_IDS_RESULT, invalidMessageIdList)
            }
            Result.success(workData.build())
        }
    }

    private fun getValidAndInvalidMessages(messageIds: Array<String>): Pair<Array<String>, Array<String>> {
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

        return validMessageIdList.toTypedArray() to invalidMessageIdList.toTypedArray()
    }
}
