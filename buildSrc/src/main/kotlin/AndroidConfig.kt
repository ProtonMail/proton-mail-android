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
import com.android.build.gradle.TestedExtension
import com.android.build.gradle.internal.dsl.DefaultConfig

/**
 * Apply the default Android configuration for `app` module
 *
 * @param extraConfig [ExtraConfig] block that will be applied into the `android` closure
 *
 * @param extraDefaultConfig [ExtraDefaultConfig] block that will be applied into the
 * `android.defaultConfig` closure
 */
fun TestedExtension.configApp(
        appId: String = Project.appId,
        minSdk: Int = Project.minSdk,
        targetSdk: Int = Project.targetSdk,
        extraConfig: ExtraConfig = {},
        extraDefaultConfig: ExtraDefaultConfig = {}
) = applyAndroidConfig(appId, minSdk, targetSdk, extraConfig, extraDefaultConfig)

/**
 * Apply the default Android configuration a library modules
 *
 * @param extraConfig [ExtraConfig] block that will be applied into the `android` closure
 *
 * @param extraDefaultConfig [ExtraDefaultConfig] block that will be applied into the
 * `android.defaultConfig` closure
 */
fun TestedExtension.configLib(
        minSdk: Int = Project.minSdk,
        targetSdk: Int = Project.targetSdk,
        extraConfig: ExtraConfig = {},
        extraDefaultConfig: ExtraDefaultConfig = {}
) = applyAndroidConfig(null, minSdk, targetSdk, extraConfig, extraDefaultConfig)

private fun TestedExtension.applyAndroidConfig(
        appId: String?,
        minSdk: Int,
        targetSdk: Int,
        extraConfig: ExtraConfig,
        extraDefaultConfig: ExtraDefaultConfig
) {
    compileSdkVersion(Project.targetSdk)
    defaultConfig {

        // Params
        appId?.let { applicationId = it }
        versionCode = Project.versionCode
        versionName = Project.versionName

        // Sdk
        minSdkVersion(minSdk)
        targetSdkVersion(targetSdk)

        // Other
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        extraDefaultConfig(this)
    }
    buildTypes {
        register("releasePlayStore")
        register("releaseBeta")
    }
    // Add support for `src/x/kotlin` instead of `src/x/java` only
    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin")
    }
    compileOptions {
        sourceCompatibility = Project.jdkVersion
        targetCompatibility = Project.jdkVersion
    }
    packagingOptions {
        exclude("go/error.java")
        exclude("go/LoadJNI.java")
        exclude("go/Seq.java")
        exclude("go/Universe.java")
        exclude("META-INF/atomicfu.kotlin_module")
    }

    extraConfig()
}

typealias ExtraConfig = TestedExtension.() -> Unit
typealias ExtraDefaultConfig = DefaultConfig.() -> Unit
