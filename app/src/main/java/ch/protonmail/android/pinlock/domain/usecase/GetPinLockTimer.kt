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

package ch.protonmail.android.pinlock.domain.usecase

import android.content.Context
import android.content.SharedPreferences
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants.Prefs.PREF_AUTO_LOCK_PIN_PERIOD
import ch.protonmail.android.di.BackupSharedPreferences
import kotlinx.coroutines.withContext
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.DurationUnit.MILLISECONDS
import kotlin.time.toDuration

class GetPinLockTimer @Inject constructor(
    context: Context,
    @BackupSharedPreferences
    private val preferences: SharedPreferences,
    private val dispatchers: DispatcherProvider
) {

    private val rawValues: IntArray by lazy {
        context.resources.getIntArray(R.array.auto_logout_values)
    }

    suspend operator fun invoke(): Result = withContext(dispatchers.Io) {
        preferences.get(PREF_AUTO_LOCK_PIN_PERIOD, -1)
            .takeIf { it >= 0 }
            ?.let { option -> rawValues[option].toDuration(MILLISECONDS).let(::Result) }
            ?: Duration.INFINITE.let(::Result)
    }

    // Wrap Duration for mocking
    data class Result(val duration: Duration)
}
