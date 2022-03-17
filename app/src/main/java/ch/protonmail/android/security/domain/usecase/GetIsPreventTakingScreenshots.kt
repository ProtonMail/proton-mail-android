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

package ch.protonmail.android.security.domain.usecase

import android.content.SharedPreferences
import ch.protonmail.android.core.Constants.Prefs.PREF_PREVENT_TAKING_SCREENSHOTS
import ch.protonmail.android.di.BackupSharedPreferences
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class GetIsPreventTakingScreenshots @Inject constructor(
    @BackupSharedPreferences
    private val preferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(): Boolean = withContext(dispatchers.Io) {
        blocking()
    }

    fun blocking(): Boolean =
        preferences.getInt(PREF_PREVENT_TAKING_SCREENSHOTS, 0) == 1
}
