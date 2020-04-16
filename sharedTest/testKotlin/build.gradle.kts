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
    apply(Plugin.java_library)
    apply(Plugin.kotlin)
}

dependencies {
    // region base dependencies
    // Kotlin
    implementation(Lib.kotlin)
    implementation(Lib.coroutines)
    // endregion


    // region test dependencies
    // region jUnit 5

    // (Required) Writing and executing Unit Tests on the JUnit Platform
    api(Lib.Test.jUnit5_jupiterApi)
    testRuntimeOnly(Lib.Test.jUnit5_jupiterEngine)

    // (Optional) If you need "Parameterized Tests"
    api(Lib.Test.jUnit5_jupiterParams)

    // (Optional) If you also have JUnit 4-based tests
    api(Lib.Test.jUnit4)
    testRuntimeOnly(Lib.Test.jUnit5_vintageEngine)

    // endregion

    // Assertion
    api(Lib.Test.assertJ)

    // MockK
    api(Lib.Test.mockk)

    // Kotlin
    api(Lib.Test.coroutines)
    // endregion
}
