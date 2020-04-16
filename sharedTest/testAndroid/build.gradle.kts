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
    apply(Plugin.android_library)
    apply(Plugin.kotlin_android)
    apply(Plugin.kotlin_android_extensions)
}

android { configLib() }

dependencies {
    // region base dependencies
    // Kotlin
    implementation(Lib.kotlin)
    implementation(Lib.coroutines_android)

    // Android
    implementation(Lib.Android.lifecycle_runtime)
    implementation(Lib.Android.lifecycle_liveData)
    implementation(Lib.Android.lifecycle_viewModel)

    // RxJava
    implementation(Lib.rxJava_android)
    // endregion


    // region test dependencies
    api(project(Module.testKotlin))

    // Android
    api(Lib.Test.android_arch)
    // endregion
}
