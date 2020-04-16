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
import org.gradle.api.JavaVersion

/**
 * Params for the Application and various modules
 * @author Davide Farella
 */
object Project {
    const val appId = "ch.protonmail.android"
    const val versionName = "1.12.3"
    const val versionCode = 687 // jenkinsBuildNumber.toInteger()

    const val targetSdk = 28
    const val minSdk = 21
    val jdkVersion = JavaVersion.VERSION_1_8
}
