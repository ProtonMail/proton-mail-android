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

package ch.protonmail.android.settings.domain.usecase

import ch.protonmail.android.settings.domain.DeviceSettingsRepository
import ch.protonmail.android.settings.domain.model.AppThemeSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import kotlin.test.Test
import kotlin.test.assertEquals

class GetAppThemeSettingsTest {

    private val repository: DeviceSettingsRepository = mockk()
    private val getAppThemeSettings = GetAppThemeSettings(repository)

    @Test
    fun `returns correct data from the repository`() = runBlockingTest {
        // given
        val expected = AppThemeSettings.DARK
        coEvery { repository.getAppThemeSettings() } returns expected

        // when
        val result = getAppThemeSettings()

        // then
        assertEquals(expected, result)
    }
}
