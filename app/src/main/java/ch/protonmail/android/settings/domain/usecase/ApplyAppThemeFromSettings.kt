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

import androidx.appcompat.app.AppCompatDelegate
import ch.protonmail.android.settings.domain.model.AppThemeSettings
import kotlinx.coroutines.runBlocking
import me.proton.core.util.kotlin.exhaustive
import javax.inject.Inject

/**
 * Apply theme to the app, using user's settings
 * @see AppThemeSettings
 */
class ApplyAppThemeFromSettings @Inject constructor(
    private val getAppThemeSettings: GetAppThemeSettings
) {

    suspend operator fun invoke() {
        val flag = when (getAppThemeSettings()) {
            AppThemeSettings.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            AppThemeSettings.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppThemeSettings.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }.exhaustive
        AppCompatDelegate.setDefaultNightMode(flag)
    }

    fun blocking() {
        runBlocking { this@ApplyAppThemeFromSettings() }
    }
}
