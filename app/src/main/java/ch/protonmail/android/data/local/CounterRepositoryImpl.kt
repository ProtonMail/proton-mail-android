/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.data.local

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.repository.MessageRepository
import kotlinx.coroutines.yield
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

internal class CounterRepositoryImpl @Inject constructor(
    private val counterDao: CounterDao,
    private val messageRepository: MessageRepository
) : CounterRepository {

    override suspend fun updateMessageLabelCounter(
        userId: UserId,
        labelId: String,
        messageIds: List<String>,
        modificationMethod: CounterRepository.CounterModificationMethod
    ) {

        if (messageIds.isEmpty() || labelId.isBlank() || userId.id.isBlank()) {
            throw IllegalArgumentException("Arguments: $labelId, $userId, $messageIds are incorrect")
        }

        var totalUnread = 0

        for (messageId in messageIds) {
            yield()
            val message: Message = messageRepository.findMessage(userId, messageId) ?: continue
            if (message.isRead) {
                totalUnread++
            }
        }

        counterDao.findUnreadLabelById(labelId)?.let { counter ->
            when (modificationMethod) {
                CounterRepository.CounterModificationMethod.INCREMENT -> counter.increment(totalUnread)
                CounterRepository.CounterModificationMethod.DECREMENT -> counter.decrement(totalUnread)
            }
            counterDao.insertUnreadLabel(counter)
        }
    }

}
