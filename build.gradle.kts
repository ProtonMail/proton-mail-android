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

buildscript {
    initVersions()
    repositories(repos)
    dependencies(classpathDependencies)
}

plugins {
    id("me.proton.kotlin") version "0.1" // Released: Oct 09, 2020
    id("me.proton.tests") version "0.1" // Released: Oct 09, 2020
    id("io.gitlab.arturbosch.detekt").version("1.15.0")
    `sonarQube`
}

allprojects {
    repositories(repos)
}

kotlinCompilerArgs(
    "-Xuse-experimental=kotlin.Experimental",
    // Enables unsigned types, like `UInt`, `ULong`, etc
    "-Xopt-in=kotlin.ExperimentalUnsignedTypes",
    // Enables experimental Coroutines from coroutines-test artifact, like `runBlockingTest`
    "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
    // Enables experimental kotlin.time
    "-Xopt-in=kotlin.time.ExperimentalTime"
)

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}
