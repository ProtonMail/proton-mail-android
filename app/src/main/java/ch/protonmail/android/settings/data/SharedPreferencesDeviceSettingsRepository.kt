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

import android.content.SharedPreferences
import ch.protonmail.android.di.DefaultSharedPreferences
import ch.protonmail.android.settings.domain.DeviceSettingsRepository
import ch.protonmail.android.settings.domain.model.AppThemeSettings
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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
            isPreventTakingScreenshots()
        }

    override fun observeIsPreventTakingScreenshots(): Flow<Boolean> = callbackFlow {
        send(isPreventTakingScreenshots())

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == PREF_PREVENT_TAKING_SCREENSHOTS) {
                trySend(isPreventTakingScreenshots())
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun isPreventTakingScreenshots() =
        preferences.getInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0) == 1

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
