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
package ch.protonmail.android.sentry

import ch.protonmail.android.core.apiError
import ch.protonmail.android.core.toDetailedException
import ch.protonmail.android.core.userId
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.verify
import io.sentry.Sentry
import io.sentry.SentryEvent
import io.sentry.protocol.SentryId
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

            val expectedLogMessage = "$logMessage\n" +
                "DetailedException(message=$exceptionMessage, " +
                "cause=java.lang.IllegalArgumentException: $exceptionMessage, " +
                "extras={})"

            every { Sentry.captureEvent(any()) } returns SentryId.EMPTY_ID
            val exception = IllegalArgumentException(exceptionMessage)

            // when
            tree.w(
                exception.toDetailedException(),
                logMessage
            )

            // then
            val event = slot<SentryEvent>()
            verify {
                Sentry.captureEvent(capture(event))
            }
            assertEquals(expectedLogMessage, event.captured.message?.message?.take2Lines())
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
                KEY_USER_ID to userIdString,
                KEY_CODE to errorCode,
                KEY_MESSAGE to errorMessage,
            )

            val expectedLogMessage = "$logMessage\n" +
                "DetailedException(message=$exceptionMessage, " +
                "cause=java.lang.IllegalArgumentException: $exceptionMessage, " +
                "extras={User id=$userIdString, API error code=$errorCode, API error message=$errorMessage})"

            every { Sentry.captureEvent(any()) } returns SentryId.EMPTY_ID
            val exception = IllegalArgumentException(exceptionMessage)

            // when
            tree.w(
                exception.userId(UserId(userIdString)).apiError(errorCode, errorMessage),
                logMessage
            )

            // then
            val event = slot<SentryEvent>()
            verify {
                Sentry.captureEvent(capture(event))
            }
            assertEquals(expectedExtras[KEY_USER_ID], event.captured.getExtra(KEY_USER_ID))
            assertEquals(expectedExtras[KEY_CODE], event.captured.getExtra(KEY_CODE))
            assertEquals(expectedExtras[KEY_MESSAGE], event.captured.getExtra(KEY_MESSAGE))
            assertEquals(expectedLogMessage, event.captured.message?.message?.take2Lines())
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
            val event = slot<SentryEvent>()
            verify {
                Sentry.captureEvent(capture(event))
            }
            assertEquals(expectedLogMessage, event.captured.message?.message)
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
            val event = slot<SentryEvent>()
            verify {
                Sentry.captureEvent(capture(event))
            }
            assertEquals(expectedLogMessage, event.captured.message?.message)
        }
    }

    private fun String.take2Lines(): String =
        split("\n").take(2).joinToString(separator = "\n")

    companion object {
        private const val KEY_USER_ID = "User id"
        private const val KEY_CODE = "API error code"
        private const val KEY_MESSAGE = "API error message"
    }
}
