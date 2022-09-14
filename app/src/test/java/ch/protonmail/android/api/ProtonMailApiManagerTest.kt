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
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import kotlin.test.assertEquals

internal class ProtonMailApiManagerTest {

    private val apiMock = mockk<ProtonMailApi>()
    private val mainBackendApiMock = mockk<ProtonMailApi>()
    private val apiManager = ProtonMailApiManager(
        apiMock,
        mainBackendApiMock
    )

    @Test
    fun `should call only the main backend api when pinging the main backend`() = runBlockingTest {
        // given
        val expectedResponse = ResponseBody()
        coEvery { mainBackendApiMock.ping() } returns expectedResponse

        // when
        val actualResponse = apiManager.pingMainBackend()

        // then
        assertEquals(expectedResponse, actualResponse)
        coVerify { apiMock wasNot called }
    }
}
