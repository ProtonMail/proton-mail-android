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

plugins {
    `java-library`
    `kotlin`
}

dependencies {

    // Base dependencies
    implementation(
        // Proton
        `Proton-kotlin-util`,

        // Kotlin
        `kotlin-jdk8`,
        `coroutines-core`,
        `serialization`
    )

    // Test dependencies
    api(
        // Proton
        `Proton-kotlin-test`,

        // Kotlin
        `kotlin-test`,
        `kotlin-test-junit`,
        `coroutines-test`,

        // jUnit 5
        // (Required) Writing and executing Unit Tests on the JUnit Platform
        `jUnit5-jupiter-api`,
        // (Optional) If you need "Parameterized Tests"
        `jUnit5-jupiter-params`,

        // Other
        `assertJ`,
        `mockk`
    )

    // (Required) Writing and executing Unit Tests on the JUnit Platform
    testRuntimeOnly(`jUnit5-jupiter-engine`)
    // (Optional) If you also have JUnit 4-based tests
    testRuntimeOnly(`jUnit5-vintage-engine`)
}
