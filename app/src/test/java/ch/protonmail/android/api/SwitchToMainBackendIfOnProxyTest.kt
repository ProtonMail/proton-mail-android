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

import android.content.SharedPreferences
import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.models.doh.ProxyItem
import ch.protonmail.android.core.UserManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals

internal class SwitchToMainBackendIfOnProxyTest {

    private val legacyUserMock = mockk<User>()
    private val userManagerMock = mockk<UserManager> {
        every { requireCurrentLegacyUser() } returns legacyUserMock
    }
    private val switchToMainBackendIfAvailableMock = mockk<SwitchToMainBackendIfAvailable>()
    private val sharedPrefsMock = mockk<SharedPreferences>()
    private val switchToMainBackendIfOnProxy = SwitchToMainBackendIfOnProxy(
        userManagerMock,
        switchToMainBackendIfAvailableMock,
        sharedPrefsMock
    )

    @BeforeTest
    fun setUp() {
        mockkObject(Proxies.Companion)
        val proxyItem = ProxyItem(baseUrl = "", lastTrialTimestamp = 0, success = true, active = true)
        val proxiesMock = mockk<Proxies> {
            every { getCurrentActiveProxy() } returns proxyItem
        }
        every { Proxies.getInstance(null, sharedPrefsMock) } returns proxiesMock
    }

    @Test
    fun `should return correct result without trying to switch to main BE when using the main BE`() = runBlockingTest {
        // given
        val expectedResult = SwitchToMainBackendIfOnProxy.AlreadyUsingMainBackend
        every { legacyUserMock.usingDefaultApi } returns true

        // when
        val actualResult = switchToMainBackendIfOnProxy()

        // then
        assertEquals(expectedResult, actualResult)
        coVerify(exactly = 0) { switchToMainBackendIfAvailableMock() }
    }

    @Test
    fun `should return failure result when not using main BE and switching to main BE failed`() = runBlockingTest {
        // given
        val expectedResult = SwitchToMainBackendIfOnProxy.SwitchFailure
        every { legacyUserMock.usingDefaultApi } returns false
        coEvery { switchToMainBackendIfAvailableMock() } returns false

        // when
        val actualResult = switchToMainBackendIfOnProxy()

        // then
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `should return success result when not using main BE and switching to main BE succeeded`() = runBlockingTest {
        // given
        val expectedResult = SwitchToMainBackendIfOnProxy.SwitchSuccess
        every { legacyUserMock.usingDefaultApi } returns false
        coEvery { switchToMainBackendIfAvailableMock() } returns true

        // when
        val actualResult = switchToMainBackendIfOnProxy()

        // then
        assertEquals(expectedResult, actualResult)
    }

    @AfterTest
    fun cleanUp() {
        unmockkObject(Proxies.Companion)
    }
}
