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
package ch.protonmail.android.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.preference.PreferenceManager
import java.util.Locale

// region constants
const val PREF_CUSTOM_APP_LANGUAGE = "customAppLanguage"
// endregion

object CustomLocale {

    fun setLanguage(context: Context, language: String) {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putString(PREF_CUSTOM_APP_LANGUAGE, language).apply()
    }

    fun apply(context: Context): Context {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        return updateResources(context, preferences.getString(PREF_CUSTOM_APP_LANGUAGE, null) ?: "")
    }

    private fun updateResources(context: Context, locale: String): Context {

        var languageToSet = locale.substringBefore("_")
        var countryToSet = locale.substringAfter("_", "")

        if (locale == "") { // go back to default
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Resources.getSystem().configuration.locales.get(0)
            } else {
                Resources.getSystem().configuration.locale
            }.apply {
                languageToSet = language ?: "en"
                countryToSet = country ?: ""
            }
        }

        val localeToSet = Locale(languageToSet, countryToSet)
        Locale.setDefault(localeToSet)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        configuration.setLocale(localeToSet)
        val updatedContext = context.createConfigurationContext(configuration)
        return updatedContext ?: context
    }
}
