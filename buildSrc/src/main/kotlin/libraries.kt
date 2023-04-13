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
import org.gradle.api.artifacts.dsl.DependencyHandler
import studio.forface.easygradle.dsl.*
import studio.forface.easygradle.dsl.android.*

// region Proton
// Common
val DependencyHandler.`Proton-kotlin-util` get() =                  protonUtil("kotlin") version `Proton-core version`
val DependencyHandler.`Proton-shared-preferences` get() =           protonUtil("android-shared-preferences") version `Proton-core version`
val DependencyHandler.`Proton-work-manager` get() =                 protonUtil("android-work-manager") version `Proton-core version`
// Test
val DependencyHandler.`Proton-android-test` get() =                 protonTest("android") version `Proton-core version`
val DependencyHandler.`Proton-android-instrumented-test` get() =    protonTest("android-instrumented") version `Proton-core version`
val DependencyHandler.`Proton-kotlin-test` get() =                  protonTest("kotlin") version `Proton-core version`

val DependencyHandler.`Proton-account` get() =                      proton("account") version `Proton-core version`
val DependencyHandler.`Proton-account-manager` get() =              proton("account-manager") version `Proton-core version`
val DependencyHandler.`Proton-auth` get() =                         proton("auth") version `Proton-core version`
val DependencyHandler.`Proton-auth-test` get() =                    proton("auth-test") version `Proton-core version`
val DependencyHandler.`Proton-challenge` get() =                    proton("challenge") version `Proton-core version`
val DependencyHandler.`Proton-contact` get() =                      proton("contact") version `Proton-core version`
val DependencyHandler.`Proton-country` get() =                      proton("country") version `Proton-core version`
val DependencyHandler.`Proton-crypto` get() =                       proton("crypto") version `Proton-core version`
val DependencyHandler.`Proton-crypto-validator` get() =             proton("crypto-validator") version `Proton-core version`
val DependencyHandler.`Proton-data` get() =                         proton("data") version `Proton-core version`
val DependencyHandler.`Proton-data-room` get() =                    proton("data-room") version `Proton-core version`
val DependencyHandler.`Proton-domain` get() =                       proton("domain") version `Proton-core version`
val DependencyHandler.`Proton-feature-flag` get() =                 proton("feature-flag") version `Proton-core version`
val DependencyHandler.`Proton-human-verification` get() =           proton("human-verification") version `Proton-core version`
val DependencyHandler.`Proton-key` get() =                          proton("key") version `Proton-core version`
val DependencyHandler.`Proton-mail-settings` get() =                proton("mail-settings") version `Proton-core version`
val DependencyHandler.`Proton-metrics` get() =                      proton("metrics") version `Proton-core version`
val DependencyHandler.`Proton-network` get() =                      proton("network") version `Proton-core version`
val DependencyHandler.`Proton-observability` get() =                proton("observability") version `Proton-core version`
val DependencyHandler.`Proton-payment` get() =                      proton("payment") version `Proton-core version`
val DependencyHandler.`Proton-payment-iap` get() =                  proton("payment-iap") version `Proton-core version`
val DependencyHandler.`Proton-plan` get() =                         proton("plan") version `Proton-core version`
val DependencyHandler.`Proton-presentation` get() =                 proton("presentation") version `Proton-core version`
val DependencyHandler.`Proton-report` get() =                       proton("report") version `Proton-core version`
val DependencyHandler.`Proton-user` get() =                         proton("user") version `Proton-core version`
val DependencyHandler.`Proton-user-domain` get() =                  proton("user-domain") version `Proton-core version`
val DependencyHandler.`Proton-user-settings` get() =                proton("user-settings") version `Proton-core version`
val DependencyHandler.`Proton-util-android-dagger` get() = proton("util-android-dagger") version `Proton-core version`
val DependencyHandler.`Proton-test-quark` get() = proton("test-quark") version `Proton-core version`

fun DependencyHandler.protonTest(moduleSuffix: String, version: String? = null) =
    proton("test", moduleSuffix, version)
fun DependencyHandler.protonUtil(moduleSuffix: String, version: String? = null) =
    proton("util", moduleSuffix, version)

fun DependencyHandler.proton(module: String, moduleSuffix: String? = null, version: String? = null) =
    dependency("me.proton.core", module = module, moduleSuffix = moduleSuffix, version = version)
// endregion

// region Android
val DependencyHandler.`android-biometric` get() =           androidx("biometric") version `android-biometric version`
val DependencyHandler.`android-core-splashscreen` get() =   androidx("core", moduleSuffix = "splashscreen") version `android-core-splashscreen version`
val DependencyHandler.`android-flexbox` get() =             google("android", "flexbox") version `flexbox version`
val DependencyHandler.`android-fragment` get() =            androidx("fragment", moduleSuffix = "ktx") version `android-fragment version`
val DependencyHandler.`android-media` get() =               androidx("media") version `android-media version`
val DependencyHandler.`android-preference` get() =          androidx("preference", moduleSuffix = "ktx") version `android-preference version`
val DependencyHandler.`android-webkit` get() =              androidx("webkit", "webkit") version `android-webkit version`
val DependencyHandler.`google-services` get() =             googleServices()
val DependencyHandler.`android-startup-runtime` get() =     androidx("startup", moduleSuffix = "runtime") version `android-startup version`
val DependencyHandler.`lifecycle-extensions` get() =        androidxLifecycle("extensions") version `lifecycle-extensions version`
val DependencyHandler.`room-rxJava` get() =                 androidxRoom("rxjava2")
val DependencyHandler.`safetyNet` get() =                   playServices("safetynet")
val DependencyHandler.`google-play-review` get() = google("android.play", "review") version `google-play-core-libs`

