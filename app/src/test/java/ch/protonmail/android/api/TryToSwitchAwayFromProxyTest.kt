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

package ch.protonmail.android.api

import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class TryToSwitchAwayFromProxyTest {

    private val legacyUserMock = mockk<User>()
    private val userManagerMock = mockk<UserManager> {
        every { requireCurrentLegacyUser() } returns legacyUserMock
    }
    private val switchToMainBackendIfAvailableMock = mockk<SwitchToMainBackendIfAvailable>()
    private val tryToSwitchAwayFromProxy = TryToSwitchAwayFromProxy(
        userManagerMock,
        switchToMainBackendIfAvailableMock
    )

    @Test
    fun `should return false without trying to switch to main backend when using the default api`() = runBlockingTest {
        // given
        every { legacyUserMock.usingDefaultApi } returns true

        // when
        val switchedAwayFromProxy = tryToSwitchAwayFromProxy()

        // then
        assertFalse(switchedAwayFromProxy)
        coVerify(exactly = 0) { switchToMainBackendIfAvailableMock() }
    }

    @Test
    fun `should return false when not using the default api and switching to main backend failed`() = runBlockingTest {
        // given
        every { legacyUserMock.usingDefaultApi } returns false
        coEvery { switchToMainBackendIfAvailableMock() } returns false

        // when
        val switchedAwayFromProxy = tryToSwitchAwayFromProxy()

        // then
        assertFalse(switchedAwayFromProxy)
    }

    @Test
    fun `should return true when not using default api and switching to main backend succeeded`() = runBlockingTest {
        // given
        every { legacyUserMock.usingDefaultApi } returns false
        coEvery { switchToMainBackendIfAvailableMock() } returns true

        // when
        val switchedAwayFromProxy = tryToSwitchAwayFromProxy()

        // then
        assertTrue(switchedAwayFromProxy)
    }
}
