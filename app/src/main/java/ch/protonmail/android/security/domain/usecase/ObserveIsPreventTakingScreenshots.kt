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

package ch.protonmail.android.security.domain.usecase

import android.content.SharedPreferences
import ch.protonmail.android.core.Constants.Prefs.PREF_PREVENT_TAKING_SCREENSHOTS
import ch.protonmail.android.di.BackupSharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class ObserveIsPreventTakingScreenshots @Inject constructor(
    @BackupSharedPreferences
    private val preferences: SharedPreferences
) {

    operator fun invoke(): Flow<Boolean> = callbackFlow {
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
}
