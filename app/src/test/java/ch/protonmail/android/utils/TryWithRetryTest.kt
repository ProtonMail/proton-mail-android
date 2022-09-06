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

import ch.protonmail.android.domain.util.leftOrNull
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals

internal class TryWithRetryTest {

    private val block = mockk<() -> String>()
    private val beforeRetry = mockk<(Int) -> Unit> {
        every { this@mockk(any()) } just runs
    }
    private val tryWithRetry = TryWithRetry()

    @Test
    fun `should return the result when block returns and not call the before-retry block`() = runBlockingTest {
        // given
        val expectedResult = "great success"
        every { block() } returns expectedResult

        // when
        val actualResult = tryWithRetry(
            numberOfRetries = NUMBER_OF_RETRIES,
            beforeRetry = beforeRetry,
            block = block
        ).orNull()

        // then
        assertEquals(expectedResult, actualResult)
        verify(exactly = 0) { beforeRetry(any()) }
    }

    @Test
    fun `should return error when all retries fail`() = runBlockingTest {
        // given
        val expectedError = IOException("Nah")
        every { block() } throws expectedError

        // when
        val actualResult = tryWithRetry { block() }.leftOrNull()

        // then
        assertEquals(expectedError, actualResult)
    }

    @Test
    fun `should try the expected number of times before failing`() = runBlockingTest {
        // given
        val expectedNumberOfBlockCalls = NUMBER_OF_RETRIES + 1
        every { block() } throws IOException()

        // when
        tryWithRetry(numberOfRetries = NUMBER_OF_RETRIES) { block() }

        // then
        verify(exactly = expectedNumberOfBlockCalls) { block() }
    }

    @Test
    fun `should call the before-retry block before each retry passing the current retry count`() = runBlockingTest {
        // given
        every { block() } throws IOException()

        // when
        tryWithRetry(
            numberOfRetries = NUMBER_OF_RETRIES,
            beforeRetry = beforeRetry
        ) { block() }

        // then
        val capturedRetryCounts = mutableListOf<Int>()
        verify(exactly = NUMBER_OF_RETRIES) { beforeRetry(capture(capturedRetryCounts)) }
        capturedRetryCounts.forEachIndexed { index, capturedRetryCount ->
            assertEquals(index, capturedRetryCount)
        }
    }

    @Test
    fun `should retry as long as the error is retryable`() = runBlockingTest {
        // given
        val isErrorRetryable: (Exception) -> Boolean = { it is IOException }
        every { block() } throws IOException() andThenThrows IOException() andThenThrows IllegalStateException()

        // when
        tryWithRetry(
            numberOfRetries = NUMBER_OF_RETRIES,
            beforeRetry = beforeRetry,
            shouldRetryOnError = isErrorRetryable
        ) { block() }

        // then
        verify(exactly = 3) { block() }
        verify(exactly = 2) { beforeRetry(any()) }
    }

    @Test
    fun `should retry as long as the result is retryable`() = runBlockingTest {
        // given
        val expectedResult = "123456"
        val isResultRetryable: (String) -> Boolean = { result -> result.length < 6 }
        every { block() } returns "123" andThen "1234" andThen "12345" andThen expectedResult andThen "1234567"

        // when
        val actualResult = tryWithRetry(
            numberOfRetries = NUMBER_OF_RETRIES,
            beforeRetry = beforeRetry,
            shouldRetryOnError = { true },
            shouldRetryOnResult = isResultRetryable
        ) { block() }.orNull()

        // then
        assertEquals(expectedResult, actualResult)
        verify(exactly = 4) { block() }
        verify(exactly = 3) { beforeRetry(any()) }
    }

    private companion object {

        const val NUMBER_OF_RETRIES = 42
    }
}
