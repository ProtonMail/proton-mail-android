/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.pendingaction.data.repository

import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.pendingaction.data.worker.SchedulePendingSendCleanUpWhenOnline
import ch.protonmail.android.pendingaction.domain.repository.PendingSendRepository
import javax.inject.Inject

class PendingSendRepositoryImpl @Inject constructor(
    private val pendingActionDao: PendingActionDao,
    private val schedulePendingSendCleanUpWhenOnline: SchedulePendingSendCleanUpWhenOnline
) : PendingSendRepository {

    override fun deletePendingSendByDatabaseId(databaseId: Long) = pendingActionDao
        .deletePendingSendByDbId(databaseId)

    override fun schedulePendingSendCleanupByMessageId(
        messageId: String,
        messageSubject: String,
        messageDatabaseId: Long
    ) {
        schedulePendingSendCleanUpWhenOnline(messageId, messageSubject, messageDatabaseId)
    }
}
