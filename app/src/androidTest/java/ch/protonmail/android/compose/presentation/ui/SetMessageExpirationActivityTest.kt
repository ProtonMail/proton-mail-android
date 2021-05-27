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

package ch.protonmail.android.compose.presentation.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import com.google.android.material.textfield.TextInputEditText
import org.hamcrest.Matchers.`is`
import org.hamcrest.core.AllOf
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test suite for [SetMessageExpirationActivity]
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class SetMessageExpirationActivityTest {

    @get:Rule
    val activityScenarioRule = activityScenarioRule<SetMessageExpirationActivity>()

    @BeforeTest
    fun setup() {
        Intents.init()
    }

    // region inputs
    // TODO parametrized tests
    // endregion

    // region outputs
    @Test
    fun noneResultIsSetCorrectly() {
        // given
        val expectedDays = 0
        val expectedHours = 0

        // when
        onNoneSelected()
        performSetClick()

        // then
        assertResult(expectedDays, expectedHours)
    }

    @Test
    fun oneHourResultIsSetCorrectly() {
        // given
        val expectedDays = 0
        val expectedHours = 1

        // when
        onOneHourSelected()
        performSetClick()

        // then
        assertResult(expectedDays, expectedHours)
    }

    @Test
    fun oneDayResultIsSetCorrectly() {
        // given
        val expectedDays = 1
        val expectedHours = 0

        // when
        onOneDaySelected()
        performSetClick()

        // then
        assertResult(expectedDays, expectedHours)
    }

    @Test
    fun threeDaysResultIsSetCorrectly() {
        // given
        val expectedDays = 3
        val expectedHours = 0

        // when
        onThreeDaysSelected()
        performSetClick()

        // then
        assertResult(expectedDays, expectedHours)
    }

    @Test
    fun oneWeekResultIsSetCorrectly() {
        // given
        val expectedDays = 7
        val expectedHours = 0

        // when
        onOneWeekSelected()
        performSetClick()

        // then
        assertResult(expectedDays, expectedHours)
    }

    @Test
    fun customResultIsSetCorrectly() {
        // given
        val expectedDays = 4
        val expectedHours = 7

        // when
        onCustomSelected()
        setCustomDaysAndHours(expectedDays, expectedHours)
        performSetClick()

        // then
        assertResult(expectedDays, expectedHours)
    }
    // endregion

    private fun onNoneSelected(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_none_text_view)).perform(click())

    private fun onOneHourSelected(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_hour_text_view)).perform(click())

    private fun onOneDaySelected(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_day_text_view)).perform(click())

    private fun onThreeDaysSelected(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_3_days_text_view)).perform(click())

    private fun onOneWeekSelected(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_week_text_view)).perform(click())

    private fun onCustomSelected(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_custom_text_view)).perform(click())

    @Suppress("SameParameterValue")
    private fun setCustomDaysAndHours(days: Int, hours: Int) {
        onView(
            AllOf.allOf(
                withId(R.id.days_and_hours_picker_days_input),
                withClassName(`is`(TextInputEditText::class.qualifiedName))
            )
        ).perform(replaceText(days.toString()))

        onView(
            AllOf.allOf(
                withId(R.id.days_and_hours_picker_hours_input),
                withClassName(`is`(TextInputEditText::class.qualifiedName))
            )
        ).perform(replaceText(hours.toString()))
    }

    private fun performSetClick(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_set)).perform(click())

    private fun assertResult(expectedDays: Int, expectedHours: Int) {
        val resultIntent = activityScenarioRule.scenario.result.resultData
        assertThat(
            resultIntent,
            AllOf.allOf(
                hasExtra(ARG_SET_MESSAGE_EXPIRATION_DAYS, expectedDays),
                hasExtra(ARG_SET_MESSAGE_EXPIRATION_HOURS, expectedHours)
            )
        )
    }
}
