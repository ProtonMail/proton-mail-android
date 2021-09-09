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

package ch.protonmail.android.compose.presentation.model

import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.utils.ServerTimeProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

private const val TIME_TO_EXPIRE_SECONDS = 4000L
private const val CURRENT_TIME_MILLIS = 6000L

class AddExpirationTimeToMessageTest {

    private val serverTimeProviderMock = mockk<ServerTimeProvider>() {
        every { currentTimeMillis() } returns CURRENT_TIME_MILLIS
    }
    private val addExpirationTimeToMessage = AddExpirationTimeToMessage(serverTimeProviderMock)

    @Test
    fun shouldAddExpirationTimeToMessageIfExpiresAfterIsPositive() {
        // given
        val message = Message()
        val expectedExpirationTime = TIME_TO_EXPIRE_SECONDS + CURRENT_TIME_MILLIS / 1000

        // when
        val messageWithExpirationTime = addExpirationTimeToMessage(message, TIME_TO_EXPIRE_SECONDS)

        // then
        assertEquals(expectedExpirationTime, messageWithExpirationTime.expirationTime)
    }

    @Test
    fun shouldNotAddExpirationTimeToMessageIfExpiresAfterIsZero() {
        // given
        val expectedExpirationTime = 0L
        val message = Message(expirationTime = expectedExpirationTime)

        // when
        val messageWithoutExpirationTime = addExpirationTimeToMessage(message, expiresAfterInSeconds = 0)

        // then
        assertEquals(expectedExpirationTime, messageWithoutExpirationTime.expirationTime)
    }
}
