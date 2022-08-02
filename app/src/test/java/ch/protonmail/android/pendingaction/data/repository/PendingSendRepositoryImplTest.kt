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
import ch.protonmail.android.testdata.MessageTestData
import ch.protonmail.android.testdata.UserTestData
import io.mockk.every
import io.mockk.mockk
import org.junit.Test

internal class PendingSendRepositoryImplTest {

    private val pendingActionDaoMock = mockk<PendingActionDao>(relaxUnitFun = true)
    private val schedulePendingSendCleanUpMock = mockk<SchedulePendingSendCleanUpWhenOnline>(relaxUnitFun = true)

    private val databaseProvider: DatabaseProvider = mockk {
        every { providePendingActionDao(any()) } returns pendingActionDaoMock
    }

    private val userManager = mockk<UserManager> {
        every { requireCurrentUserId() } returns UserTestData.userId
    }

    private val repository = PendingSendRepositoryImpl(
        databaseProvider,
        userManager,
        schedulePendingSendCleanUpMock
    )

    @Test
    fun `should delete pending send by message database id`() {
        // when
        repository.deletePendingSendByDatabaseId(MessageTestData.MESSAGE_DATABASE_ID)

        // then
        pendingActionDaoMock.deletePendingSendByDbId(MessageTestData.MESSAGE_DATABASE_ID)
    }

    @Test
    fun `should schedule pending send cleanup when online`() {
        // when
        repository.schedulePendingSendCleanupByMessageId(
            MessageTestData.MESSAGE_ID_RAW,
            MessageTestData.MESSAGE_SUBJECT,
            MessageTestData.MESSAGE_DATABASE_ID,
            UserTestData.userId
        )

        // then
        schedulePendingSendCleanUpMock(
            MessageTestData.MESSAGE_ID_RAW,
            MessageTestData.MESSAGE_SUBJECT,
            MessageTestData.MESSAGE_DATABASE_ID,
            UserTestData.userId
        )
    }
}
