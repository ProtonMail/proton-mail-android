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
@file:Suppress("PackageDirectoryMismatch")

import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.initialization.dsl.ScriptHandler.CLASSPATH_CONFIGURATION
import org.gradle.kotlin.dsl.KotlinBuildScript
import org.gradle.kotlin.dsl.apply
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

/**
 * Gradle plugins for the project
 * @author Davide Farella
 */
object Plugin {
    val android =                   pPlugin("com.android.tools.build:gradle", V.android_gradle_plugin)
    val android_application =       mPlugin("com.android.application")
    val android_library =           mPlugin("com.android.library")

    val dokka =                     gPlugin("org.jetbrains.dokka:dokka-android-gradle-plugin", V.publishing_dokka_plugin, "dokka")
    val dokka_android =             mPlugin("dokka-android")

    val hugo =                      Hugo.gradlePlugin

    val java_library =              mPlugin("java-library")

    val kapt =                      mPlugin("kotlin-kapt")
    val sentry =                    Sentry.sentryGradlePlugin
    val detekt =                    pPlugin("io.gitlab.arturbosch.detekt:detekt-gradle-plugin", "1.5.0")
    val kotlin =                    Kotlin.gradlePlugin
    val kotlin_android =            Kotlin.androidGradlePlugin
    val kotlin_android_extensions = Kotlin.androidExtGradlePlugin
    val kotlin_serialization =      gPlugin("org.jetbrains.kotlin:kotlin-serialization", V.kotlin, "kotlinx-serialization")

    val sonarQube =                 gPlugin("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin", V.sonarQube, "org.sonarqube")
}

/** A plugin intended to be applied to the classpath of the Project */
interface ProjectPlugin {
    val path: String
    val version: String
}

/** A plugin intended to be applied of a single Module of the Project */
interface ModulePlugin {
    val id: String
    val version: String?
}

/** Implements [ProjectPlugin] and [ModulePlugin] */
interface GradlePlugin : ProjectPlugin, ModulePlugin

// region Constructor functions
/** @return [ProjectPlugin] */
@Suppress("unused") // restricted scope to Plugin object
internal fun Plugin.pPlugin(partialPath: String, version: String) =
        object : ProjectPlugin {
            override val path = "$partialPath:$version"
            override val version = version
        }

/** @return [ModulePlugin] */
@Suppress("unused") // restricted scope to Plugin object
internal fun Plugin.mPlugin(id: String, version: String? = null) =
        object : ModulePlugin {
            override val id = id
            override val version = version
        }

/** @return [GradlePlugin] */
@Suppress("unused") // restricted scope to Plugin object
internal fun Plugin.gPlugin(partialPath: String, version: String, id: String) =
        object : GradlePlugin {
            override val path = "$partialPath:$version"
            override val id = id
            override val version = version
        }
// endregion

// region Gradle helpers
/** Add given [ProjectPlugin] to the classpath */
internal fun DependencyHandler.addClasspath(plugin: ProjectPlugin) =
        add(CLASSPATH_CONFIGURATION, plugin.path)

/** Add the given [ModulePlugin] to a module */
fun PluginDependenciesSpec.apply(plugin: ModulePlugin, withVersion: Boolean = false): PluginDependencySpec {
    if (plugin.id.isEmpty())
        throw UnsupportedOperationException("This plugin is not supposed to be applied to a module")

    return id(plugin.id).apply { if (withVersion) version(plugin.version) }
}

/** Add the given [ModulePlugin]s to a module */
fun KotlinBuildScript.plugins(vararg plugins: ModulePlugin) {
    plugins.forEach {
        if (it.id.isEmpty())
            throw UnsupportedOperationException("This plugin is not supposed to be applied to a module")

        apply(plugin = it.id)
    }
}
// endregion
