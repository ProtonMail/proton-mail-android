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
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*
import java.io.FileInputStream
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
    `sentry-android`
    `browserstack`
    `jacoco`
}

kapt {
    correctErrorTypes = true
}

val privateProperties = Properties().apply {
    try {
        load(FileInputStream("privateConfig/private.properties"))
    } catch (e: FileNotFoundException) {
        put("sentryDNS_1", "")
        put("sentryDNS_2", "")
        put("safetyNet_apiKey", "")
        put("b_endpointUrl", "")
        put("d_endpointUrl", "")
        put("h_endpointUrl", "")
        put("pm_clientSecret", "")
    }
}

val experimentalProperties = Properties().apply {
    load(FileInputStream("experimental.properties"))
}

val adb = "${System.getenv("ANDROID_HOME")}/platform-tools/adb"
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

android(
    appIdSuffix = "android",
    minSdk = 23,
    useDataBinding = true
) {

    useLibrary("org.apache.http.legacy")
    flavorDimensions("default")

    defaultConfig {

        // Private
        buildConfigField("String", "SENTRY_DNS_1", "\"${privateProperties["sentryDNS_1"]}\"")
        buildConfigField("String", "SENTRY_DNS_2", "\"${privateProperties["sentryDNS_2"]}\"")
        buildConfigField("String", "SAFETY_NET_API_KEY", "\"${privateProperties["safetyNet_apiKey"]}\"")
        buildConfigField("String", "B_ENDPOINT_URL", "\"${privateProperties["b_endpointUrl"]}\"")
        buildConfigField("String", "D_ENDPOINT_URL", "\"${privateProperties["d_endpointUrl"]}\"")
        buildConfigField("String", "H_ENDPOINT_URL", "\"${privateProperties["h_endpointUrl"]}\"")
        buildConfigField("String", "PM_CLIENT_SECRET", "\"${privateProperties["pm_clientSecret"]}\"")
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
    }

    signingConfigs {
        register("release") {
            if (privateProperties.getProperty("sentryDNS_1").isNotEmpty()) {
                storeFile = file("$rootDir/privateConfig/keystore/ProtonMail.keystore")
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

dependencies {

    // Hilt
    kapt(
        `assistedInject-processor-dagger`,
        `hilt-android-compiler`,
        `hilt-androidx-compiler`
    )

    // Dagger modules
    // These dependency can be resolved at compile-time only.
    // We should not use include any of them in the run-time of this module, we need this dependency for of being able
    // to build the Dagger's dependency graph
    compileOnly(
        project(Module.credentials),
        `assistedInject-annotations-dagger`
    )

    implementation(

        // Core
        rootProject.aar(Lib.protonCore, version = `old protonCore version`),
        // rootProject.aar(Lib.composer, version = `composer version`),

        `Proton-data`,
        `Proton-domain`,
        `Proton-presentation`,
        `Proton-network`,
        `Proton-kotlin-util`,
        `Proton-shared-preferences`,
        `Proton-work-manager`,
        `Proton-crypto`,
        `Proton-auth`,
        `Proton-account`,
        `Proton-account-manager`,
        `Proton-user`,
        `Proton-key`,
        `Proton-human-verification`,
        `Proton-country`,

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
        `appcompat`,
        `android-biometric`,
        `constraint-layout`,
        `android-fragment`,
        `android-flexbox`,
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
        `gotev-cookieStore`,
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
        `coil`
    )

    debugImplementation(
        `leakcanary`
    )

    kapt(

        // Room
        `room-compiler`,

        // Other
        `butterknife-compiler`
    )

    testImplementation(project(Module.testAndroid))
    androidTestUtil(`orchestrator`)
    androidTestImplementation(
        project(Module.testAndroidInstrumented),
        `aerogear`,
        `falcon`,
        `espresso-intents`,
        `espresso-web`,
        `uiautomator`,
        `android-activation`,
        `Proton-android-instrumented-test`
    )
}

apply(from = "old.build.gradle")
