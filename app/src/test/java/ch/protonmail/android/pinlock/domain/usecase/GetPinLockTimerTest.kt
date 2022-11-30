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

package ch.protonmail.android.pinlock.domain.usecase

import android.content.Context
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants.Prefs.PREF_AUTO_LOCK_PIN_PERIOD
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.test.android.mocks.mockSharedPreferences
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.android.sharedpreferences.set
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.DurationUnit.DAYS
import kotlin.time.DurationUnit.HOURS
import kotlin.time.DurationUnit.MINUTES
import kotlin.time.toDuration

class GetPinLockTimerTest {

    private val context: Context = mockk {
        every { resources.getIntArray(R.array.auto_logout_values) } returns intArrayOf(
            0,
            300_000,
            900_000,
            3_600_000,
            86_400_000
        )
    }
    private val dispatchers = TestDispatcherProvider()

    private val getPinLockTimer = GetPinLockTimer(
        context = context,
        preferences = mockSharedPreferences,
        dispatchers = dispatchers
    )

    @Test
    fun `correctly returns disabled timer`() = runTest(dispatchers.Main) {
        // given
        mockSharedPreferences[PREF_AUTO_LOCK_PIN_PERIOD] = -1
        val expected = Duration.INFINITE

        // when
        val result = getPinLockTimer().duration

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `correctly returns immediate lock timer`() = runTest(dispatchers.Main) {
        // given
        mockSharedPreferences[PREF_AUTO_LOCK_PIN_PERIOD] = 0
        val expected = Duration.ZERO

        // when
        val result = getPinLockTimer().duration

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `correctly returns timer for option 1`() = runTest(dispatchers.Main) {
        // given
        mockSharedPreferences[PREF_AUTO_LOCK_PIN_PERIOD] = 1
        val expected = 5.toDuration(MINUTES)

        // when
        val result = getPinLockTimer().duration

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `correctly returns timer for option 2`() = runTest(dispatchers.Main) {
        // given
        mockSharedPreferences[PREF_AUTO_LOCK_PIN_PERIOD] = 2
        val expected = 15.toDuration(MINUTES)

        // when
        val result = getPinLockTimer().duration

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `correctly returns timer for option 3`() = runTest(dispatchers.Main) {
        // given
        mockSharedPreferences[PREF_AUTO_LOCK_PIN_PERIOD] = 3
        val expected = 1.toDuration(HOURS)

        // when
        val result = getPinLockTimer().duration

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `correctly returns timer for option 4`() = runTest(dispatchers.Main) {
        // given
        mockSharedPreferences[PREF_AUTO_LOCK_PIN_PERIOD] = 4
        val expected = 1.toDuration(DAYS)

        // when
        val result = getPinLockTimer().duration

        // then
        assertEquals(expected, result)
    }
}
