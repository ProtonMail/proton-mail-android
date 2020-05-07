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
package ch.protonmail.android.prefs

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/**
 * This class will provide Preferences needed for the app, particularly:
 * @property sharedPreferences App level [SharedPreferences]
 * @property secureSharedPreferences App level [SecureSharedPreferences]
 * @property preferencesFor User level [SecureSharedPreferences] for each user
 *
 * This class is thread-safe
 *
 * @author Davide Farella
 */
class PreferencesProvider @Inject constructor(
    private val context: Context
) {

    private val usersPreferencesCache = ConcurrentHashMap<String, SecureSharedPreferences>()

    /** App level [SharedPreferences] */
    val sharedPreferences: SharedPreferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(context)
    }

    /** App level [SecureSharedPreferences] */
    val secureSharedPreferences: SecureSharedPreferences by lazy {
        SecureSharedPreferences(
            context,
            context.getSharedPreferences("ProtonMailSSP", Context.MODE_PRIVATE)
        )
    }

    /**
     * @return [SecureSharedPreferences] relative to the given user.
     * Values are cached in this [PreferencesProvider], use [clearPreferencesFor] for remove the cache
     */
    fun preferencesFor(username: String): SecureSharedPreferences =
        usersPreferencesCache.getOrPut(username) {
            val prefName = "${Base64.encodeToString(username.toByteArray(), Base64.NO_WRAP)}-SSP"

            SecureSharedPreferences(
                context,
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE)
            )
        }

    /** Clear Preferences cached for the given user */
    @Suppress("Unused") // Actually not used, but meant to be part of public API
    fun clearPreferencesFor(username: String) {
        usersPreferencesCache -= username
    }
}
