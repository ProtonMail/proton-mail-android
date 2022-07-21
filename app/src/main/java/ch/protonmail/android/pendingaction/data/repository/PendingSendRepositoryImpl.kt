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

package ch.protonmail.android.pendingaction.data.repository

import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.pendingaction.data.worker.SchedulePendingSendCleanUpWhenOnline
import ch.protonmail.android.pendingaction.domain.repository.PendingSendRepository
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class PendingSendRepositoryImpl @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val userManager: UserManager,
    private val schedulePendingSendCleanUpWhenOnline: SchedulePendingSendCleanUpWhenOnline
) : PendingSendRepository {

    private val userId: UserId
        get() = userManager.requireCurrentUserId()

    private val pendingActionDao: PendingActionDao
        get() = databaseProvider.providePendingActionDao(userId)

    override fun deletePendingSendByDatabaseId(databaseId: Long) = pendingActionDao
        .deletePendingSendByDbId(databaseId)

    override fun schedulePendingSendCleanupByMessageId(
        messageId: String,
        messageSubject: String,
        messageDatabaseId: Long,
        userId: UserId
    ) {
        schedulePendingSendCleanUpWhenOnline(messageId, messageSubject, messageDatabaseId, userId)
    }
}
