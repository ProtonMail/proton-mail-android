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
plugins {
    `kotlin-dsl`
}

repositories {
    google()
    jcenter()
}

dependencies {
    val android =       "3.5.0"         // Updated: Aug 08, 2019
    val easyGradle =    "1.2.3-beta-4"  // Updated: Mar 01, 2020
    val sentry =        "1.7.22"        // Updated:

    implementation("com.android.tools.build:gradle:$android")
    implementation("studio.forface.easygradle:dsl-android:$easyGradle")
    implementation("io.sentry:sentry-android-gradle-plugin:$sentry")
}
