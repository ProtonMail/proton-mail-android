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

package ch.protonmail.android.usecase.delete

import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.api.models.room.messages.Message
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.worker.DeleteMessageWorker
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * Use case responsible for removing a message from the DB and scheduling
 * [DeleteMessageWorker] that will send a deferrable delete message network request.
 */
class DeleteMessage @Inject constructor(
    private val dispatchers: DispatcherProvider,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val pendingActionsDatabase: PendingActionsDao,
    private val workerScheduler: DeleteMessageWorker.Enqueuer
) {

    suspend operator fun invoke(messageIds: List<String>): DeleteMessageResult =
        withContext(dispatchers.Io) {

            val (validMessageIdList, invalidMessageIdList) = getValidAndInvalidMessages(messageIds)

            val messagesToSave = mutableListOf<Message>()
            val searchMessagesToSave = mutableListOf<Message>()

            for (id in validMessageIdList) {
                ensureActive()
                messageDetailsRepository.findMessageById(id)?.let { message ->
                    message.deleted = true
                    messagesToSave.add(message)
                }
                messageDetailsRepository.findSearchMessageById(id)?.let { searchMessage ->
                    searchMessage.deleted = true
                    searchMessagesToSave.add(searchMessage)
                }
            }

            ensureActive()
            messageDetailsRepository.saveMessagesInOneTransaction(messagesToSave)
            messageDetailsRepository.saveSearchMessagesInOneTransaction(searchMessagesToSave)

            val scheduleWorkerResult = workerScheduler.enqueue(validMessageIdList)
            return@withContext DeleteMessageResult(
                invalidMessageIdList.isEmpty(),
                scheduleWorkerResult
            )
        }

    private fun getValidAndInvalidMessages(messageIds: List<String>): Pair<List<String>, List<String>> {
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
}
