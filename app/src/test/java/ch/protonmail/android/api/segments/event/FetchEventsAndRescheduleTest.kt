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

package ch.protonmail.android.api.segments.event

import android.content.Context
import ch.protonmail.android.api.exceptions.ApiException
import ch.protonmail.android.testdata.UserTestData
import io.mockk.called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

internal class FetchEventsAndRescheduleTest {

    private val eventManagerMock = mockk<EventManager> {
        coEvery { consumeEventsFor(listOf(UserTestData.userId)) } just runs
    }
    private val accountManagerMock = mockk<AccountManager>()
    private val contextMock = mockk<Context>()
    private val alarmReceiverMock = mockk<AlarmReceiver> {
        every { setAlarm(contextMock) } just runs
    }
    private val dispatchers = TestDispatcherProvider()

    private val fetchEventsAndReschedule = FetchEventsAndReschedule(
        eventManagerMock,
        accountManagerMock,
        alarmReceiverMock,
        contextMock,
        dispatchers
    )

    @Test
    fun `when primary id is null should do nothing`() = runTest(dispatchers.Main) {
        // given
        every { accountManagerMock.getPrimaryUserId() } returns flowOf(null)

        // when
        fetchEventsAndReschedule()

        // then
        verify { eventManagerMock wasNot called }
        verify { alarmReceiverMock wasNot called }
    }

    @Test
    fun `when primary id present should fetch the events and reschedule the event loop`() = runTest(dispatchers.Main) {
        // given
        every { accountManagerMock.getPrimaryUserId() } returns flowOf(UserTestData.userId)

        // when
        fetchEventsAndReschedule()

        // then
        coVerify { eventManagerMock.consumeEventsFor(listOf(UserTestData.userId)) }
        verify { alarmReceiverMock.setAlarm(contextMock) }
    }

    @Test
    fun `when event manager throws a network error, should not propagate the error to the caller`() =
        runTest(dispatchers.Main) {
            // given
            every { accountManagerMock.getPrimaryUserId() } returns flowOf(UserTestData.userId)
            coEvery {
                eventManagerMock.consumeEventsFor(listOf(UserTestData.userId))
            } throws IOException() andThenThrows mockk<ApiException>() andThenThrows mockk<HttpException>()

            // when/then
            fetchEventsAndReschedule()
            fetchEventsAndReschedule()
            fetchEventsAndReschedule()
    }
}
