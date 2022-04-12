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

package ch.protonmail.android.settings.data

import android.content.SharedPreferences
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.testdata.UserIdTestData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Test
import kotlin.test.assertEquals

internal class SharedPreferencesAccountSettingsRepositoryTest {

    private val userSharedPreferencesEditorMock = mockk<SharedPreferences.Editor>(relaxUnitFun = true) {
        every { putBoolean(any(), any()) } returns this
    }
    private val userPreferencesMock = mockk<SharedPreferences> {
        every { edit() } returns userSharedPreferencesEditorMock
    }
    private val secureSharedPreferencesFactoryMock = mockk<SecureSharedPreferences.Factory> {
        every { userPreferences(UserIdTestData.userId) } returns userPreferencesMock
    }
    private val sharedPreferencesAccountSettingsRepository = SharedPreferencesAccountSettingsRepository(
        secureSharedPreferencesFactoryMock,
        TestDispatcherProvider
    )

    @Test
    fun `should get the show link confirmation setting`() = runBlockingTest {
        // given
        val expectedSettingValue = true
        every { userPreferencesMock.getBoolean(PREF_HYPERLINK_CONFIRM, true) } returns expectedSettingValue

        // when
        val actualSettingValue = sharedPreferencesAccountSettingsRepository
            .getShouldShowLinkConfirmationSetting(UserIdTestData.userId)

        // then
        assertEquals(expectedSettingValue, actualSettingValue)
    }

    @Test
    fun `should save the show link confirmation setting`() = runBlockingTest {
        // given
        val expectedSettingValue = false

        // when
        sharedPreferencesAccountSettingsRepository
            .saveShouldShowLinkConfirmationSetting(expectedSettingValue, UserIdTestData.userId)

        // then
        verify { userSharedPreferencesEditorMock.putBoolean(PREF_HYPERLINK_CONFIRM, expectedSettingValue) }
        verify { userSharedPreferencesEditorMock.apply() }
    }

    private companion object TestData {

        const val PREF_HYPERLINK_CONFIRM = "confirmHyperlinks"
    }
}
