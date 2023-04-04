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
import studio.forface.easygradle.dsl.android.*
import java.io.FileNotFoundException
import java.util.Properties

plugins {
    `android-application`
    `google-services`
    `kotlin-android`
    `kotlin-android-extensions`
    `kotlin-kapt`
    `hilt`
    `kotlin-serialization`
    `browserstack`
    `sentry-android`
}

kapt {
    correctErrorTypes = true
}

sentry {
    autoInstallation {
        enabled.set(false)
    }
}

val privateProperties = Properties().apply {
    try {
        load(rootDir.resolve("privateConfig/private.properties").inputStream())
    } catch (e: FileNotFoundException) {
        put("sentryDSN", "")
        put("safetyNet_apiKey", "")
        put("b_endpointUrl", "")
        put("d_endpointUrl", "")
        put("h_endpointUrl", "")
        put("pm_clientSecret", "")
    }
}

val experimentalProperties = Properties().apply {
    load(rootDir.resolve("experimental.properties").inputStream())
}

val adb = "${System.getenv("ANDROID_HOME")}/platform-tools/adb"
val sentryDSN = properties["SENTRY_DSN"] ?: privateProperties["sentryDSN"]
val browserstackUser = properties["BROWSERSTACK_USER"] ?: privateProperties["BROWSERSTACK_USER"]
val browserstackKey = properties["BROWSERSTACK_KEY"] ?: privateProperties["BROWSERSTACK_KEY"]
val testRecipient1 = properties["TEST_RECIPIENT1"] ?: privateProperties["TEST_RECIPIENT1"]
val testRecipient2 = properties["TEST_RECIPIENT2"] ?: privateProperties["TEST_RECIPIENT2"]
val testRecipient3 = properties["TEST_RECIPIENT3"] ?: privateProperties["TEST_RECIPIENT3"]
val testRecipient4 = properties["TEST_RECIPIENT4"] ?: privateProperties["TEST_RECIPIENT4"]
val testrailProjectId = properties["TESTRAIL_PROJECT_ID"] ?: privateProperties["TESTRAIL_PROJECT_ID"]
val testrailUsername = properties["TESTRAIL_USERNAME"] ?: privateProperties["TESTRAIL_USERNAME"]
val testrailPassword = properties["TESTRAIL_PASSWORD"] ?: privateProperties["TESTRAIL_PASSWORD"]
val testUser1 = properties["TEST_USER1"] ?: privateProperties["TEST_USER1"]
val testUser2 = properties["TEST_USER2"] ?: privateProperties["TEST_USER2"]
val testUser3 = properties["TEST_USER3"] ?: privateProperties["TEST_USER3"]
val testUser4 = properties["TEST_USER4"] ?: privateProperties["TEST_USER4"]
val testUser5 = properties["TEST_USER5"] ?: privateProperties["TEST_USER5"]

val alphaDebugImplementation by configurations.creating

