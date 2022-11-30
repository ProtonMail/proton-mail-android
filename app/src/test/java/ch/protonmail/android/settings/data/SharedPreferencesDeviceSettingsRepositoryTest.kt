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

package ch.protonmail.android.settings.data

import ch.protonmail.android.settings.domain.model.AppThemeSettings
import io.mockk.every
import kotlinx.coroutines.test.runTest
import me.proton.core.test.android.mocks.newMockSharedPreferences
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.android.sharedpreferences.get
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
class SharedPreferencesDeviceSettingsRepositoryTest {

    @RunWith(Parameterized::class)
    class AppTheme(
        @Suppress("unused") private val testName: String,
        private val preferencesValue: Int,
        private val domainValue: AppThemeSettings,
        private val shouldSave: Boolean
    ) {

        private val preferences = newMockSharedPreferences
        private val dispatchers = TestDispatcherProvider()
        private val repository = SharedPreferencesDeviceSettingsRepository(
            preferences = preferences,
            dispatchers = dispatchers
        )

        @Test
        fun get() = runTest(dispatchers.Main) {
            every { preferences.getInt(PREF_APP_THEME, any()) } returns preferencesValue
            assertEquals(domainValue, repository.getAppThemeSettings())
        }

        @Test()
        fun save() = runTest(dispatchers.Main) {
            if (shouldSave.not()) return@runTest

            repository.saveAppThemeSettings(domainValue)
            assertEquals(preferencesValue, preferences[PREF_APP_THEME])
        }

        data class Parameters(
            val testName: String,
            val preferencesValue: Int,
            val domainValue: AppThemeSettings,
            val shouldSave: Boolean = true
        )

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "AppTheme: {0}")
            fun data() = listOf(

                Parameters(
                    testName = "follow system",
                    preferencesValue = 0,
                    domainValue = AppThemeSettings.FOLLOW_SYSTEM
                ),

                Parameters(
                    testName = "light",
                    preferencesValue = 1,
                    domainValue = AppThemeSettings.LIGHT
                ),

                Parameters(
                    testName = "dark",
                    preferencesValue = 2,
                    domainValue = AppThemeSettings.DARK
                ),

                Parameters(
                    testName = "unknown value",
                    preferencesValue = 15,
                    domainValue = AppThemeSettings.FOLLOW_SYSTEM,
                    shouldSave = false
                )

            ).map { arrayOf(it.testName, it.preferencesValue, it.domainValue, it.shouldSave) }
        }
    }

    @RunWith(Parameterized::class)
    class PreventTakingScreenshots(
        @Suppress("unused") private val testName: String,
        private val preferencesValue: Int,
        private val domainValue: Boolean,
        private val shouldSave: Boolean
    ) {

        private val preferences = newMockSharedPreferences
        private val dispatchers = TestDispatcherProvider()
        private val repository = SharedPreferencesDeviceSettingsRepository(
            preferences = preferences,
            dispatchers = dispatchers
        )

        @Test
        fun get() = runTest(dispatchers.Main) {
            every { preferences.getInt(PREF_PREVENT_TAKING_SCREENSHOTS, any()) } returns preferencesValue
            assertEquals(domainValue, repository.getIsPreventTakingScreenshots())
        }

        @Test
        fun save() = runTest(dispatchers.Main) {
            if (shouldSave.not()) return@runTest

            repository.savePreventTakingScreenshots(domainValue)
            assertEquals(preferencesValue, preferences[PREF_PREVENT_TAKING_SCREENSHOTS])
        }

        data class Parameters(
            val testName: String,
            val preferencesValue: Int,
            val domainValue: Boolean,
            val shouldSave: Boolean = true
        )

        companion object {

            @JvmStatic
            @Parameterized.Parameters(name = "PreventTakingScreenshots: {0}")
            fun data() = listOf(

                Parameters(
                    testName = "prevent",
                    preferencesValue = 1,
                    domainValue = true
                ),

                Parameters(
                    testName = "not prevent",
                    preferencesValue = 0,
                    domainValue = false
                ),

                Parameters(
                    testName = "handle unknown value",
                    preferencesValue = 15,
                    domainValue = false,
                    shouldSave = false
                )

            ).map { arrayOf(it.testName, it.preferencesValue, it.domainValue, it.shouldSave) }
        }
    }
}
