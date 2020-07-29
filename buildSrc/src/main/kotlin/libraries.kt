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
import org.gradle.api.artifacts.dsl.DependencyHandler
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

// region Proton
// Common
val DependencyHandler.`Proton-kotlin-util` get() =                  protonUtil("kotlin") version `Proton-kotlin-util version`
val DependencyHandler.`Proton-shared-preferences` get() =           protonUtil("android-shared-preferences") version `Proton-shared-preferences version`
val DependencyHandler.`Proton-work-manager` get() =                 protonUtil("android-work-manager") version `Proton-work-manager version`
// Test
val DependencyHandler.`Proton-android-test` get() =                 protonTest("android") version `Proton-android-test version`
val DependencyHandler.`Proton-android-instrumented-test` get() =    protonTest("android-instrumented") version `Proton-android-instr-test version`
val DependencyHandler.`Proton-kotlin-test` get() =                  protonTest("kotlin") version `Proton-kotlin-test version`

val DependencyHandler.`Proton-domain` get() =                       proton("domain") version `Proton-domain version`

fun DependencyHandler.protonTest(moduleSuffix: String, version: String? = null) =
    proton("test", moduleSuffix, version)
fun DependencyHandler.protonUtil(moduleSuffix: String, version: String? = null) =
    proton("util", moduleSuffix, version)

fun DependencyHandler.proton(module: String, moduleSuffix: String? = null, version: String? = null) =
    dependency("me.proton.core", module = module, moduleSuffix = moduleSuffix, version = version)
// endregion

// region Android
val DependencyHandler.`android-biometric` get() =           androidx("biometric") version `android-biometric version`
val DependencyHandler.`android-fragment` get() =            androidx("fragment", moduleSuffix = "ktx") version `android-fragment version`
val DependencyHandler.`android-media` get() =               androidx("media") version `android-media version`
val DependencyHandler.`gcm` get() =                         playServices("gcm")
val DependencyHandler.`room-rxJava` get() =                 androidxRoom("rxjava2")
val DependencyHandler.`safetyNet` get() =                   playServices("safetynet")

fun DependencyHandler.playServices(moduleSuffix: String, version: String = `playServices version`) =
    googleAndroid("gms", "play-services", moduleSuffix, version)
// endregion

// region Test
val DependencyHandler.`assertJ` get() =                     dependency("org.assertj", module = "assertj-core") version `assertJ version`
val DependencyHandler.`hamcrest` get() =                    dependency("org.hamcrest", module = "hamcrest-library") version `hamcrest version`
val DependencyHandler.`jUnit5-jupiter-api` get() =          jUnit5jupiter("api")
val DependencyHandler.`jUnit5-jupiter-engine` get() =       jUnit5jupiter("engine")
val DependencyHandler.`jUnit5-jupiter-params` get() =       jUnit5jupiter("params")
val DependencyHandler.`jUnit5-vintage-engine` get() =       jUnit5vintage("engine")
val DependencyHandler.`robolectric` get() =                 dependency("org.robolectric", module = "robolectric") version `robolectric version`

//testRail
val DependencyHandler.`jsonsimple` get() =                 dependency("com.googlecode.json-simple", module = "json-simple") version `json-simple version`

// region jUnit 5 groups
fun DependencyHandler.jUnit5jupiter(moduleSuffix: String, version: String = `jUnit5 version`) =
        jUnit5("jupiter", "jupiter-$moduleSuffix", version)

fun DependencyHandler.jUnit5vintage(moduleSuffix: String, version: String = `jUnit5 version`) =
        jUnit5("vintage", "vintage-$moduleSuffix", version)

fun DependencyHandler.jUnit5(groupName: String, moduleSuffix: String, version: String = `jUnit5 version`) =
        dependency("org.junit", groupName, "junit", moduleSuffix, version)
// endregion
// endregion

// region Retrofit
val DependencyHandler.`retrofit-gson` get() =               squareup("retrofit2", "converter-gson") version `retrofit version`
val DependencyHandler.`retrofit-rxJava` get() =             squareup("retrofit2", "adapter-rxjava2") version `retrofit version`
val DependencyHandler.`okHttp-loggingInterceptor` get() =   squareup("okhttp3", "logging-interceptor") version `okHttp3 version`
// endregion

// region RxJava
val DependencyHandler.`rxJava-android` get() =              dependency("io.reactivex", "rxjava2", "rxandroid") version `rxJava version`
val DependencyHandler.`rxRelay` get() =                     jakeWharton("rxrelay2", "rxrelay") version `rxRelay version`
// endregion

// region Other
val DependencyHandler.`apache-commons-lang` get() =         dependency("org.apache", "commons", moduleSuffix = "lang3") version `apache-commons-lang version`
val DependencyHandler.`butterknife-runtime` get() =         jakeWharton(module = "butterknife") version `butterKnife version`
val DependencyHandler.`butterknife-compiler` get() =        jakeWharton(module = "butterknife", moduleSuffix = "compiler") version `butterKnife version`
val DependencyHandler.`gson` get() =                        google("code.gson", "gson") version `gson version`
val DependencyHandler.`hugo-annotations` get() =            jakeWharton("hugo", moduleSuffix = "annotations") version `hugo version`
val DependencyHandler.`hugo-plugin` get() =                 jakeWharton("hugo", moduleSuffix = "plugin") version `hugo version`
val DependencyHandler.`jsoup` get() =                       dependency("org.jsoup", module = "jsoup") version `jsoup version`
val DependencyHandler.`sentry-android` get() =              dependency("io.sentry", module = "sentry-android") version `sentry version`
val DependencyHandler.`sentry-android-plugin` get() =       dependency("io.sentry", module = "sentry-android-gradle-plugin") version `sentry-plugin version`
val DependencyHandler.`stetho` get() =                      dependency("com.facebook", "stetho") version `stetho version`
val DependencyHandler.`timber` get() =                      jakeWharton("timber") version `timber version`
val DependencyHandler.`trustKit` get() =                    dependency("com.datatheorem.android", "trustkit") version `trustKit version`
// region DoH
val DependencyHandler.`minidns` get() =                     dependency("org.minidns", module = "minidns-hla") version `minidns version`
val DependencyHandler.`retrofit2-converter` get() =         dependency("com.squareup.retrofit2", module = "converter-jackson") version `rf2 converter version`
val DependencyHandler.`fasterxml-jackson-core` get() =      dependency("com.fasterxml.jackson.core", module = "jackson-core") version `jackson version`
val DependencyHandler.`fasterxml-jackson-anno` get() =      dependency("com.fasterxml.jackson.core", module = "jackson-annotations") version `jackson version`
val DependencyHandler.`fasterxml-jackson-databind` get() =  dependency("com.fasterxml.jackson.core", module = "jackson-databind") version `jackson version`
// endregion

// endregion

// region Instrumentation tests
val DependencyHandler.aerogear get() =  dependency("org.jboss.aerogear", module = "aerogear-otp-java") version `aerogear version`
val DependencyHandler.`espresso-contrib` get() =  androidx("test.espresso", module = "espresso-contrib") version `espresso version`
val DependencyHandler.`espresso-intents` get() =  androidx("test.espresso", module = "espresso-intents") version `espresso version`
val DependencyHandler.falcon get() = dependency("com.jraska", module = "falcon") version `falcon version`
val DependencyHandler.`orchestrator` get() =  androidx("test", module = "orchestrator") version `android-test version`
// endregion
