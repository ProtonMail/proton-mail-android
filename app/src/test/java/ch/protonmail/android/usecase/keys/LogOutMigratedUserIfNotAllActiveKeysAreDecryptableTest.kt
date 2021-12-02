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
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.utils.notifier.UserNotifier
import ch.protonmail.android.utils.resources.StringResourceResolver
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LogOutMigratedUserIfNotAllActiveKeysAreDecryptableTest {

    private val userSpy = spyk<User>()
    private val userManagerMock = mockk<UserManager> {
        every { user } returns userSpy
    }
    private val areActiveKeysDecryptableMock = mockk<CheckIfActiveKeysAreDecryptable>()
    private val userNotifierMock = mockk<UserNotifier>()
    private val getStringResourceMock = mockk<StringResourceResolver>()
    private val logOutMigratedUserIfNotAllActiveKeysAreDecryptable = LogOutMigratedUserIfNotAllActiveKeysAreDecryptable(
        userManagerMock,
        areActiveKeysDecryptableMock,
        userNotifierMock,
        getStringResourceMock
    )

    @Test
    fun `should return false and not log out nor notify user when keys are decryptable and user is migrated`() {
        // given
        every { areActiveKeysDecryptableMock() } returns true
        every { userSpy.legacyAccount } returns false

        // when
        val loggingOut = logOutMigratedUserIfNotAllActiveKeysAreDecryptable()

        // then
        assertFalse(loggingOut)
        verify(exactly = 0) { userManagerMock.logoutLastActiveAccount() }
        verify { userNotifierMock wasNot called }
    }

    @Test
    fun `should return false and not log out nor notify user when keys are decryptable and user is not migrated`() {
        // given
        every { areActiveKeysDecryptableMock() } returns true
        every { userSpy.legacyAccount } returns true

        // when
        val loggingOut = logOutMigratedUserIfNotAllActiveKeysAreDecryptable()

        // then
        assertFalse(loggingOut)
        verify(exactly = 0) { userManagerMock.logoutLastActiveAccount() }
        verify { userNotifierMock wasNot called }
    }

    @Test
    fun `should return false and not log out nor notify user when keys are not decryptable and user is not migrated`() {
        // given
        every { areActiveKeysDecryptableMock() } returns false
        every { userSpy.legacyAccount } returns true

        // when
        val loggingOut = logOutMigratedUserIfNotAllActiveKeysAreDecryptable()

        // then
        assertFalse(loggingOut)
        verify(exactly = 0) { userManagerMock.logoutLastActiveAccount() }
        verify { userNotifierMock wasNot called }
    }

    @Test
    fun `should return true, log out and notify user when keys are not decryptable and user is migrated`() {
        // given
        every { areActiveKeysDecryptableMock() } returns false
        every { userManagerMock.logoutLastActiveAccount() } just runs
        every { userNotifierMock.showError(any()) } just runs
        every { getStringResourceMock(R.string.logged_out_description) } returns TestData.EXPECTED_ERROR_MESSAGE
        every { userSpy.legacyAccount } returns false

        // when
        val loggingOut = logOutMigratedUserIfNotAllActiveKeysAreDecryptable()

        // then
        assertTrue(loggingOut)
        verify { userManagerMock.logoutLastActiveAccount() }
        verify { userNotifierMock.showError(TestData.EXPECTED_ERROR_MESSAGE) }
    }

    @Test
    fun `should return true, log out and notify with provided message if keys are not decryptable and user migrated`() {
        // given
        val providedErrorMessage = R.string.logged_out_contact_support
        every { areActiveKeysDecryptableMock() } returns false
        every { userManagerMock.logoutLastActiveAccount() } just runs
        every { userNotifierMock.showError(any()) } just runs
        every { getStringResourceMock(providedErrorMessage) } returns TestData.EXPECTED_ERROR_MESSAGE
        every { userSpy.legacyAccount } returns false

        // when
        val loggingOut = logOutMigratedUserIfNotAllActiveKeysAreDecryptable(providedErrorMessage)

        // then
        assertTrue(loggingOut)
        verify { userManagerMock.logoutLastActiveAccount() }
        verify { userNotifierMock.showError(TestData.EXPECTED_ERROR_MESSAGE) }
    }

    private object TestData {
        const val EXPECTED_ERROR_MESSAGE = "We logged you out, whatchu' gonna do about it?"
    }
}
