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
package ch.protonmail.android

/**
 * Using toggle ( Boolean ) for trigger events for specific app version is a bad pattern, a better
 * pattern would be to store the previous app version in SharedPreferences and, if needed store
 * the ( or a list of ) version where the specific action needs to be triggered.
 *
 * Examples:
 *
 * ###### Trigger on current versions
 * ```kotlin
 *  // Gradle file
 *  buildConfigField("int[]", "CLEAN_CONTACTS_VERSIONS", "{ 710; 724; 750 }")
 *
 *  // Code
 *  if (BuildConfig.VERSION_CODE in BuildConfig.CLEAN_CONTACTS_VERSIONS) {
 *      cleanContactsTable()
 *  }
 * ```
 *
 * ###### Trigger on previous versions
 * ```kotlin
 *  // Gradle file
 *  buildConfigField("int[]", "CLEAN_CONTACTS_PREVIOUS_VERSIONS", "{ 710; 724; 750 }")
 *
 *  // Code
 *  val previousInstalledVersion: Int = TO\\DO("Get previous installed version from preferences")
 *  if (previousInstalledVersion in BuildConfig.CLEAN_CONTACTS_PREVIOUS_VERSIONS) {
 *      cleanContactsTable()
 *      saveNewVersionToPreferences(BuildConfig.VERSION_CODE)
 *  }
 * ```
 */
const val MIGRATE_FROM_BUILD_CONFIG_FIELD_DOC = 0
