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
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

plugins {
    `android-library`
    `kotlin-android`
    `kotlin-serialization`
    `kotlin-kapt`
}

android()

dependencies {

    // Hilt
    kapt(
        `hilt-android-compiler`,
        `hilt-androidx-compiler`
    )

    implementation(

        // Core
        `Proton-kotlin-util`,
        `Proton-shared-preferences`,

        // Modules
        project(Module.domain),

        // Kotlin
        `kotlin-jdk8`,
        `coroutines-android`,
        `serialization-json`,

        // DI
        `hilt-android`,
        `hilt-androidx-annotations`
    )

    testImplementation(project(Module.testAndroid))
    androidTestImplementation(project(Module.testAndroidInstrumented))
}
