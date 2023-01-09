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

package ch.protonmail.android.settings.domain

import ch.protonmail.android.settings.domain.usecase.UpdateSwipeActions
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class UpdateSwipeActionsTest {

    private var repository: MailSettingsRepository = mockk()

    private lateinit var updateSwipeActions: UpdateSwipeActions

    private val userId = UserId("userId")

    private val dispatchers = TestDispatcherProvider()

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        updateSwipeActions = UpdateSwipeActions(repository, dispatchers)
    }

    @Test
    fun verifyThatUpdateSwipeLeftIsExecuted() = runTest(dispatchers.Main) {

        // given
        val swipeLeft = SwipeAction.MarkRead
        coEvery { repository.updateSwipeLeft(userId, any()) } returns mockk()

        // when
        updateSwipeActions(userId, swipeLeft = swipeLeft)

        // then
        coVerify { repository.updateSwipeLeft(userId, swipeLeft) }
        coVerify(exactly = 0) { repository.updateSwipeRight(userId, swipeLeft) }
    }

    @Test
    fun verifyThatUpdateSwipeRightIsExecuted() = runTest(dispatchers.Main) {

        // given
        val swipeRight = SwipeAction.Spam
        coEvery { repository.updateSwipeRight(userId, any()) } returns mockk()

        // when
        updateSwipeActions.invoke(userId, swipeRight = swipeRight)

        // then
        coVerify { repository.updateSwipeRight(userId, swipeRight) }
        coVerify(exactly = 0) { repository.updateSwipeLeft(userId, swipeRight) }
    }

}
