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
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.version
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

val PluginDependenciesSpec.`android-application` get() =        plugin("com.android.application")
val PluginDependenciesSpec.`android-library` get() =            plugin("com.android.library")
val PluginDependenciesSpec.`google-services` get() =            plugin("com.google.gms.google-services")
val PluginDependenciesSpec.`hilt` get() =                       plugin("dagger.hilt.android.plugin")
val PluginDependenciesSpec.`java-library` get() =               plugin("java-library")
val PluginDependenciesSpec.`kotlin` get() =                     plugin("kotlin")
val PluginDependenciesSpec.`kotlin-android` get() =             kotlin("android")
val PluginDependenciesSpec.`kotlin-android-extensions` get() =  kotlin("android.extensions")
val PluginDependenciesSpec.`kotlin-kapt` get() =                kotlin("kapt")
val PluginDependenciesSpec.`kotlin-serialization` get() =       kotlin("plugin.serialization")
val PluginDependenciesSpec.`sonarQube` get() =                  plugin("org.sonarqube") version `sonarQube version`
val PluginDependenciesSpec.`browserstack` get() =               plugin("com.browserstack.gradle")

private fun PluginDependenciesSpec.plugin(id: String): PluginDependencySpec = id(id)
