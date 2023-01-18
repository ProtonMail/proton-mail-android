/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.compose.presentation.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import ch.protonmail.android.util.withProtonInputEditTextId
import org.hamcrest.core.AllOf
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Test suite for [SetMessageExpirationActivity]
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class SetMessageExpirationActivityTest {

    @BeforeTest
    fun setup() {
        Intents.init()
    }

    // region inputs
    @Test
    fun noneInputIsSetCorrectly() {
        // given
        val expectedDays = 0
        val expectedHours = 0

        // when
        launchActivityForResult(expectedDays, expectedHours)

        // then
        onNoneCheck().checkSelected()

        onOneHourCheck().checkNotSelected()
        onOneDayCheck().checkNotSelected()
        onThreeDaysCheck().checkNotSelected()
        onOneWeekCheck().checkNotSelected()
        onCustomCheck().checkNotSelected()
        onCustomPickerView().checkNotVisible()
    }

    @Test
    fun oneHourInputIsSetCorrectly() {
        // given
        val expectedDays = 0
        val expectedHours = 1

        // when
        launchActivityForResult(expectedDays, expectedHours)

        // then
        onNoneCheck().checkNotSelected()

        onOneHourCheck().checkSelected()

        onOneDayCheck().checkNotSelected()
        onThreeDaysCheck().checkNotSelected()
        onOneWeekCheck().checkNotSelected()
        onCustomCheck().checkNotSelected()
        onCustomPickerView().checkNotVisible()
    }

    @Test
    fun oneDayInputIsSetCorrectly() {
        // given
        val expectedDays = 1
        val expectedHours = 0

        // when
        launchActivityForResult(expectedDays, expectedHours)

        // then
        onNoneCheck().checkNotSelected()
        onOneHourCheck().checkNotSelected()

        onOneDayCheck().checkSelected()

        onThreeDaysCheck().checkNotSelected()
        onOneWeekCheck().checkNotSelected()
        onCustomCheck().checkNotSelected()
        onCustomPickerView().checkNotVisible()
    }

    @Test
    fun threeDaysInputIsSetCorrectly() {
        // given
        val expectedDays = 3
        val expectedHours = 0

        // when
        launchActivityForResult(expectedDays, expectedHours)

        // then
        onNoneCheck().checkNotSelected()
        onOneHourCheck().checkNotSelected()
        onOneDayCheck().checkNotSelected()

        onThreeDaysCheck().checkSelected()

        onOneWeekCheck().checkNotSelected()
        onCustomCheck().checkNotSelected()
        onCustomPickerView().checkNotVisible()
    }

    @Test
    fun oneWeekInputIsSetCorrectly() {
        // given
        val expectedDays = 7
        val expectedHours = 0

        // when
        launchActivityForResult(expectedDays, expectedHours)

        // then
        onNoneCheck().checkNotSelected()
        onOneHourCheck().checkNotSelected()
        onOneDayCheck().checkNotSelected()
        onThreeDaysCheck().checkNotSelected()

        onOneWeekCheck().checkSelected()

        onCustomCheck().checkNotSelected()
        onCustomPickerView().checkNotVisible()
    }

    // region outputs
    @Test
    fun noneResultIsSetCorrectly() {
        // given
        val expectedDays = 0
        val expectedHours = 0

        // when
        val scenario = launchActivityForResult()
        onNoneView().performSelection()
        performSetClick()

        // then
        assertResult(scenario, expectedDays, expectedHours)
    }

    @Test
    fun oneHourResultIsSetCorrectly() {
        // given
        val expectedDays = 0
        val expectedHours = 1

        // when
        val scenario = launchActivityForResult()
        onOneHourView().performSelection()
        performSetClick()

        // then
        assertResult(scenario, expectedDays, expectedHours)
    }

    @Test
    fun oneDayResultIsSetCorrectly() {
        // given
        val expectedDays = 1
        val expectedHours = 0

        // when
        val scenario = launchActivityForResult()
        onOneDayView().performSelection()
        performSetClick()

        // then
        assertResult(scenario, expectedDays, expectedHours)
    }

    @Test
    fun threeDaysResultIsSetCorrectly() {
        // given
        val expectedDays = 3
        val expectedHours = 0

        // when
        val scenario = launchActivityForResult()
        onThreeDaysView().performSelection()
        performSetClick()

        // then
        assertResult(scenario, expectedDays, expectedHours)
    }

    @Test
    fun oneWeekResultIsSetCorrectly() {
        // given
        val expectedDays = 7
        val expectedHours = 0

        // when
        val scenario = launchActivityForResult()
        onOneWeekView().performSelection()
        performSetClick()

        // then
        assertResult(scenario, expectedDays, expectedHours)
    }

    private fun assertResult(
        scenario: ActivityScenario<SetMessageExpirationActivity>,
        expectedDays: Int,
        expectedHours: Int
    ) {
        val resultIntent = scenario.result.resultData
        assertThat(
            resultIntent,
            AllOf.allOf(
                hasExtra(ARG_SET_MESSAGE_EXPIRATION_DAYS, expectedDays),
                hasExtra(ARG_SET_MESSAGE_EXPIRATION_HOURS, expectedHours)
            )
        )
    }
    // endregion

    private fun launchActivityForResult(
        extraExpirationDays: Int? = 0,
        extraExpirationHours: Int? = 0
    ) = ActivityScenario.launchActivityForResult<SetMessageExpirationActivity>(
        Intent(ApplicationProvider.getApplicationContext(), SetMessageExpirationActivity::class.java)
            .apply {
                extraExpirationDays?.let { putExtra(ARG_SET_MESSAGE_EXPIRATION_DAYS, it) }
                extraExpirationHours?.let { putExtra(ARG_SET_MESSAGE_EXPIRATION_HOURS, it) }
            }
    )

    private fun onNoneView(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_none_text_view))

    private fun onOneHourView(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_hour_text_view))

    private fun onOneDayView(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_day_text_view))

    private fun onThreeDaysView(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_3_days_text_view))

    private fun onOneWeekView(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_week_text_view))

    private fun onCustomView(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_custom_text_view))

    private fun onNoneCheck(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_none_check))

    private fun onOneHourCheck(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_hour_check))

    private fun onOneDayCheck(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_day_check))

    private fun onThreeDaysCheck(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_3_days_check))

    private fun onOneWeekCheck(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_1_week_check))

    private fun onCustomCheck(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_custom_check))

    private fun onCustomPickerView(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_picker_view))

    private fun onCustomDaysView(): ViewInteraction =
        onView(withProtonInputEditTextId(R.id.days_and_hours_picker_days_input))

    private fun onCustomHoursView(): ViewInteraction =
        onView(withProtonInputEditTextId(R.id.days_and_hours_picker_hours_input))

    private fun ViewInteraction.checkSelected(): ViewInteraction =
        checkVisible()

    private fun ViewInteraction.checkNotSelected(): ViewInteraction =
        checkNotVisible()

    private fun ViewInteraction.checkVisible(): ViewInteraction =
        check(matches(withEffectiveVisibility(ViewMatchers.Visibility.VISIBLE)))

    private fun ViewInteraction.checkNotVisible(): ViewInteraction =
        check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)))

    private fun ViewInteraction.matchesNumberText(number: Int): ViewInteraction =
        check(matches(withText(number.toString())))

    private fun ViewInteraction.performSelection(): ViewInteraction =
        perform(click())

    @Suppress("SameParameterValue")
    private fun setCustomDaysAndHours(days: Int, hours: Int) {
        onCustomDaysView().perform(replaceText(days.toString()))
        onCustomHoursView().perform(replaceText(hours.toString()))
    }

    private fun performSetClick(): ViewInteraction =
        onView(withId(R.id.set_msg_expiration_set)).perform(click())
}