android(
    appIdSuffix = "android",
    minSdk = 23,
) {

    useLibrary("org.apache.http.legacy")
    flavorDimensions("default")

    defaultConfig {

        // Private
        buildConfigField("String", "SAFETY_NET_API_KEY", "\"${privateProperties["safetyNet_apiKey"]}\"")
        buildConfigField("String", "B_ENDPOINT_URL", "\"${privateProperties["b_endpointUrl"]}\"")
        buildConfigField("String", "D_ENDPOINT_URL", "\"${privateProperties["d_endpointUrl"]}\"")
        buildConfigField("String", "H_ENDPOINT_URL", "\"${privateProperties["h_endpointUrl"]}\"")
        buildConfigField("String", "PM_CLIENT_SECRET", "\"${privateProperties["pm_clientSecret"]}\"")
        buildConfigField("String", "SENTRY_DSN", "\"$sentryDSN\"")
        buildConfigField("String", "BROWSERSTACK_USER", browserstackUser.toString())
        buildConfigField("String", "BROWSERSTACK_KEY", browserstackKey.toString())
        buildConfigField("String", "TEST_RECIPIENT1", testRecipient1.toString())
        buildConfigField("String", "TEST_RECIPIENT2", testRecipient2.toString())
        buildConfigField("String", "TEST_RECIPIENT3", testRecipient3.toString())
        buildConfigField("String", "TEST_RECIPIENT4", testRecipient4.toString())
        buildConfigField("String", "TESTRAIL_PROJECT_ID", testrailProjectId.toString())
        buildConfigField("String", "TESTRAIL_USERNAME", testrailUsername.toString())
        buildConfigField("String", "TESTRAIL_PASSWORD", testrailPassword.toString())
        buildConfigField("String", "TEST_USER1", testUser1.toString())
        buildConfigField("String", "TEST_USER2", testUser2.toString())
        buildConfigField("String", "TEST_USER3", testUser3.toString())
        buildConfigField("String", "TEST_USER4", testUser4.toString())
        buildConfigField("String", "TEST_USER5", testUser5.toString())

        // Experimental
        buildConfigField("boolean", "EXPERIMENTAL_USERS_MANAGEMENT", "${experimentalProperties["users-management"] ?: false}")

        buildConfigField("boolean", "FETCH_FULL_CONTACTS", "true")
        buildConfigField("boolean", "REREGISTER_FOR_PUSH", "true")
        buildConfigField("int", "ROOM_DB_VERSION", "${properties["DATABASE_VERSION"]}")

        buildConfigField("boolean", "REPORT_TO_TESTRAIL", "false")

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }

        testInstrumentationRunnerArguments += mapOf(
            "clearPackageData" to "true"
        )

        testInstrumentationRunner = "ch.protonmail.android.HiltCustomTestRunner"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    signingConfigs {
        register("release") {
            val keystore = file("$rootDir/privateConfig/keystore/ProtonMail.keystore")
            if (keystore.exists()) {
                storeFile = keystore
                storePassword = "${privateProperties["keyStorePassword"]}"
                keyAlias = "ProtonMail"
                keyPassword = "${privateProperties["keyStoreKeyPassword"]}"
            }
        }
    }

    productFlavors {
        register("production") {
            applicationId = "ch.protonmail.android"
        }
        register("beta") {
            applicationId = "ch.protonmail.android.beta"
        }
        register("alpha") {
            applicationId = "ch.protonmail.android"
        }
        register("uiAutomation") {
            applicationId = "ch.protonmail.android"
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

            var testOnLive = false
            val testEnvUrl = when (project.properties["testEnv"] ?: "live") {
                "local" -> "${privateProperties["local_endpointUrl"]}"
                "test" -> "${privateProperties["test_endpointUrl"]}"
                else -> {
                    testOnLive = true
                    "${privateProperties["live_endpointUrl"]}"
                }
            }

            buildConfigField("String", "TEST_ENDPOINT_URL", "\"$testEnvUrl\"")
            buildConfigField("boolean", "TEST_ON_LIVE", "$testOnLive")
        }
    }

    buildTypes {
        all {
            archivesBaseName = "ProtonMail-Android-${android.defaultConfig.versionName}"
            multiDexKeepProguard = File("multidex-proguard.pro")
        }
        getByName("debug") {
            isMinifyEnabled = project.hasProperty("minify")
            isTestCoverageEnabled = true
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), File("proguard-rules.pro"))
            signingConfig = signingConfigs["release"]
        }
    }

    packagingOptions {
        exclude("MANIFEST.MF")
        exclude("LICENSE-2.0.txt")
        exclude("RELEASE.txt")
        exclude("META-INF/NOTICE.md")
        exclude("META-INF/INDEX.LIST")
        exclude("META-INF/gfprobe-provider.xml")
    }
}

browserStackConfig {
    username = browserstackUser.toString().replace("\"", "")
    accessKey = browserstackKey.toString().replace("\"", "")
    configFilePath = "./app/src/uiTest/kotlin/ch/protonmail/android/uitests/testsHelper/browserstack_config.json"
}

// Clear app data each time you run tests locally once before the run. Should be added in IDE run configuration.
tasks.register("clearData", Exec::class) {
    commandLine(adb, "shell", "pm", "clear", "ch.protonmail.android.beta")
}

// Run as ch.protonmail.android.beta and copy test artifacts to sdcard/Download location
tasks.register("copyArtifacts", Exec::class) {
    commandLine(
        adb,
        "shell",
        "run-as",
        "ch.protonmail.android.beta",
        "cp",
        "-R",
        "./files/artifacts/",
        "/sdcard/Download/artifacts"
    )
}

tasks.register("pullTestArtifacts", Exec::class) {
    dependsOn("copyArtifacts")
    commandLine(adb, "pull", "/sdcard/Download/artifacts")
}

tasks.register("jacocoTestReport", JacocoReport::class) {

    dependsOn("testBetaDebugUnitTest")

    reports {
        xml.isEnabled = true
        html.isEnabled = true
    }

    val fileFilter =
        listOf("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*", "**/*Test*.*", "android/**/*.*")
    val debugTree: ConfigurableFileTree =
        fileTree(
            listOf("${project.buildDir}/tmp/kotlin-classes/betaDebug", "${project.buildDir}/tmp/kotlin-classes/debug")
        )
    debugTree.exclude(fileFilter)

    val mainSrc = "${project.projectDir}/src/main/java"
    sourceDirectories.setFrom(files(listOf(mainSrc)))
    classDirectories.setFrom(files(listOf(debugTree)))

    val reportFilesFilter = listOf("**/*.exec", "**/*.ec")
    val reportFiles: ConfigurableFileTree = fileTree(project.buildDir)
    reportFiles.include(reportFilesFilter)
    executionData.setFrom(reportFiles)
}

