import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
plugins {
    `kotlin-dsl`
    kotlin("jvm") version "1.4.30-RC"
}

repositories {
    google()
    jcenter()
    maven(url = "https://dl.bintray.com/proton/Core-publishing")
    maven("https://dl.bintray.com/kotlin/kotlin-eap")
}

dependencies {
    val android =       "4.0.1"         // Released: Jul 14, 2020
    val easyGradle =    "2.7"           // Released: Oct 15, 2020

    // Needed for setup Android config
    implementation("com.android.tools.build:gradle:$android")
    // Needed for many utils
    implementation("studio.forface.easygradle:dsl-android:$easyGradle")
    implementation(kotlin("stdlib-jdk8"))
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}