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

package ch.protonmail.android.settings.presentation.viewmodel

import app.cash.turbine.test
import ch.protonmail.android.settings.domain.model.AppThemeSettings
import ch.protonmail.android.settings.domain.usecase.ApplyAppThemeFromSettings
import ch.protonmail.android.settings.domain.usecase.GetAppThemeSettings
import ch.protonmail.android.settings.domain.usecase.SaveAppThemeSettings
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeChooserViewModelTest :
    CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

    private val getAppThemeSettings: GetAppThemeSettings = mockk()
    private val saveAppThemeSettings: SaveAppThemeSettings = mockk(relaxUnitFun = true)
    private val applyAppThemeFromSettings: ApplyAppThemeFromSettings = mockk(relaxUnitFun = true)

    private val viewModel by lazy {
        ThemeChooserViewModel(
            getAppThemeSettings = getAppThemeSettings,
            saveAppThemeSettings = saveAppThemeSettings,
            applyAppThemeFromSettings = applyAppThemeFromSettings
        )
    }

    @Test
    fun `correct theme is emitted`() = coroutinesTest {
        // given
        val theme = AppThemeSettings.DARK
        val expectedState = ThemeChooserViewModel.State.Data(theme)
        coEvery { getAppThemeSettings() } returns theme

        // when
        viewModel.state.test {

            // then
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `saves correct theme`() = coroutinesTest {
        // given
        val expectedTheme = AppThemeSettings.FOLLOW_SYSTEM

        // when
        viewModel.process(ThemeChooserViewModel.Action.SetSystemTheme)

        // then
        coVerify { saveAppThemeSettings(expectedTheme) }
    }

    @Test
    fun `correctly requests to apply the theme`() = coroutinesTest {
        // when
        viewModel.process(ThemeChooserViewModel.Action.SetLightTheme)

        coVerify { applyAppThemeFromSettings() }
    }

}
