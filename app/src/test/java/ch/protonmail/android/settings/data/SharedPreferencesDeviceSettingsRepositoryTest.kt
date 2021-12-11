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

package ch.protonmail.android.settings.data

import ch.protonmail.android.settings.domain.model.AppThemeSettings
import io.mockk.every
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.mocks.newMockSharedPreferences
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedPreferencesDeviceSettingsRepositoryTest {

    private val preferences = newMockSharedPreferences
    private val repository = SharedPreferencesDeviceSettingsRepository(
        preferences = preferences,
        dispatchers = TestDispatcherProvider
    )

    @Test
    fun `get correct App Theme`() = runBlockingTest {
        // given
        val expected = AppThemeSettings.DARK
        every { preferences.getInt(PREF_APP_THEME, any()) } returns expected.int

        // when
        val result = repository.getAppThemeSettings()

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `get default App Theme if none is store`() = runBlockingTest {
        // given
        val expected = AppThemeSettings.FOLLOW_SYSTEM
        every { preferences.getInt(PREF_APP_THEME, any()) } returns -1

        // when
        val result = repository.getAppThemeSettings()

        // then
        assertEquals(expected, result)
    }
}
