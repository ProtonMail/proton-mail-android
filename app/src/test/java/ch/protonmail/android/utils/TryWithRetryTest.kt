/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.utils

import ch.protonmail.android.domain.util.leftOrNull
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class TryWithRetryTest {

    private val block = mockk<() -> Int>()
    private val tryWithRetry = TryWithRetry()

    @Test
    fun `should return the result when block returns`() = runBlockingTest {
        // given
        val expectedResult = 42
        every { block() } returns expectedResult

        // when
        val actualResult = tryWithRetry { block() }.orNull()

        // then
        assertEquals(expectedResult, actualResult)
    }

    @Test
    fun `should return errors when all retries fail`() = runBlockingTest {
        // given
        every { block() } throws IOException()

        // when
        val actualResult = tryWithRetry { block() }.leftOrNull()

        // then
        assertEquals(3, actualResult?.size)
        assertTrue(actualResult!!.all { it is IOException })
    }

    @Test
    fun `should try the provided number of times before failing`() = runBlockingTest {
        // given
        val expectedNumberOfRetries = 42
        every { block() } throws IOException()

        // when
        tryWithRetry(numberOfRetries = expectedNumberOfRetries) { block() }

        // then
        verify(exactly = expectedNumberOfRetries) { block() }
    }
}
