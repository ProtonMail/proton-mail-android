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

package ch.protonmail.android.pinlock.domain.usecase

import ch.protonmail.android.pinlock.domain.usecase.ShouldShowPinLockScreenTest.Parameters.Input
import ch.protonmail.android.pinlock.domain.usecase.ShouldShowPinLockScreenTest.Parameters.Output
import ch.protonmail.android.usecase.GetElapsedRealTimeMillis
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.toDuration

@RunWith(Parameterized::class)
class ShouldShowPinLockScreenTest(
    @Suppress("unused") private val testName: String,
    private val input: Input,
    private val output: Output
) {

    private val isPinLockEnabled: IsPinLockEnabled = mockk {
        coEvery { this@mockk() } returns input.isPinLockEnabled
    }
    private val getPinLockTimer: GetPinLockTimer = mockk {
        coEvery { this@mockk() } returns GetPinLockTimer.Result(input.pinLockTimer)
    }
    private val getElapsedRealTimeMillis: GetElapsedRealTimeMillis = mockk {
        every { this@mockk() } returns NOW_TIME
    }
    private val shouldShowPinLockScreen = ShouldShowPinLockScreen(
        isPinLockEnabled = isPinLockEnabled,
        getPinLockTimer = getPinLockTimer,
        getElapsedRealTimeMillis = getElapsedRealTimeMillis
    )

    @Test
    fun test() = runBlockingTest {
        // given - when
        val result = shouldShowPinLockScreen(
            wasAppInBackground = input.wasAppInBackground,
            isPinLockScreenShown = input.isPinLockScreenShown,
            isPinLockScreenOpen = input.isPinLockScreenOpen,
            isAddingAttachments = input.isAddingAttachments,
            lastForegroundTime = input.lastForegroundTime
        )

        // then
        assertEquals(output.result, result)
    }

    data class Parameters(
        val name: String,
        val input: Input,
        val output: Output
    ) {

        data class Input(
            val isPinLockEnabled: Boolean = true,
            val pinLockTimer: Duration = 5.toDuration(MINUTES),
            val wasAppInBackground: Boolean,
            val isPinLockScreenShown: Boolean,
            val isPinLockScreenOpen: Boolean,
            val isAddingAttachments: Boolean,
            val lastForegroundTime: Long
        )

        data class Output(val result: Boolean)
    }

    companion object Data {

        const val NOW_TIME = 100_000_000L
        const val SIX_MIN_AGO_TIME = NOW_TIME - 6 * 1_000 * 60

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun data() = listOf(

            Parameters(
                name = "returns *false* if app was not in background",
                Input(
                    wasAppInBackground = false,
                    isPinLockScreenShown = false,
                    isPinLockScreenOpen = false,
                    isAddingAttachments = false,
                    lastForegroundTime = SIX_MIN_AGO_TIME
                ),
                Output(result = false)
            ),

            Parameters(
                name = "returns *false* if a Pin screen is shown",
                Input(
                    wasAppInBackground = true,
                    isPinLockScreenShown = true,
                    isPinLockScreenOpen = false,
                    isAddingAttachments = false,
                    lastForegroundTime = SIX_MIN_AGO_TIME
                ),
                Output(result = false)
            ),

            Parameters(
                name = "returns *false* if is adding attachments and timer is Immediate",
                Input(
                    pinLockTimer = Duration.ZERO,
                    wasAppInBackground = true,
                    isPinLockScreenShown = false,
                    isPinLockScreenOpen = false,
                    isAddingAttachments = true,
                    lastForegroundTime = SIX_MIN_AGO_TIME
                ),
                Output(result = false)
            ),

            Parameters(
                name = "returns *true* if is adding attachments and timer is not Immediate",
                Input(
                    pinLockTimer = 5.toDuration(MINUTES),
                    wasAppInBackground = true,
                    isPinLockScreenShown = false,
                    isPinLockScreenOpen = false,
                    isAddingAttachments = true,
                    lastForegroundTime = SIX_MIN_AGO_TIME
                ),
                Output(result = true)
            ),

            Parameters(
                name = "returns *false* if Pin Lock is disabled",
                Input(
                    isPinLockEnabled = false,
                    wasAppInBackground = true,
                    isPinLockScreenShown = false,
                    isPinLockScreenOpen = false,
                    isAddingAttachments = false,
                    lastForegroundTime = SIX_MIN_AGO_TIME
                ),
                Output(result = false)
            ),

            Parameters(
                name = "returns *false* if Pin Lock timer is not expired",
                Input(
                    wasAppInBackground = true,
                    isPinLockScreenShown = false,
                    isPinLockScreenOpen = false,
                    isAddingAttachments = false,
                    lastForegroundTime = NOW_TIME
                ),
                Output(result = false)
            ),

            Parameters(
                name = "return *true* if app was in background, Pin Lock screen is not shown, Pin Lock is enabled " +
                    "and timer is expired",
                Input(
                    wasAppInBackground = true,
                    isPinLockScreenShown = false,
                    isPinLockScreenOpen = false,
                    isAddingAttachments = false,
                    lastForegroundTime = SIX_MIN_AGO_TIME
                ),
                Output(result = true)
            ),

            Parameters(
                name = "return *true* if app was not in background, but Pin Lock is the last screen shown",
                Input(
                    wasAppInBackground = false,
                    isPinLockScreenShown = false,
                    isPinLockScreenOpen = true,
                    isAddingAttachments = false,
                    lastForegroundTime = SIX_MIN_AGO_TIME
                ),
                Output(result = true)
            ),

            Parameters(
                name = "return *false* if Pin Lock is the last screen shown, but also the current screen",
                Input(
                    wasAppInBackground = false,
                    isPinLockScreenShown = true,
                    isPinLockScreenOpen = true,
                    isAddingAttachments = false,
                    lastForegroundTime = SIX_MIN_AGO_TIME
                ),
                Output(result = false)
            ),

        ).map { arrayOf(it.name, it.input, it.output) }
    }
}
