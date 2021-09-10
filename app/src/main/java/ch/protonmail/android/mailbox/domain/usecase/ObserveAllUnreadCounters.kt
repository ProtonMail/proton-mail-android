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

package ch.protonmail.android.mailbox.domain.usecase

import ch.protonmail.android.mailbox.domain.ConversationsRepository
import ch.protonmail.android.mailbox.domain.model.AllUnreadCounters
import ch.protonmail.android.mailbox.domain.model.UnreadCounter
import ch.protonmail.android.repository.MessageRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Emit [AllUnreadCounters]
 *
 * This can emit more than once for a single event, particularly it will emits:
 * * **one** [DataResult.Success] if we have success for Messages Counters and Conversations Counters
 * * **one** [DataResult.Success] and **one** [DataResult.Error] if only one if one of the above is successfully
 * * **two** [DataResult.Error] if none of the above is successfully
 */
internal class ObserveAllUnreadCounters @Inject constructor(
    private val messagesRepository: MessageRepository,
    private val conversationsRepository: ConversationsRepository
) {

    operator fun invoke(
        userId: UserId,
        refreshInterval: Duration = DEFAULT_REFRESH_INTERVAL
    ): Flow<DataResult<AllUnreadCounters>> {
        val refreshFlow = flow {
            emit(Unit)
            while (currentCoroutineContext().isActive) {
                delay(refreshInterval)
                messagesRepository.refreshUnreadCounters()
                conversationsRepository.refreshUnreadCounters()
            }
        }

        return combineTransform(
            refreshFlow,
            messagesRepository.getUnreadCounters(userId),
            conversationsRepository.getUnreadCounters(userId)
        ) { _, messagesCounters, conversationsCounters ->
            emitAllUnreadCounters(
                messagesCounters = messagesCounters,
                conversationsCounters = conversationsCounters
            )
        }
    }

    private suspend fun FlowCollector<DataResult<AllUnreadCounters>>.emitAllUnreadCounters(
        messagesCounters: DataResult<List<UnreadCounter>>,
        conversationsCounters: DataResult<List<UnreadCounter>>
    ) {
        if (messagesCounters is DataResult.Success || conversationsCounters is DataResult.Success) {
            val allCounters = AllUnreadCounters(
                messagesCounters = messagesCounters.valueOrEmpty(),
                conversationsCounters = conversationsCounters.valueOrEmpty()
            )
            emit(DataResult.Success(ResponseSource.Local, allCounters))
        }

        if (messagesCounters is DataResult.Error) emit(messagesCounters)
        if (conversationsCounters is DataResult.Error) emit(conversationsCounters)
    }

    private fun DataResult<List<UnreadCounter>>.valueOrEmpty(): List<UnreadCounter> =
        if (this is DataResult.Success) value else emptyList()

    companion object {

        val DEFAULT_REFRESH_INTERVAL = 30.toDuration(DurationUnit.SECONDS)
    }
}
