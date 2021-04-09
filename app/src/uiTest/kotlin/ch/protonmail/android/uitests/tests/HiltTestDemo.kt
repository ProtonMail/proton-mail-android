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

package ch.protonmail.android.uitests.tests

import ch.protonmail.android.di.BaseUrl
import ch.protonmail.android.uitests.robots.login.LoginRobot
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import org.junit.Rule
import org.robolectric.annotation.Config
import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.test.Test

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
class HiltTestDemo {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private val loginRobot = LoginRobot()

    @Inject
    @BaseUrl
    lateinit var baseUrl: String

    @BeforeTest
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun justTestIt() {

    }


}
