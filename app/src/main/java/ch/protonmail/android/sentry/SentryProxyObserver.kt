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

package ch.protonmail.android.sentry

import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import ch.protonmail.android.core.Constants.Prefs.PREF_USING_REGULAR_API
import ch.protonmail.android.di.DefaultSharedPreferences
import io.sentry.Sentry
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SentryProxyObserver @Inject constructor(
    @DefaultSharedPreferences
    private val defaultSharedPreferences: SharedPreferences,
) {

    private val preferencesObserver = OnSharedPreferenceChangeListener { sharedPreferences, key ->
        if (key == PREF_USING_REGULAR_API) {
            setSentryTag(sharedPreferences.usingProxy())
        }
    }

    fun start() {
        setSentryTag(defaultSharedPreferences.usingProxy())
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(preferencesObserver)
    }

    private fun SharedPreferences.usingProxy() = getBoolean(PREF_USING_REGULAR_API, true).not()

    private fun setSentryTag(usingProxy: Boolean) {
        Sentry.setTag(SWITCHED_TO_PROXY, usingProxy.toString())
    }

    private companion object {

        const val SWITCHED_TO_PROXY = "ON_PROXY"
    }
}
