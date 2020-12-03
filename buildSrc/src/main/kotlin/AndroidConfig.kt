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
import org.gradle.api.JavaVersion
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.configure
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

/**
 * Dsl for apply the android configuration to a library or application module
 * @author Davide Farella
 */
@Suppress("LongMethod") // This is a setup for Android module, it does not contain any logic and doesn't
//                                   make sense to split it
fun org.gradle.api.Project.android(

    appIdSuffix: String? = null,
    minSdk: Int = ProtonMail.minSdk,
    targetSdk: Int = ProtonMail.targetSdk,
    version: Version? = null,
    versionCode: Int = ProtonMail.versionCode,
    versionName: String = ProtonMail.versionName,
    config: ExtraConfig = {}

) = (this as ExtensionAware).extensions.configure<TestedExtension> {

    compileSdkVersion(targetSdk)
    ndkVersion = "21.3.6528147" // Same as Docker image
    defaultConfig {

        // Params
        appIdSuffix?.let { applicationId = "ch.protonmail.$it" }
        if (version != null) {
            this.version = version
        } else {
            this.versionCode = versionCode
            this.versionName = versionName
        }

        // SDK
        minSdkVersion(minSdk)
        targetSdkVersion(targetSdk)

        // Other
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
        multiDexEnabled = true

        javaCompileOptions {
            annotationProcessorOptions {

                // Annotation processors must be explicitly declared now.  The following dependencies on
                // the compile classpath are found to contain annotation processor.  Please add them to the
                // annotationProcessor configuration.
                // - auto-service-1.0-rc4.jar (com.google.auto.service:auto-service:1.0-rc4)
                //
                // Note that this option ( 👇 ) is deprecated and will be removed in the future.
                // See https://developer.android.com/r/tools/annotation-processor-error-message.html for
                // more details.
                includeCompileClasspath = true
            }
        }
    }

    buildTypes {
        register("releasePlayStore")
        register("releaseBeta")
    }

    lintOptions {
        disable("InvalidPackage")
        disable("MissingTranslation")
        disable("ExtraTranslation")
    }
    // Add support for `src/x/kotlin` instead of `src/x/java` only
    sourceSets {
        getByName("main").java.srcDirs("src/main/kotlin")
        getByName("test").java.srcDirs("src/test/kotlin")
        getByName("androidTest").java.srcDirs("src/androidTest/kotlin", "src/uiTest/kotlin")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = sourceCompatibility
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        animationsDisabled = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    packagingOptions {
        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/AL2.0")
        exclude("META-INF/DEPENDENCIES")
        exclude("META-INF/DEPENDENCIES.txt")
        exclude("META-INF/LGPL2.1")
        exclude("META-INF/LICENSE")
        exclude("META-INF/LICENSE.md")
        exclude("META-INF/LICENSE.txt")
        exclude("META-INF/LICENSE-notice.md")
        exclude("META-INF/NOTICE")
        exclude("META-INF/NOTICE.txt")
        exclude("META-INF/rxjava.properties")
    }

    apply(config)
}

typealias ExtraConfig = TestedExtension.() -> Unit
