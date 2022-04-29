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
        `material`,
        `lifecycle-runtime`,
        `lifecycle-liveData`,
        `lifecycle-viewModel`,
        `android-ktx`,

        // TestRail
         `json-simple`
    )

    // Test dependencies
    api(
        project(Module.testAndroid) exclude
            // Exclude Assert4k since backtick names are not supported in Android test
            `assert4k` exclude
            // Exclude MockK since we will use MockK-Android
            `mockk` exclude
            // Exclude Robolectric since not needed for instrumented tests
            `robolectric`,

        `Proton-android-instrumented-test` exclude
            // Exclude MockK since we will use MockK-Android
            `mockk` exclude
            // Exclude Robolectric since not needed for instrumented tests
            `robolectric`,

        // MockK
        `mockk-android`,

        // Android
        `android-annotation`,
        `android-test-core-ktx`,
        `android-test-junit`,
        `android-test-runner`,
        `android-test-rules`,
        `android-work-testing`,
        `espresso`,
        `espresso-contrib`,
        `espresso-intents`,
        `hamcrest`,
        `json-simple`
    )
}
