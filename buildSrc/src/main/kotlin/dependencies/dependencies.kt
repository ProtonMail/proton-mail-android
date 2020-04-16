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

// Android Arch
object AndroidArch: Dependency, DepGroup("androidx.arch.core") {
    override val version = V.android_arch

    val common = module("core-common")
    val testing = module("core-testing")

    override fun all() = common + testing
}

// Android Lifecycle
object AndroidLifecycle: Dependency, DepGroup("androidx.lifecycle") {
    override val version = V.android_lifecycle

    val extensions = module("lifecycle-extensions")
    val runtime = module("lifecycle-runtime") // TODO -ktx artifact available starting from 2.2.x
    val liveData = module("lifecycle-livedata-ktx")
    val viewModel = module("lifecycle-viewmodel-ktx")

    override fun all() = extensions + runtime + liveData + viewModel
}

// Android Test
object AndroidTest: Dependency, DepGroup("androidx.test") {
    override val version = V.androidx_test

    val core = module("core")
    val rules = module("rules")
    val runner = module("runner")

    override fun all() = core + rules + runner
}

// ButterKnife
object ButterKnife: Dependency, DepGroup("com.jakewharton") {
    override val version = V.butterKnife

    val runtime = module("butterknife")
    val compiler = module("butterknife-compiler")

    override fun all() = runtime + compiler
}

// Dagger
object Dagger: Dependency, DepGroup("com.google.dagger") {
    override val version = V.dagger

    val dagger = module("dagger")
    val android = module("dagger-android")
    val androidSupport = module("dagger-android-support")
    val compiler = module("dagger-compiler")
    val androidProcessor = module("dagger-android-processor")

    override fun all() = dagger + android + androidSupport + compiler + androidProcessor
}

// Hugo
object Hugo: Dependency, DepGroup("com.jakewharton.hugo") {
    override val version = V.hugo

    val annotations = module("hugo-annotations")
    val gradlePlugin = gradlePlugin("hugo-plugin", "com.jakewharton.hugo")

    override fun all() = listOf(annotations)
}

// JUnit 5
object JUnit5: Dependency {
    override val version = V.jUnit5

    object Jupiter : DepGroup("org.junit.jupiter") {
        val api = module("junit-jupiter-api")
        val engine = module("junit-jupiter-engine")
        val params = module("junit-jupiter-params")

        override fun all() = api + engine + params
    }
    object Vintage : DepGroup("org.junit.vintage") {
        val engine = module("junit-vintage-engine")

        override fun all() = listOf(engine)
    }

    override fun allGroups() = Jupiter + Vintage
}

// Kotlin
object Kotlin : Dependency, DepGroup("org.jetbrains.kotlin") {
    override val version = V.kotlin

    val jdk7 = module("kotlin-stdlib-jdk7")
    val reflect = module("kotlin-reflect")

    val gradlePlugin = gradlePlugin("kotlin-gradle-plugin", "kotlin")
    val androidGradlePlugin = modulePlugin("kotlin-android")
    val androidExtGradlePlugin = modulePlugin("kotlin-android-extensions")

    override fun all() = jdk7 + reflect
}

// MockK
object MockK: Dependency, DepGroup("io.mockk") {
    override val version = V.mockk

    val mockk = module("mockk")
    val android = module("mockk-android")

    override fun all() = mockk + android
}

// Retrofit
object Retrofit: Dependency, DepGroup("com.squareup.retrofit2") {
    override val version = V.retrofit

    val retrofit = module("retrofit")
    val gson = module("converter-gson")
    val rxJava = module("adapter-rxjava2")

    override fun all() = retrofit + gson + rxJava
}

// Room
object Room: Dependency, DepGroup("androidx.room") {
    override val version = V.android_room

    val runtime = module("room-runtime")
    val ktx = module("room-ktx")
    val rxJava = module("room-rxjava2")
    val compiler = module("room-compiler")

    override fun all() = runtime + ktx + rxJava + compiler
}

// ViewStateStore
object ViewStateStore: Dependency, DepGroup("studio.forface.viewstatestore") {
    override val version = V.viewStateStore

    val viewStateStore = module("viewstatestore")
    val paging = module("viewstatestore-paging")

    override fun all() = viewStateStore + paging
}

// Sentry
object Sentry: Dependency, DepGroup("io.sentry") {
    override val version = V.sentry

    val sentry = module("sentry-android")
    val gradlePlugin = gradlePlugin("sentry-android-gradle-plugin","sentry")
    val sentryGradlePlugin = modulePlugin("io.sentry.android.gradle")

    override fun all() = listOf(sentry)
}