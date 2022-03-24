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
import ch.protonmail.android.testdata.MessageTestData
import io.mockk.mockk
import org.junit.Test

internal class PendingSendRepositoryImplTest {

    private val pendingActionDaoMock = mockk<PendingActionDao>(relaxUnitFun = true)
    private val schedulePendingSendCleanUpMock = mockk<SchedulePendingSendCleanUpWhenOnline>(relaxUnitFun = true)
    private val repository = PendingSendRepositoryImpl(
        pendingActionDaoMock,
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
            MessageTestData.MESSAGE_DATABASE_ID
        )

        // then
        schedulePendingSendCleanUpMock(
            MessageTestData.MESSAGE_ID_RAW,
            MessageTestData.MESSAGE_SUBJECT,
            MessageTestData.MESSAGE_DATABASE_ID
        )
    }
}
