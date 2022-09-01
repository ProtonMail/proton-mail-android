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

import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class SwitchToMainBackendIfAvailableTest {

    private val legacyUserMock = mockk<User>(relaxUnitFun = true)
    private val userManagerMock = mockk<UserManager> {
        every { requireCurrentLegacyUser() } returns legacyUserMock
    }
    private val apiManagerMock = mockk<ProtonMailApiManager>()
    private val networkSwitcherMock = mockk<NetworkSwitcher>(relaxUnitFun = true)
    private val switchToMainBackendIfAvailable = SwitchToMainBackendIfAvailable(
        userManagerMock,
        { apiManagerMock },
        { networkSwitcherMock }
    )

    @Test
    fun `should switch to main BE, update the user and return true when main BE ping succeeds`() = runBlockingTest {
        // given
        coEvery { apiManagerMock.pingMainBackend() } returns ResponseBody()

        // when
        val switchedToMainBackend = switchToMainBackendIfAvailable()

        // then
        assertTrue(switchedToMainBackend)
        verify { networkSwitcherMock.forceSwitchToMainBackend() }
        verify { legacyUserMock.usingDefaultApi = true }
    }

    @Test
    fun `should not switch to main BE or update the user and return false when main BE ping fails`() = runBlockingTest {
        // given
        coEvery { apiManagerMock.pingMainBackend() } throws IllegalStateException("nope")

        // when
        val switchedToMainBackend = switchToMainBackendIfAvailable()

        // then
        assertFalse(switchedToMainBackend)
        verify(exactly = 0) { networkSwitcherMock.forceSwitchToMainBackend() }
        verify(exactly = 0) { legacyUserMock.usingDefaultApi = true }
    }
}
