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

package ch.protonmail.android

import ch.protonmail.android.di.AlternativeApiPins
import ch.protonmail.android.di.BaseUrl
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import me.proton.core.humanverification.presentation.CaptchaApiHost
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import javax.inject.Inject
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@HiltAndroidTest
class ExampleCustomInjectionTest {

    @get:Rule(order = 0) // Ensures that the Hilt component is initialized before running the ActivityScenarioRule
    var hiltRule = HiltAndroidRule(this)

//    @get:Rule(order = 1)
//    val activityRule = activityScenarioRule<SplashActivity>()

    @Inject
    @BaseUrl
    lateinit var baseUrl: String

    @Inject
    @CaptchaApiHost
    lateinit var captchaApiHost: String

    @Inject
    @AlternativeApiPins
    lateinit var alternativePins: List<String>

    @BeforeTest
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun verifyBaseUrl() {
        assertThat(baseUrl).isNotNull
        assertEquals("http://localhost:8080/", baseUrl)
        assertEquals("localhost:8080", captchaApiHost)
        assertEquals(emptyList(), alternativePins)
    }
}
