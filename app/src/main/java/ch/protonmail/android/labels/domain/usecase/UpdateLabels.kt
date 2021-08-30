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

package ch.protonmail.android.labels.domain.usecase

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelType
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.worker.ApplyLabelWorker
import ch.protonmail.android.worker.RemoveLabelWorker
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

internal class UpdateLabels @Inject constructor(
    private val messageRepository: MessageRepository,
    private val accountManager: AccountManager,
    private val labelRepository: LabelRepository,
    private val applyLabelWorker: ApplyLabelWorker.Enqueuer,
    private val removeLabelWorker: RemoveLabelWorker.Enqueuer,
    private val dispatchers: DispatcherProvider,
) {

    suspend operator fun invoke(
        messageId: String,
        checkedLabelIds: List<String>
    ) = withContext(dispatchers.Io) {
        val userId = accountManager.getPrimaryUserId().filterNotNull().first()
        val message = requireNotNull(messageRepository.findMessage(userId, messageId))
        val existingLabels = labelRepository.findAllLabels(userId, false)
            .filter { it.id.id in message.labelIDsNotIncludingLocations }
        Timber.v("UpdateLabels checkedLabelIds: $checkedLabelIds")
        findAllLabelsWithIds(
            message,
            checkedLabelIds,
            existingLabels,
            userId
        )
    }

    private suspend fun findAllLabelsWithIds(
        message: Message,
        checkedLabelIds: List<String>,
        labels: List<LabelEntity>,
        userId: UserId
    ) = withContext(dispatchers.Io) {
        val labelsToRemove = arrayListOf<String>()
        val messageId = requireNotNull(message.messageId)
        val mutableLabelIds = checkedLabelIds.toMutableList()
        for (label in labels) {
            ensureActive()
            val labelId: String = label.id.id
            if (!mutableLabelIds.contains(labelId) && label.type == LabelType.MESSAGE_LABEL) {
                // this label should be removed
                labelsToRemove.add(labelId)
                removeLabelWorker.enqueue(listOf(messageId), labelId)
            } else if (mutableLabelIds.contains(labelId)) {
                // the label remains
                mutableLabelIds.remove(labelId)
            }
        }
        // schedule updates of all message
        mutableLabelIds.forEach { applyLabelWorker.enqueue(listOf(messageId), it) }
        // update the message with the new labels
        message.addLabels(mutableLabelIds)
        message.removeLabels(labelsToRemove)

        messageRepository.saveMessage(userId, message)
    }
}
