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

package ch.protonmail.android.settings.domain.usecase

import androidx.appcompat.app.AppCompatDelegate
import ch.protonmail.android.settings.domain.model.AppThemeSettings
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

class ApplyAppThemeFromSettingsTest {

    private val getAppThemeSettings: GetAppThemeSettings = mockk()
    private val applyTheme = ApplyAppThemeFromSettings(getAppThemeSettings)

    @BeforeTest
    fun setup() {
        mockkStatic(AppCompatDelegate::class)
        every { AppCompatDelegate.setDefaultNightMode(any()) } just Runs
    }

    @AfterTest
    fun teardown() {
        unmockkStatic(AppCompatDelegate::class)
    }

    @Test
    fun `dark theme is applied correctly`() = runBlockingTest {
        // given
        coEvery { getAppThemeSettings() } returns AppThemeSettings.DARK

        // when
        applyTheme()

        // then
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
    }

    @Test
    fun `light theme is applied correctly`() = runBlockingTest {
        // given
        coEvery { getAppThemeSettings() } returns AppThemeSettings.LIGHT

        // when
        applyTheme()

        // then
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
    }

    @Test
    fun `system theme is applied correctly`() = runBlockingTest {
        // given
        coEvery { getAppThemeSettings() } returns AppThemeSettings.FOLLOW_SYSTEM

        // when
        applyTheme()

        // then
        verify { AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) }
    }
}
