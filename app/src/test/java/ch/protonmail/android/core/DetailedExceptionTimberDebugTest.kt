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

package ch.protonmail.android.core

import android.util.Log
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [DetailedException] in [Timber.DebugTree]
 */
class DetailedExceptionTimberDebugTest {

    private val tree = Timber.DebugTree()

    @Test
    @Ignore(
        "Test are running correctly but fails on CI for unknown reasons. We spent enough time trying to " +
            "understand ( including trying to run them with different Java versions ), without any result"
    )
    fun handleCorrectlyDetailedException() {
        mockkStatic(Log::class) {

            // given
            val exceptionMessage = "Something went wrong!"
            val logMessage = "ouch!"

            val expectedLogMessage = "$logMessage\n" +
                "DetailedException(message=$exceptionMessage, " +
                "cause=java.lang.IllegalArgumentException: $exceptionMessage, extras={})"

            every { Log.println(any(), any(), any()) } returns 0
            val exception = IllegalArgumentException(exceptionMessage)

            // when
            tree.d(exception.toDetailedException(), logMessage)

            // then
            val message = slot<String>()
            verify {
                Log.println(Log.DEBUG, any(), capture(message))
            }
            assertEquals(expectedLogMessage, message.captured.take2Lines())
        }
    }

    @Test
    @Ignore(
        "Test are running correctly but fails on CI for unknown reasons. We spent enough time trying to " +
            "understand ( including trying to run them with different Java versions ), without any result"
    )
    fun handleCorrectlyDetailedExceptionWithExtra() {
        mockkStatic(Log::class) {

            // given
            val exceptionMessage = "Something went wrong!"
            val logMessage = "ouch!"

            val userIdString = "user id"
            val errorCode = 404
            val errorMessage = "Not found!"

            val expectedLogMessage = "$logMessage\n" +
                "DetailedException(message=$exceptionMessage, " +
                "cause=java.lang.IllegalArgumentException: $exceptionMessage, " +
                "extras={User id=$userIdString, API error code=$errorCode, API error message=$errorMessage})"

            every { Log.println(any(), any(), any()) } returns 0
            val exception = IllegalArgumentException(exceptionMessage)

            // when
            tree.d(
                exception.userId(UserId(userIdString)).apiError(errorCode, errorMessage),
                logMessage
            )

            // then
            val message = slot<String>()
            verify {
                Log.println(Log.DEBUG, any(), capture(message))
            }
            assertEquals(expectedLogMessage, message.captured.take2Lines())
        }
    }

    private fun String.take2Lines(): String =
        split("\n").take(2).joinToString(separator = "\n")
}
