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

import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.event.Event
import me.proton.core.domain.entity.UserId
import kotlin.test.Test
import kotlin.test.assertEquals

internal class SentryTreeTest {

    private val tree = SentryTree()

    @Test
    fun handleCorrectlyDetailedException() {
        mockkStatic(Sentry::class) {

            // given
            val exceptionMessage = "Something went wrong!"
            val logMessage = "ouch!"

            val expectedExtras = emptyMap<String, Any?>()

            val expectedLogMessage = "$logMessage\n" +
                "DetailedException(message=$exceptionMessage, " +
                "cause=java.lang.IllegalArgumentException: $exceptionMessage, " +
                "extras={})"

            every { Sentry.capture(any<Event>()) } returns Unit
            val exception = IllegalArgumentException(exceptionMessage)

            // when
            tree.w(
                exception.toDetailedException(),
                logMessage
            )

            // then
            val event = slot<Event>()
            verify {
                Sentry.capture(capture(event))
            }
            assertEquals(expectedExtras, event.captured.extra)
            assertEquals(expectedLogMessage, event.captured.message.take2Lines())
        }
    }

    @Test
    fun handleCorrectlyDetailedExceptionWithExtra() {
        mockkStatic(Sentry::class) {

            // given
            val exceptionMessage = "Something went wrong!"
            val logMessage = "ouch!"

            val userIdString = "user id"
            val errorCode = 404
            val errorMessage = "Not found!"

            val expectedExtras = mapOf(
                "User id" to userIdString,
                "API error code" to errorCode,
                "API error message" to errorMessage,
            )

            val expectedLogMessage = "$logMessage\n" +
                "DetailedException(message=$exceptionMessage, " +
                "cause=java.lang.IllegalArgumentException: $exceptionMessage, " +
                "extras={User id=$userIdString, API error code=$errorCode, API error message=$errorMessage})"

            every { Sentry.capture(any<Event>()) } returns Unit
            val exception = IllegalArgumentException(exceptionMessage)

            // when
            tree.w(
                exception.userId(UserId(userIdString)).apiError(errorCode, errorMessage),
                logMessage
            )

            // then
            val event = slot<Event>()
            verify {
                Sentry.capture(capture(event))
            }
            assertEquals(expectedExtras, event.captured.extra)
            assertEquals(expectedLogMessage, event.captured.message.take2Lines())
        }
    }

    @Test
    fun obfuscateEmailsWithOneEmail() {
        mockkStatic(Sentry::class) {

            // given
            val input = "Hello world! My email address is some.email@protonmail.ch. Have a nice day!!"
            val expectedLogMessage = "Hello world! My email address is *******ail@protonmail.ch. Have a nice day!!"

            // when
            tree.w(input)

            // then
            val event = slot<Event>()
            verify {
                Sentry.capture(capture(event))
            }
            assertEquals(expectedLogMessage, event.captured.message)
        }
    }

    @Test
    fun obfuscateEmailsWithManyEmails() {
        mockkStatic(Sentry::class) {

            // given
            val input = """
                Hello world! My email address is some.email@protonmail.ch. Have a nice day!!
                Oh! I forgot that I also have another email address which is another.email@protonmail.ch!!
                Have a nice day!
            """.trimIndent()

            val expectedLogMessage = """
                Hello world! My email address is *******ail@protonmail.ch. Have a nice day!!
                Oh! I forgot that I also have another email address which is **********ail@protonmail.ch!!
                Have a nice day!
            """.trimIndent()

            // when
            tree.w(input)

            // then
            val event = slot<Event>()
            verify {
                Sentry.capture(capture(event))
            }
            assertEquals(expectedLogMessage, event.captured.message)
        }
    }

    private fun String.take2Lines(): String =
        split("\n").take(2).joinToString(separator = "\n")
}
