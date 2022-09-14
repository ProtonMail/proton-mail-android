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
import ch.protonmail.android.core.NetworkConnectivityManager
import ch.protonmail.android.core.UserManager
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Test

internal class NetworkConfiguratorTest {

    private val prefsMock = mockk<SharedPreferences>()
    private val userManagerMock = mockk<UserManager>()
    private val connectivityManager = mockk<NetworkConnectivityManager>()
    private val switchToMainBackendIfAvailable = mockk<SwitchToMainBackendIfAvailable>()
    private val networkSwitcherMock = mockk<NetworkSwitcher>(relaxUnitFun = true)
    private val networkConfigurator = NetworkConfigurator(
        dohProviders = emptyArray(),
        prefsMock,
        TestCoroutineScope(),
        userManagerMock,
        connectivityManager,
        switchToMainBackendIfAvailable,
        { networkSwitcherMock }
    )

    @Test
    fun `should force switch to main BE`() {
        // when
        networkConfigurator.forceSwitchToMainBackend()

        // then
        verify { networkSwitcherMock.forceSwitchToMainBackend() }
    }
}
