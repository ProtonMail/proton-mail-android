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

// TODO remove: workaround for nested object in Groovy
object LibAndroid { val get = Lib.Android }

/**
 * Libraries used by the project
 * @author Davide Farella
 */
object Lib {

    /* Kotlin */
    val kotlin =                                    Kotlin.jdk7()
    const val coroutines =                          "org.jetbrains.kotlinx:kotlinx-coroutines-core:${V.coroutines}"
    const val coroutines_android =                  "org.jetbrains.kotlinx:kotlinx-coroutines-android:${V.coroutines}"
    val reflect =                                   Kotlin.reflect()
    const val serialization =                       "org.jetbrains.kotlinx:kotlinx-serialization-runtime:${V.serialization}"

    /* Test */
    object Test {
        val android_arch =                          AndroidArch.testing()
        val android_test_core =                     AndroidTest.core()
        val android_test_rules =                    AndroidTest.rules()
        val android_test_runner =                   AndroidTest.runner()
        const val assertJ =                         "org.assertj:assertj-core:${V.assertJ}"
        const val coroutines =                      "org.jetbrains.kotlinx:kotlinx-coroutines-test:${V.coroutines}"
        const val espresso =                        "androidx.test.espresso:espresso-core:${V.espresso}"
        const val hamcrest =                        "org.hamcrest:hamcrest-library:${V.hamcrest}"
        const val jUnit4 =                          "junit:junit:${V.jUnit4}"
        val jUnit5_jupiterApi =                     JUnit5.Jupiter.api()
        val jUnit5_jupiterEngine =                  JUnit5.Jupiter.engine()
        val jUnit5_jupiterParams =                  JUnit5.Jupiter.params()
        val jUnit5_vintageEngine =                  JUnit5.Vintage.engine()
        const val kotlin =                          "org.jetbrains.kotlin:kotlin-test:${V.kotlin}"
        const val kotlin_junit =                    "org.jetbrains.kotlin:kotlin-test-junit:${V.kotlin}"
        val mockk =                                 MockK.mockk()
        val mockk_android =                         MockK.android()
    }

    /* Android */
    object Android {
        const val annotations =                     "androidx.annotation:annotation:${V.android_annotations}"
        const val appcompat =                       "androidx.appcompat:appcompat:${V.android_support}"
        val arch_core =                             AndroidArch.common()
        const val biometric =                       "androidx.biometric:biometric:${V.android_biometric}"
        const val constraintLayout =                "androidx.constraintlayout:constraintlayout:${V.android_constraintLayout}"
        const val espresso =                        "androidx.test.espresso:espresso-core:${V.android_espresso}"
        const val ktx =                             "androidx.core:core-ktx:${V.android_ktx}"
        val lifecycle_extensions =                  AndroidLifecycle.extensions()
        val lifecycle_runtime =                     AndroidLifecycle.runtime()
        val lifecycle_liveData =                    AndroidLifecycle.liveData()
        val lifecycle_viewModel =                   AndroidLifecycle.viewModel()
        const val material =                        "com.google.android.material:material:${V.android_material}"
        const val paging =                          "androidx.paging:paging-runtime-ktx:${V.android_paging}"
        const val palette =                         "androidx.palette:palette:${V.android_palette}"
        val room_compiler =                         Room.compiler()
        val room_ktx =                              Room.ktx()
        val room_runtime =                          Room.runtime()
        val room_rxJava =                           Room.rxJava()
        const val work =                            "androidx.work:work-runtime-ktx:${V.android_work}"
    }

    /* Other */
    val butterKnife =                               ButterKnife.runtime()
    val butterKnife_compiler =                      ButterKnife.compiler()
    val dagger =                                    Dagger.dagger()
    val dagger_android =                            Dagger.android()
    val dagger_androidSupport =                     Dagger.androidSupport()
    val dagger_compiler =                           Dagger.compiler()
    val dagger_androidProcessor =                   Dagger.androidProcessor()
    const val gson =                                "com.google.code.gson:gson:${V.gson}"
    val hugo_annotations =                          Hugo.annotations()
    const val okHttp_loggingInterceptor =           "com.squareup.okhttp3:logging-interceptor:${V.okHttp3}"
    val retrofit =                                  Retrofit.retrofit()
    val retrofit_gson =                             Retrofit.gson()
    val retrofit_rxJava =                           Retrofit.rxJava()
    const val rxJava_android =                      "io.reactivex.rxjava2:rxandroid:${V.rxJava}"
    const val rxRelay =                             "com.jakewharton.rxrelay2:rxrelay:${V.rxRelay}"
    const val stetho =                              "com.facebook.stetho:stetho:${V.stetho}"
    const val timber =                              "com.jakewharton.timber:timber:${V.timber}"
    const val trustKit =                            "com.datatheorem.android.trustkit:trustkit:${V.trustKit}"
    val viewStateStore =                            ViewStateStore.viewStateStore()
    val viewStateStore_paging =                     ViewStateStore.paging()
    val sentry =                                    Sentry.sentry()
}
