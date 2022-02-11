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

package ch.protonmail.android.details.domain.usecase

import ch.protonmail.android.repository.MessageRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.entity.UserId
import kotlin.test.Test

/**
 * Tests the behaviour of [SetViewInDarkModeMessagePreference]
 */
class SetViewInDarkModeMessagePreferenceTest {

    private val messageRepository: MessageRepository = mockk {
        coEvery { saveViewInDarkModeMessagePreference(any(), any(), any()) } just runs
    }

    private val setViewInDarkModeMessagePreference = SetViewInDarkModeMessagePreference(
        messageRepository
    )

    private val testUserId = UserId("testUserId")
    private val testMessageId = "messageId"

    @Test
    fun `should call repository method for saving the view in dark mode message preference`() = runBlockingTest {
        // when
        setViewInDarkModeMessagePreference(testUserId, testMessageId, viewInDarkMode = true)

        // then
        coVerify {
            messageRepository.saveViewInDarkModeMessagePreference(testUserId, testMessageId, viewInDarkMode = true)
        }
    }
}
