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
    `kotlin-android-extensions`
}

android()

dependencies {

    // Base dependencies
    implementation(
        // Proton
        `Proton-kotlin-util`,

        // Kotlin
        `kotlin-jdk7`,
        `kotlin-reflect`,
        `coroutines-android`,

        // Android
        `lifecycle-runtime`,
        `lifecycle-liveData`,
        `lifecycle-viewModel`,

        //TestRail
         `jsonsimple`
    )

    // Test dependencies
    api(
        project(Module.testAndroid) exclude
            // Exclude Assert4k since backtick names are not supported in Android test
            `assert4k` exclude
            // Exclude MockK since we will use MockK-Android
            `mockk` exclude
            // Exclude JUnit 5 since we will use JUnit 4 on instrumented tests
            jUnit5(`any`, `any`) exclude
            // Exclude Robolectric since not needed for instrumented tests
            `robolectric`,

        `Proton-android-instrumented-test` exclude
            // Exclude MockK since we will use MockK-Android
            `mockk`,

        // MockK
        `mockk-android`,

        // Android
        `android-annotation`,
        `android-test-core`,
        `android-test-runner`,
        `android-test-rules`,
        `espresso`,
        `hamcrest`,
        `jsonsimple`
    )
}
