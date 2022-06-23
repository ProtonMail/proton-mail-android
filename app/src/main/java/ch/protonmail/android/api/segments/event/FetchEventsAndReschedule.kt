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

package ch.protonmail.android.api.segments.event

import android.content.Context
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

internal class FetchEventsAndReschedule @Inject constructor(
    private val eventManager: EventManager,
    private val accountManager: AccountManager,
    private val alarmReceiver: AlarmReceiver,
    private val context: Context,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke() {
        withContext(dispatchers.Io) {
            accountManager.getPrimaryUserId()
                .first()
                ?.let { userId ->
                    eventManager.consumeEventsFor(listOf(userId))
                    alarmReceiver.setAlarm(context)
                }
        }
    }
}