tasks.withType<Test> {
    // add more coroutines debug information
    systemProperty("kotlinx.coroutines.debug", "on")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions { jvmTarget = ProtonMail.jvmTarget.toString() }
}

dependencies {

    androidTestImplementation(files("libs/fusion-release.aar"))
    coreLibraryDesugaring(
        `desugaring`
    )
    // Hilt
    kapt(
        `assistedInject-processor-dagger`,
        `hilt-android-compiler`,
        `hilt-androidx-compiler`
    )

    kaptAndroidTest( // https://developer.android.com/training/dependency-injection/hilt-testing
        `hilt-android-compiler`
    )

    // Dagger modules
    // These dependency can be resolved at compile-time only.
    // We should not use include any of them in the run-time of this module, we need this dependency for of being able
    // to build the Dagger's dependency graph
    compileOnly(
        `assistedInject-annotations-dagger`
    )

    implementation(

        // Core
        `Proton-data`,
        `Proton-data-room`,
        `Proton-domain`,
        `Proton-presentation`,
        `Proton-network`,
        `Proton-kotlin-util`,
        `Proton-shared-preferences`,
        `Proton-work-manager`,
        `Proton-challenge`,
        `Proton-crypto`,
        `Proton-crypto-validator`,
        `Proton-auth`,
        `Proton-account`,
        `Proton-account-manager`,
        `Proton-user`,
        `Proton-user-settings`,
        `Proton-mail-settings`,
        `Proton-metrics`,
        `Proton-key`,
        `Proton-human-verification`,
        `Proton-observability`,
        `Proton-payment`,
        // `Proton-payment-iap`,
        `Proton-plan`,
        `Proton-contact`,
        `Proton-country`,
        `Proton-plan`,
        `Proton-report`,
        `Proton-feature-flag`,
        `Proton-util-android-dagger`,

        // Modules
        project(Module.domain),
        // project(Module.tokenAutoComplete),

        // Kotlin
        `kotlin-jdk7`,
        `kotlin-reflect`,
        `coroutines-android`,
        `serialization-json`,

        // Arrow
        `arrow-core`,

        // Android
        `android-annotation`,
        `android-biometric`,
        `android-core-splashscreen`,
        `android-fragment`,
        `android-flexbox`,
        `android-ktx`,
        `android-media`,
        `android-startup-runtime`,
        `android-webkit`,
        `android-work-runtime`,
        `appcompat`,
        `constraint-layout`,
        `material`,
        `paging-runtime`,

        // Lifecycle
        `lifecycle-extensions`,
        `lifecycle-runtime`,
        `lifecycle-liveData`,
        `lifecycle-viewModel`,

        // Room
        `room-runtime`,
        `room-ktx`,
        `room-rxJava`,

        // Hilt
        `hilt-android`,
        `hilt-androidx-workManager`,

        // Retrofit
        `okHttp-loggingInterceptor`,
        `retrofit`,
        `retrofit-gson`,
        `retrofit-rxJava`,
        `retrofit2-converter`,

        // RxJava
        `rxJava-android`,
        `rxRelay`,

        // Other
        `apache-commons-lang`,
        `butterknife-runtime`,
        `fasterxml-jackson-core`,
        `fasterxml-jackson-anno`,
        `fasterxml-jackson-databind`,
        `firebase-messaging`,
        `gson`,
        `jsoup`,
        `minidns`,
        `okhttp-url-connection`,
        `safetyNet`,
        `sentry-android`,
        `stetho`,
        `timber`,
        `trustKit`, `android-preference`, // Workaround (https://github.com/datatheorem/TrustKit-Android/issues/76).
        `viewStateStore`,
        `viewStateStore-paging`,
        `remark`,
        `okio`,
        `store`,
        `coil-base`
    )

    alphaDebugImplementation(
        `leakcanary`
    )

    kapt(

        // Room
        `room-compiler`,

        // Other
        `butterknife-compiler`
    )

    testImplementation(
        `Proton-kotlin-util`,
        project(Module.testAndroid)
    )
    androidTestUtil(`orchestrator`)
    androidTestImplementation(
        project(Module.testAndroidInstrumented),
        `aerogear`,
        `falcon`,
        `espresso-intents`,
        `espresso-web`,
        `uiautomator`,
        `android-activation`,
        `Proton-android-instrumented-test`,
        `Proton-auth-test`,
        `Proton-payment-iap`,
        `junit-ext`,
        `mock-web-server`,
        `okhttp-tls`,
        `Proton-test-quark`
    )
}

apply(from = "old.build.gradle")
