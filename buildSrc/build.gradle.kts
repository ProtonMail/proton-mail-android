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
    val android =       "3.5.0"         // Released: Aug 08, 2019
    val detekt =        "1.9.1"         // Released: May 17, 2020
    val easyGradle =    "1.3.2"         // Released: May 22, 2020
    val kotlin =        "1.3.72"        // Released: Apr 14, 2020
    val sentry =        "1.7.22"        // Released:

    // Needed for setup Android config
    implementation("com.android.tools.build:gradle:$android")
    // Needed to setup Detekt config
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:$detekt")
    // Needed for many utils
    implementation("studio.forface.easygradle:dsl-android:$easyGradle")
    // Needed for setup Kotlin options
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin")


    implementation("io.sentry:sentry-android-gradle-plugin:$sentry")
}
