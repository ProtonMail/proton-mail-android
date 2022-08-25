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

package ch.protonmail.android.utils.extensions

import ch.protonmail.android.testutils.extensions.asArray
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@RunWith(Enclosed::class)
internal class DurationExtensionsKtTest {

    @RunWith(Parameterized::class)
    class ExponentialDelayTest(private val testInput: TestInput) {

        @BeforeTest
        fun setUp() {
            mockkObject(Random.Default)
            every { Random.nextDouble() } returns testInput.randomDouble
        }

        @Test
        fun `check the exponential delay`() = with(testInput) {
            // when
            val actualDelay = currentDuration.exponentialDelay(retryCount, exponentialBase)

            // then
            assertEquals(actualDelay, expectedDelay)
        }

        @AfterTest
        fun tearDown() {
            unmockkObject(Random.Default)
        }

        companion object {

            @JvmStatic
            @Parameterized.Parameters
            fun data(): Collection<Array<Any>> {
                return listOf(
                    TestInput(
                        currentDuration = 0.milliseconds,
                        retryCount = 42,
                        exponentialBase = 2.0,
                        randomDouble = 0.2,
                        expectedDelay = 0.milliseconds
                    ).asArray(),
                    TestInput(
                        currentDuration = 42.milliseconds,
                        retryCount = 34,
                        exponentialBase = 1.0,
                        randomDouble = 0.2,
                        expectedDelay = 42.milliseconds
                    ).asArray(),
                    TestInput(
                        currentDuration = 10.milliseconds,
                        retryCount = 2,
                        exponentialBase = 2.0,
                        randomDouble = 0.5,
                        expectedDelay = 60.milliseconds
                    ).asArray(),
                    TestInput(
                        currentDuration = 2.milliseconds,
                        retryCount = 3,
                        exponentialBase = 1.2,
                        randomDouble = 0.1,
                        expectedDelay = 3.52512.milliseconds
                    ).asArray()
                )
            }
        }

        internal data class TestInput(
            val currentDuration: Duration,
            val retryCount: Int,
            val exponentialBase: Double,
            val randomDouble: Double,
            val expectedDelay: Duration
        )
    }
}
