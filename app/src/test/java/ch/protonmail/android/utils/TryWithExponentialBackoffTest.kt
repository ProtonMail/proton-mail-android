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

package ch.protonmail.android.utils

import arrow.core.right
import ch.protonmail.android.utils.extensions.exponentialDelay
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

internal class TryWithExponentialBackoffTest {

    private val tryWithRetry = mockk<TryWithRetry>()
    private val tryWithExponentialBackoff = TryWithExponentialBackoff(tryWithRetry)

    @Test
    fun `should try with retry`() = runBlockingTest {
        // given
        mockkStatic(Duration::exponentialDelay)

        val shouldRetryOnError: (Exception) -> Boolean = { true }
        val shouldRetryOnResult: (String) -> Boolean = { true }
        val block: () -> String = { EXPECTED_RESULT }
        val beforeRetryMock = mockk<suspend (Int, String?) -> Unit> {
            coEvery { this@mockk(any(), any()) } just runs
        }
        val beforeRetrySlot = slot<suspend (Int, String?) -> Unit>()
        coEvery {
            tryWithRetry(
                NUMBER_OF_RETRIES,
                shouldRetryOnError,
                shouldRetryOnResult,
                capture(beforeRetrySlot),
                block
            )
        } returns EXPECTED_RESULT.right()

        // when
        val actualResult = tryWithExponentialBackoff(
            numberOfRetries = NUMBER_OF_RETRIES,
            backoffDuration = BACKOFF_DURATION,
            exponentialBase = EXPONENTIAL_BASE,
            shouldRetryOnError = shouldRetryOnError,
            shouldRetryOnResult = shouldRetryOnResult,
            beforeRetry = beforeRetryMock,
            block = block
        ).orNull()

        // then
        assertEquals(EXPECTED_RESULT, actualResult)
        beforeRetrySlot.captured(RETRY_COUNT, EXPECTED_RESULT)
        coVerify { beforeRetryMock(RETRY_COUNT, EXPECTED_RESULT) }
        verify { BACKOFF_DURATION.exponentialDelay(RETRY_COUNT, EXPONENTIAL_BASE) }

        unmockkStatic(Duration::exponentialDelay)
    }

    private companion object TestData {

        const val NUMBER_OF_RETRIES = 42
        val BACKOFF_DURATION = 10.milliseconds
        const val EXPECTED_RESULT = "result"
        const val RETRY_COUNT = 13
        const val EXPONENTIAL_BASE = 1.1
    }
}
