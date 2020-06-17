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
    maven(url = "https://dl.bintray.com/proton/Core-publishing")
}

dependencies {
    val android =       "3.5.0"         // Released: Aug 08, 2019
    val easyGradle =    "1.5-beta-6"    // Released: Jun 17, 2020
    val protonGradle =  "0.1.4"         // Released: Jun 16, 2020

    // Needed for setup Android config
    implementation("com.android.tools.build:gradle:$android")
    // Needed for many utils
    implementation("studio.forface.easygradle:dsl-android:$easyGradle")
    implementation("me.proton.core:util-gradle:$protonGradle")
}
