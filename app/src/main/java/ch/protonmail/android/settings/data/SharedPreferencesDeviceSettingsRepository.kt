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
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.settings.domain.DeviceSettingsRepository
import ch.protonmail.android.settings.domain.model.AppThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.toInt
import javax.inject.Inject

const val PREF_APP_THEME = "Preferences.app.theme"
const val PREF_PREVENT_TAKING_SCREENSHOTS = "prevent_taking_screenshots"

class SharedPreferencesDeviceSettingsRepository @Inject constructor(
    @DefaultSharedPreferences private val preferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) : DeviceSettingsRepository {

    override suspend fun getAppThemeSettings(): AppThemeSettings =
        withContext(dispatchers.Io) {
            AppThemeSettings.fromIntOrDefault(preferences.getInt(PREF_APP_THEME, -1))
        }

    override suspend fun getIsPreventTakingScreenshots(): Boolean =
        withContext(dispatchers.Io) {
            preferences.getInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0) == 1
        }

    override fun observePreventTakingScreenshots(): Flow<Boolean> {
        TODO("Not yet implemented")
    }

    override suspend fun saveAppThemeSettings(settings: AppThemeSettings) {
        withContext(dispatchers.Io) {
            preferences[PREF_APP_THEME] = settings.int
        }
    }

    override suspend fun savePreventTakingScreenshots(shouldPrevent: Boolean) {
        withContext(dispatchers.Io) {
            preferences[PREF_PREVENT_TAKING_SCREENSHOTS] = shouldPrevent.toInt()
        }
    }
}
