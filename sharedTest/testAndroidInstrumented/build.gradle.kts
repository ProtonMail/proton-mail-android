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
    // endregion

    // region test dependencies
    api(project(Module.testAndroid)) {
        // Exclude MockK since we will use MockK-Android
        // Exclude JUnit 5 since we will use JUnit 4 on instrumented tests
        exclude(MockK.mockk, JUnit5.allModules())
    }

    // jUnit 4
    api(Lib.Test.jUnit4)

    // MockK
    api(Lib.Test.mockk_android)

    // Android
    api(Lib.Android.annotations)
    api(Lib.Test.android_test_core)
    api(Lib.Test.android_test_runner)
    api(Lib.Test.android_test_rules)
    api(Lib.Test.espresso)
    api(Lib.Test.hamcrest)
    // endregion
}
