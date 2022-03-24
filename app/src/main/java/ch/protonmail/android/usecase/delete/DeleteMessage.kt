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

import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.Constants.MAX_MESSAGE_ID_WORKER_ARGUMENTS
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.pendingaction.data.model.PendingSend
import ch.protonmail.android.pendingaction.data.model.PendingUpload
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.usecase.model.DeleteMessageResult
import ch.protonmail.android.worker.DeleteMessageWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

/**
 * Use case responsible for removing a message from the DB and scheduling
 * [DeleteMessageWorker] that will send a deferrable delete message network request.
 */
class DeleteMessage @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val messageRepository: MessageRepository,
    private val conversationsRepository: ConversationsRepository,
    private val workerScheduler: DeleteMessageWorker.Enqueuer,
    private val externalScope: CoroutineScope
) {

    suspend operator fun invoke(
        messageIds: List<String>,
        currentLabelId: String?,
        userId: UserId
    ): DeleteMessageResult = externalScope.async {
        val (validMessageIdList, invalidMessageIdList) = getValidAndInvalidMessages(userId, messageIds)

        validMessageIdList
            .chunked(MAX_MESSAGE_ID_WORKER_ARGUMENTS)
            .forEach { ids ->
                conversationsRepository.updateConversationsWhenDeletingMessages(userId, ids)
                messageRepository.deleteMessagesInDb(userId, ids)

                workerScheduler.enqueue(ids, currentLabelId)
            }
        return@async DeleteMessageResult(
            invalidMessageIdList.isEmpty()
        )
    }.await()

    private suspend fun getValidAndInvalidMessages(
        userId: UserId,
        messageIds: List<String>
    ): Pair<List<String>, List<String>> {
        val validMessageIdList = mutableListOf<String>()
        val invalidMessageIdList = mutableListOf<String>()

        for (id in messageIds) {
            if (id.isEmpty()) {
                continue
            }
            val pendingActionDao = databaseProvider.providePendingActionDao(userId)
            val pendingUploads = pendingActionDao.findPendingUploadByMessageId(id)
            val pendingForSending = pendingActionDao.findPendingSendByMessageId(id)

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
