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

package ch.protonmail.android.pinlock.domain.usecase

import ch.protonmail.android.usecase.GetElapsedRealTimeMillis
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.toDuration

class ShouldShowPinLockScreenTest {

    private val isPinLockEnabled: IsPinLockEnabled = mockk {
        coEvery { this@mockk() } returns true
    }
    private val getPinLockTimer: GetPinLockTimer = mockk {
        coEvery { this@mockk() } returns GetPinLockTimer.Result(5.toDuration(MINUTES))
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
    fun `returns false if app was not in background`() = runBlockingTest {
        // given - when
        val result = shouldShowPinLockScreen(
            wasAppInBackground = false,
            isPinLockScreenShown = false,
            isAddingAttachments = false,
            lastForegroundTime = SIX_MIN_AGO_TIME
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `returns false if a Pin screen is shown`() = runBlockingTest {
        // given - when
        val result = shouldShowPinLockScreen(
            wasAppInBackground = true,
            isPinLockScreenShown = true,
            isAddingAttachments = false,
            lastForegroundTime = SIX_MIN_AGO_TIME
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `returns false if is adding attachments and timer is Immediate`() = runBlockingTest {
        // given - when
        coEvery { getPinLockTimer() } returns GetPinLockTimer.Result(Duration.ZERO)

        val result = shouldShowPinLockScreen(
            wasAppInBackground = true,
            isPinLockScreenShown = false,
            isAddingAttachments = true,
            lastForegroundTime = SIX_MIN_AGO_TIME
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `returns true if is adding attachments and timer is not Immediate`() = runBlockingTest {
        // given - when
        coEvery { getPinLockTimer() } returns GetPinLockTimer.Result(5.toDuration(MINUTES))

        val result = shouldShowPinLockScreen(
            wasAppInBackground = true,
            isPinLockScreenShown = false,
            isAddingAttachments = true,
            lastForegroundTime = SIX_MIN_AGO_TIME
        )

        // then
        assertTrue(result)
    }

    @Test
    fun `returns false if Pin Lock is disabled`() = runBlockingTest {
        // given - when
        coEvery { isPinLockEnabled() } returns false

        val result = shouldShowPinLockScreen(
            wasAppInBackground = true,
            isPinLockScreenShown = false,
            isAddingAttachments = false,
            lastForegroundTime = SIX_MIN_AGO_TIME
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `returns false if Pin Lock timer is not expired`() = runBlockingTest {
        // given - when
        val result = shouldShowPinLockScreen(
            wasAppInBackground = true,
            isPinLockScreenShown = false,
            isAddingAttachments = false,
            lastForegroundTime = NOW_TIME
        )

        // then
        assertFalse(result)
    }

    @Test
    fun `return true if app was in background, Pin Lock screen is not shown, Pin Lock is enabled and timer is expired`() {
        runBlockingTest {
            // given - when
            val result = shouldShowPinLockScreen(
                wasAppInBackground = true,
                isPinLockScreenShown = false,
                isAddingAttachments = false,
                lastForegroundTime = SIX_MIN_AGO_TIME
            )

            // then
            assertTrue(result)
        }
    }

    companion object Data {

        const val NOW_TIME = 100_000_000L
        const val SIX_MIN_AGO_TIME = NOW_TIME - 6 * 1_000 * 60
    }
}
