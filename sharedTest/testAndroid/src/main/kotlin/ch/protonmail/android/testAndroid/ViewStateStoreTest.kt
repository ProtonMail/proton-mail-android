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
package ch.protonmail.android.testAndroid

import studio.forface.viewstatestore.ErrorStateGenerator
import studio.forface.viewstatestore.ViewStateStore
import studio.forface.viewstatestore.ViewStateStoreConfig

/**
 * An interface meant to be implemented by a Test Suite that uses [ViewStateStore].
 * Example:
```
class MyClassTest : ViewStateStoreTest by viewStateStoreTest(myErrorStateGenerator) {
// test cases
}
```
 *
 * It provides an [ErrorStateGenerator] for [ViewStateStoreConfig].
 *
 * @author Davide Farella
 */
interface ViewStateStoreTest {

    /**
     * Declare which [ErrorStateGenerator] must be used for the test suite
     */
    val errorStateGenerator: ErrorStateGenerator
}

internal fun ViewStateStoreTest.init() {
    ViewStateStoreConfig.errorStateGenerator = errorStateGenerator
}

/** @see ViewStateStoreTest */
fun viewStateStoreTest(errorStateGenerator: ErrorStateGenerator) = object : ViewStateStoreTest {
    override val errorStateGenerator = errorStateGenerator
}.apply { init() }
