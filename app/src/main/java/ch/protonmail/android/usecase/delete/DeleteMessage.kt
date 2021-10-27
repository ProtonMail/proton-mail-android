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
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.PendingSend
import ch.protonmail.android.data.local.model.PendingUpload
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.usecase.model.DeleteMessageResult
import ch.protonmail.android.worker.DeleteMessageWorker
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

/**
 * Use case responsible for removing a message from the DB and scheduling
 * [DeleteMessageWorker] that will send a deferrable delete message network request.
 */
class DeleteMessage @Inject constructor(
    private val conversationsRepository: ConversationsRepository,
    private val dispatchers: DispatcherProvider,
    private val messageDetailsRepository: MessageDetailsRepository,
    private val pendingActionDatabase: PendingActionDao,
    private val workerScheduler: DeleteMessageWorker.Enqueuer
) {

    suspend operator fun invoke(
        messageIds: List<String>,
        currentLabelId: String?,
        userId: UserId
    ): DeleteMessageResult =
        withContext(dispatchers.Io) {

            val (validMessageIdList, invalidMessageIdList) = getValidAndInvalidMessages(messageIds)

            val messagesToSave = mutableListOf<Message>()

            for (id in validMessageIdList) {
                ensureActive()
                messageDetailsRepository.findMessageById(id).first()?.let { message ->
                    message.deleted = true
                    messagesToSave.add(message)
                }
            }

            ensureActive()
            messageDetailsRepository.saveMessagesInOneTransaction(messagesToSave)
            conversationsRepository.updateConversationsAfterDeletingMessages(userId, validMessageIdList)

            val scheduleWorkerResult = workerScheduler.enqueue(validMessageIdList, currentLabelId)
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
            val pendingUploads = pendingActionDatabase.findPendingUploadByMessageId(id)
            val pendingForSending = pendingActionDatabase.findPendingSendByMessageId(id)

            if (areThereAnyPendingUplandsOrSends(pendingUploads, pendingForSending)) {
                invalidMessageIdList.add(id)
            } else {
                validMessageIdList.add(id)
            }
        }
        return validMessageIdList to invalidMessageIdList
    }

    /**
     * Verify pending uploads.
     *
     * @return true if
     * there is a pending upload or pending send message
     * or if there is a failed pending send expressed by the nullable Sent property of the PendingSend class
     */
    private fun areThereAnyPendingUplandsOrSends(
        pendingUploads: PendingUpload?,
        pendingForSending: PendingSend?
    ) =
        pendingUploads != null || !(pendingForSending == null || pendingForSending.sent == false)
}
