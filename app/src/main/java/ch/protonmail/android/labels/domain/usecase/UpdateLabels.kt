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
import ch.protonmail.android.jobs.RemoveLabelJob
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelType
import ch.protonmail.android.repository.MessageRepository
import ch.protonmail.android.worker.ApplyLabelWorker
import com.birbit.android.jobqueue.Job
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

internal class UpdateLabels @Inject constructor(
    private val messageRepository: MessageRepository,
    private val accountManager: AccountManager,
    private val labelRepository: LabelRepository,
    private val applyLabelWorker: ApplyLabelWorker.Enqueuer
) {

    suspend operator fun invoke(
        messageId: String,
        checkedLabelIds: List<String>
    ) {
        val userId = accountManager.getPrimaryUserId().filterNotNull().first()
        val message = requireNotNull(messageRepository.findMessage(userId, messageId))
        val existingLabels = labelRepository.findAllLabels(userId)
            .filter { it.id in message.labelIDsNotIncludingLocations }
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
    ) {
        val labelsToRemove = ArrayList<String>()
        val jobList = ArrayList<Job>()
        val messageId = requireNotNull(message.messageId)
        val mutableLabelIds = checkedLabelIds.toMutableList()
        for (label in labels) {
            val labelId = label.id
            if (!mutableLabelIds.contains(labelId) && label.type == LabelType.MESSAGE_LABEL) {
                // this label should be removed
                labelsToRemove.add(labelId.id)
                jobList.add(RemoveLabelJob(listOf(messageId), labelId.id))
            } else if (mutableLabelIds.contains(labelId)) {
                // the label remains
                mutableLabelIds.remove(labelId)
            }
        }
        // what remains are the new labels
        mutableLabelIds.map { applyLabelWorker.enqueue(listOf(messageId), it) }
        // update the message with the new labels
        message.addLabels(mutableLabelIds)
        message.removeLabels(labelsToRemove)

        messageRepository.saveMessage(userId, message)
    }
}
