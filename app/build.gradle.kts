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
import java.io.FileInputStream
import java.util.*
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

plugins {
    `android-application`
    `detekt`
    `kotlin-android`
    `kotlin-android-extensions`
    `kotlin-kapt`
    `kotlin-serialization`
    `hugo`
    `sentry-android`
}

kapt {
    correctErrorTypes = true
}

detekt {
    failFast = false
    config = files("$rootDir/detekt/config.yml")

    reports {
        xml.enabled = false
        html.enabled = false
        txt.enabled = false
        custom {
            reportId = "DetektQualityOutputReport"
            destination = file("$rootDir/detekt/report.json")
        }
    }
}
configurations { detekt }

val privateProperties = Properties().apply {
    load(FileInputStream("privateConfig/private.properties"))
}

android(appIdSuffix = "android") {

    useLibrary("org.apache.http.legacy")
    flavorDimensions("default")

    defaultConfig {
        multiDexEnabled = true
        
        buildConfigField("String", "SENTRY_DNS_1", "\"${privateProperties["sentryDNS_1"]}\"")
        buildConfigField("String", "SENTRY_DNS_2", "\"${privateProperties["sentryDNS_2"]}\"")
        buildConfigField("String", "SAFETY_NET_API_KEY", "\"${privateProperties["safetyNet_apiKey"]}\"")
        buildConfigField("String", "B_ENDPOINT_URL", "\"${privateProperties["b_endpointUrl"]}\"")
        buildConfigField("String", "D_ENDPOINT_URL", "\"${privateProperties["d_endpointUrl"]}\"")
        buildConfigField("String", "H_ENDPOINT_URL", "\"${privateProperties["h_endpointUrl"]}\"")
        buildConfigField("String", "PM_CLIENT_SECRET", "\"${privateProperties["pm_clientSecret"]}\"")

        buildConfigField("boolean", "FETCH_FULL_CONTACTS", "true")
        buildConfigField("boolean", "REREGISTER_FOR_PUSH", "true")
        buildConfigField("int", "ROOM_DB_VERSION", "${properties["DATABASE_VERSION"]}")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    signingConfigs {
        register("release") {
            keyAlias = "ProtonMail"
        }
    }

    productFlavors {
        register("playstore") {
            applicationId = "ch.protonmail.android"
        }
        register("beta") {
            applicationId = "ch.protonmail.android.beta"
        }
    }

    buildTypes {
        all {
            archivesBaseName = "ProtonMail-Android-${android.defaultConfig.versionName}"
            multiDexKeepProguard = File("multidex-proguard.pro")
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("releasePlayStore") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), File("proguard-rules.pro"))
            signingConfig = signingConfigs["release"]
        }
        getByName("releaseBeta") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), File("proguard-rules.pro"))
        }
    }
}

dependencies {
    implementation(
        rootProject.aar(Lib.protonCore, version = `protonCore version`)
//        rootProject.aar(Lib.composer, version = `composer version`),
//        project(Module.tokenAutoComplete)
    )

    // Kapt
    kapt(
        `butterknife-compiler`,
        `dagger-compiler`,
        `dagger-android-processor`,
        `room-compiler`
    )
    kaptTest(`dagger-compiler`)

    implementation(
        // Kotlin
        `kotlin-jdk7`,
        `kotlin-reflect`,
        `coroutines-android`,
        `serialization`,

        // Android
        `android-annotation`,
        `appcompat`,
        `android-biometric`,
        `constraint-layout`,
        `android-fragment`,
        `android-ktx`,
        `android-media`,
        `material`,
        `paging-runtime`,
        `android-work-runtime`,

        // Lifecycle
        `lifecycle-extensions`,
        `lifecycle-runtime`,
        `lifecycle-liveData`,
        `lifecycle-viewModel`,

        // Room
        `room-runtime`,
        `room-ktx`,
        `room-rxJava`,

        // Dagger
        `dagger-android`,
        `dagger-android-support`,

        // Retrofit
        `retrofit`,
        `retrofit-gson`,
        `retrofit-rxJava`,
        `okHttp-loggingInterceptor`,

        // RxJava
        `rxJava-android`,
        `rxRelay`,

        // Other
        `apache-commons-lang`,
        `butterknife-runtime`,
        `gcm`,
        `gson`,
        `hugo-annotations`,
        `jsoup`,
        `safetyNet`,
        `sentry-android`,
        `stetho`,
        `timber`,
        `trustKit`,
        `viewStateStore`,
        `viewStateStore-paging`
    )

    testImplementation(project(Module.testAndroid))
    androidTestImplementation(project(Module.testAndroidInstrumented))

    // Detekt
    detekt(`detekt-cli`)
    detektPlugins(`detekt-code-analysis`)
    detektPlugins(`detekt-formatting`)
}

apply(from = "old.build.gradle")
