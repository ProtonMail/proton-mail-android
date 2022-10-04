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
import ch.protonmail.android.api.models.doh.Proxies
import ch.protonmail.android.api.models.doh.ProxyItem
import ch.protonmail.android.api.models.doh.ProxyList
import ch.protonmail.android.api.segments.event.EventManager
import ch.protonmail.android.api.segments.event.EventService
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

internal class NetworkSwitcherTest {

    private val apiManagerMock = mockk<ProtonMailApiManager>(relaxUnitFun = true) {
        coEvery { ping() } returns ResponseBody()
    }
    private val apiProviderMock = mockk<ProtonMailApiProvider>()
    private val protonOkHttpProviderMock = mockk<OkHttpProvider>()
    private val eventManagerMock = mockk<EventManager>(relaxUnitFun = true)
    private val networkSwitcher = NetworkSwitcher(
        apiManagerMock,
        apiProviderMock,
        protonOkHttpProviderMock,
        BASE_URL,
        eventManagerMock
    )

    @Test
    fun `should ping BE`() = runTest {
        // when
        networkSwitcher.tryRequest()

        // then
        coVerify { apiManagerMock.ping() }
    }

    @Test
    fun `should reconfigure the api using main BE endpoint when force switching to main BE`() {
        // given
        val mainBackendApi = mockk<ProtonMailApi>()
        val securedServices = mockk<SecuredServices>()
        val eventService = mockk<EventService>()
        coEvery { securedServices.event } returns eventService
        coEvery { mainBackendApi.securedServices } returns securedServices
        coEvery { apiProviderMock.rebuild(protonOkHttpProviderMock, BASE_URL) } returns mainBackendApi

        // when
        networkSwitcher.forceSwitchToMainBackend()

        // then
        verify { apiManagerMock.reset(mainBackendApi) }
        verify { eventManagerMock.reconfigure(eventService) }
    }

    @Test
    fun `should reconfigure the api using current active proxy`() {
        // given
        val proxies = Proxies(
            proxyList = ProxyList(
                proxies = listOf(
                    NON_ACTIVE_PROXY_URL.toProxy(active = false),
                    ACTIVE_PROXY_URL.toProxy(active = true)
                )
            ),
            prefs = mockk()
        )
        val activeProxyApi = mockk<ProtonMailApi>()
        val securedServices = mockk<SecuredServices>()
        val eventService = mockk<EventService>()
        coEvery { securedServices.event } returns eventService
        coEvery { activeProxyApi.securedServices } returns securedServices
        coEvery { apiProviderMock.rebuild(protonOkHttpProviderMock, ACTIVE_PROXY_URL) } returns activeProxyApi

        // when
        networkSwitcher.reconfigureProxy(proxies)

        // then
        verify { apiManagerMock.reset(activeProxyApi) }
        verify { eventManagerMock.reconfigure(eventService) }
    }

    private fun String.toProxy(active: Boolean) = ProxyItem(
        baseUrl = this,
        lastTrialTimestamp = 200L,
        success = null,
        active = active
    )

    private companion object TestData {

        const val BASE_URL = "protonmail.com"
        const val ACTIVE_PROXY_URL = "proxy-active.com"
        const val NON_ACTIVE_PROXY_URL = "proxy-non-active.com"
    }
}
