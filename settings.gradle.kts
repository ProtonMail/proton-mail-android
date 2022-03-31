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

rootProject.name = "ProtonMail"


val (projects, modules) = rootDir.projectsAndModules()

println("Projects: ${projects.sorted().joinToString()}")
println("Modules: ${modules.sorted().joinToString()}")

for (p in projects) includeBuild(p)
for (m in modules) include(m)

// Use core libs from maven artifacts or from git submodule using Gradle's included build:
// - to enable/disable locally: gradle.properties > useCoreGitSubmodule
// - to enable/disable on CI: .gitlab-ci.yml > ORG_GRADLE_PROJECT_useCoreGitSubmodule
val coreSubmoduleDir = rootDir.resolve("proton-libs")
extra.set("coreSubmoduleDir", coreSubmoduleDir)
val includeCoreLibsHelper = File(coreSubmoduleDir, "gradle/include-core-libs.gradle.kts")
if (includeCoreLibsHelper.exists()) {
    apply(from = "${coreSubmoduleDir.path}/gradle/include-core-libs.gradle.kts")
} else if (extensions.extraProperties["useCoreGitSubmodule"].toString().toBoolean()) {
    includeBuild("proton-libs")
    println("Core libs from git submodule `$coreSubmoduleDir`")
}

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
    }
}


fun File.projectsAndModules() : Pair<Set<String>, Set<String>> {
    val blacklist = setOf(
        ".git",
        ".gradle",
        ".idea",
        "buildSrc",
        "config",
        "build",
        "src",
        "proton-libs"
    )

    fun File.childrenDirectories() = listFiles { _, name -> name !in blacklist }!!
        .filter { it.isDirectory }

    fun File.isProject() =
        File(this, "settings.gradle.kts").exists() || File(this, "settings.gradle").exists()

    fun File.isModule() = !isProject() &&
        File(this, "build.gradle.kts").exists() || File(this, "build.gradle").exists()


    val modules = mutableSetOf<String>()
    val projects = mutableSetOf<String>()

    fun File.find(name: String? = null): List<File> = childrenDirectories().flatMap {
        val newName = (name ?: "") + it.name
        when {
            it.isProject() -> {
                projects += newName
                emptyList()
            }
            it.isModule() -> {
                modules += ":$newName"
                it.find("$newName:")
            }
            else -> it.find("$newName:")
        }
    }

    find()

    return projects to modules
}
