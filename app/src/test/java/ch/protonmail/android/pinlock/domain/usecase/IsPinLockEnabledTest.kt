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

import ch.protonmail.android.core.Constants.Prefs.PREF_USE_PIN
import kotlinx.coroutines.test.runTest
import me.proton.core.test.android.mocks.mockSharedPreferences
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.android.sharedpreferences.set
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IsPinLockEnabledTest {

    private val dispatchers = TestDispatcherProvider()
    private val isPinLockEnabled = IsPinLockEnabled(mockSharedPreferences, dispatchers)

    @Test
    fun `returns true if pin lock is enabled in preferences`() = runTest(dispatchers.Main) {
        // given
        mockSharedPreferences[PREF_USE_PIN] = true

        // when
        val result = isPinLockEnabled()

        // then
        assertTrue(result)
    }

    @Test
    fun `returns false if pin lock is not enabled in preferences`() = runTest(dispatchers.Main) {
        // given
        mockSharedPreferences[PREF_USE_PIN] = false

        // when
        val result = isPinLockEnabled()

        // then
        assertFalse(result)
    }
}
