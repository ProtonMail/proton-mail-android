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
import me.proton.core.util.gradle.*

buildscript {
    initVersions()
    repositories(repos)
    dependencies(classpathDependencies)
}

plugins {
    `sonarQube`
}

allprojects {
    repositories(repos)
}

setupKotlin(
    "-XXLanguage:+NewInference",
    "-Xuse-experimental=kotlin.Experimental",
    "-XXLanguage:+InlineClasses",
    "-Xopt-in=kotlin.ExperimentalUnsignedTypes"
)
setupDetekt { "tokenAutoComplete" !in it.name }

tasks.register("clean", Delete::class.java) {
    delete(rootProject.buildDir)
}

tasks.register("injectLicenses") {
    description = "Add license header to source code files"

    apply(from = "scripts/inject_licenses/InjectLicenses.gradle.kts")
}