fun DependencyHandler.googleServices(moduleSuffix: String? = null, version: String = `googleServices version`) =
    google("gms", "google-services", moduleSuffix, version)

fun DependencyHandler.playServices(moduleSuffix: String, version: String = `playServices version`) =
    googleAndroid("gms", "play-services", moduleSuffix, version)
// endregion

// region Test
val DependencyHandler.`android-test-core-ktx` get() =       androidxTest("core-ktx")
val DependencyHandler.`android-test-junit` get() =          androidx("test.ext", "junit-ktx") version `android-test-ext version`
val DependencyHandler.`assert4k` get() =                    forface(module = "assert4k-jvm") version `assert4k version`
val DependencyHandler.`assertJ` get() =                     dependency("org.assertj", module = "assertj-core") version `assertJ version`
val DependencyHandler.`hamcrest` get() =                    dependency("org.hamcrest", module = "hamcrest-library") version `hamcrest version`
val DependencyHandler.`turbine` get() =                     dependency( "app.cash.turbine", module = "turbine") version `turbine version`
// endregion

// testRail
val DependencyHandler.`json-simple` get() =                 dependency("com.googlecode.json-simple", module = "json-simple") version `json-simple version`

// region Arrow
fun DependencyHandler.arrow(moduleSuffix: String) =
    dependency("io.arrow-kt", module = "arrow", moduleSuffix = moduleSuffix) version `arrow version`
val DependencyHandler.`arrow-core` get() = arrow("core")
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
val DependencyHandler.`desugaring` get() =                  android( "tools", module = "desugar_jdk_libs") version `desugaring version`
val DependencyHandler.`firebase-messaging` get() =          google("firebase", moduleSuffix = "messaging") version `firebase-messaging version`
val DependencyHandler.`gson` get() =                        google("code.gson", "gson") version `gson version`
val DependencyHandler.`jsoup` get() =                       dependency("org.jsoup", module = "jsoup") version `jsoup version`
val DependencyHandler.`leakcanary` get() =                  dependency("com.squareup.leakcanary", module = "leakcanary-android") version `leakcanary version`
val DependencyHandler.`okhttp-url-connection` get() =       squareup("okhttp3", "okhttp-urlconnection") version `okHttp-url-connection version`
val DependencyHandler.`okio` get() =                        dependency("com.squareup.okio", module = "okio") version `okio version`
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
val DependencyHandler.`remark` get() =                      dependency("com.overzealous", module = "remark") version `remark version`
val DependencyHandler.`store` get() =                       dependency("com.dropbox.mobile.store", module = "store4") version `store version`
val DependencyHandler.`coil-base` get() =                   dependency("io.coil-kt", module="coil-base") version `coil version`
// endregion

// endregion

// region Instrumentation tests
val DependencyHandler.aerogear get() =  dependency("org.jboss.aerogear", module = "aerogear-otp-java") version `aerogear version`
val DependencyHandler.`espresso-contrib` get() =  androidx("test.espresso", module = "espresso-contrib") version `espresso version`
val DependencyHandler.`espresso-intents` get() =  androidx("test.espresso", module = "espresso-intents") version `espresso version`
val DependencyHandler.`espresso-web` get() =  androidx("test.espresso", module = "espresso-web") version `espresso version`
val DependencyHandler.falcon get() = dependency("com.jraska", module = "falcon") version `falcon version`
val DependencyHandler.`orchestrator` get() =  androidx("test", module = "orchestrator") version `android-test-orchestrator version`
val DependencyHandler.`browserstack-gradle-plugin` get() =  dependency("gradle.plugin.com.browserstack.gradle", module = "browserstack-gradle-plugin") version `browserstack-plugin version`
val DependencyHandler.`uiautomator` get() =  androidx("test.uiautomator", module = "uiautomator") version `uiautomator version`
val DependencyHandler.`android-activation` get() =  dependency("com.sun.mail", module = "android-activation") version `sun-mail-android version`
val DependencyHandler.`junit-ext` get() =  androidx("test.ext", module = "junit-ktx") version `junit-ktx version`
val DependencyHandler.`mock-web-server` get() =  dependency("com.squareup.okhttp3", module = "mockwebserver") version `okHttp3 version`
val DependencyHandler.`okhttp-tls` get() =  dependency("com.squareup.okhttp3", module = "okhttp-tls") version `okHttp3 version`
// endregion
