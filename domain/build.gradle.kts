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

plugins {
    `java-library`
    `kotlin`
    `kotlin-serialization`
}

dependencies {
    implementation(

        // Core
        `Proton-kotlin-util`,
        `Proton-domain`,
        `Proton-user-domain`,

        // Kotlin
        `coroutines-core`,
        `serialization-json`,

        // Arrow
        `arrow-core`,

        // DI
        `dagger`
    )

    testImplementation(project(Module.testKotlin))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = freeCompilerArgs +
            // Allow to use kotlin.Result as return type
            "-Xallow-result-return-type"
        jvmTarget = ProtonMail.jvmTarget.toString()
    }
}
