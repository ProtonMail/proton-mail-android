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

package ch.protonmail.android.settings.domain.usecase

import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class UpdateShowMovedTest {

    private var repository: MailSettingsRepository = mockk()

    private lateinit var updateShowMoved: UpdateShowMoved

    private val userId = UserId("userId")

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        updateShowMoved = UpdateShowMoved(repository, TestDispatcherProvider)
    }

    @Test
    fun `verify that update show moved is executed`() = runBlockingTest {

        // given
        val showMoved = ShowMoved.Both
        coEvery { repository.updateSwipeLeft(userId, any()) } returns mockk()

        // when
        updateShowMoved(userId, showMoved)

        // then
        coVerify { repository.updateShowMoved(userId, showMoved) }
    }

}