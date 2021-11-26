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

package ch.protonmail.android.usecase.keys

import ch.protonmail.android.R
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.utils.resources.StringResourceResolver
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogOutIfNotAllActiveKeysAreDecryptableTest {

    private val userManagerMock = mockk<UserManager>()
    private val areActiveKeysDecryptableMock = mockk<CheckIfActiveKeysAreDecryptable>()
    private val userNotifierMock = mockk<UserNotifier>()
    private val getStringResourceMock = mockk<StringResourceResolver>()
    private val logOutIfNotAllActiveKeysAreDecryptable = LogOutIfNotAllActiveKeysAreDecryptable(
        userManagerMock,
        areActiveKeysDecryptableMock,
        userNotifierMock,
        getStringResourceMock
    )

    @Test
    fun `should return false and not log out nor notify user when keys are decryptable`() {
        // given
        every { areActiveKeysDecryptableMock() } returns true

        // when
        val loggingOut = logOutIfNotAllActiveKeysAreDecryptable()

        // then
        assertFalse(loggingOut)
        verify { userManagerMock wasNot called }
        verify { userNotifierMock wasNot called }
    }

    @Test
    fun `should return true, log out and notify user when keys are not decryptable`() {
        // given
        every { areActiveKeysDecryptableMock() } returns false
        every { userManagerMock.logoutLastActiveAccount() } just runs
        every { userNotifierMock.showError(any()) } just runs
        every { getStringResourceMock(R.string.logged_out_description) } returns TestData.EXPECTED_ERROR_MESSAGE

        // when
        val loggingOut = logOutIfNotAllActiveKeysAreDecryptable()

        // then
        assertTrue(loggingOut)
        verify { userManagerMock.logoutLastActiveAccount() }
        verify { userNotifierMock.showError(TestData.EXPECTED_ERROR_MESSAGE) }
    }

    @Test
    fun `should return true, log out and notify user using the provided error message when keys are not decryptable`() {
        // given
        val providedErrorMessage = R.string.logged_out_contact_support
        every { areActiveKeysDecryptableMock() } returns false
        every { userManagerMock.logoutLastActiveAccount() } just runs
        every { userNotifierMock.showError(any()) } just runs
        every { getStringResourceMock(providedErrorMessage) } returns TestData.EXPECTED_ERROR_MESSAGE

        // when
        val loggingOut = logOutIfNotAllActiveKeysAreDecryptable(providedErrorMessage)

        // then
        assertTrue(loggingOut)
        verify { userManagerMock.logoutLastActiveAccount() }
        verify { userNotifierMock.showError(TestData.EXPECTED_ERROR_MESSAGE) }
    }

    private object TestData {
        const val EXPECTED_ERROR_MESSAGE = "We logged you out, whatchu' gonna do about it?"
    }
}
