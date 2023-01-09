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

package ch.protonmail.android.ui.view

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import ch.protonmail.android.ui.view.DaysAndHoursPickerView.Companion.MAX_DAYS
import ch.protonmail.android.ui.view.DaysAndHoursPickerView.Companion.MAX_HOURS
import ch.protonmail.android.util.ViewTest
import ch.protonmail.android.util.withProtonInputEditTextId
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4ClassRunner::class)
class DaysAndHoursPickerViewTest : ViewTest<DaysAndHoursPickerView>(::DaysAndHoursPickerView), CoroutinesTest by CoroutinesTest() {

    // Days
    @Test
    fun daysAreNotChangedIfCorrect() {

        // given
        val input = 14

        // when
        testView.set(days = input, hours = 0)

        // then
        onDaysView().check(matches(withText("14")))
    }

    @Test
    fun daysAreSetToZeroIfBelowZero() {

        // given
        val input = -14

        // when
        testView.set(days = input, hours = 0)

        // then
        onDaysView().check(matches(withText("0")))
    }

    @Test
    fun daysAreSetToMaxIfTooHigh() {

        // given
        val input = 999

        // when
        testView.set(days = input, hours = 0)

        // then
        onDaysView().check(matches(withText("$MAX_DAYS")))
    }

    @Test
    fun daysAreSetToZeroIfInvalidInput() {

        // given
        val input = "hello"

        // when
        onDaysView().perform(ViewActions.typeText(input))

        // then
        onDaysView().check(matches(withText("0")))
    }

    // Hours
    @Test
    fun hoursAreNotChangedIfCorrect() {

        // given
        val input = 14

        // when
        testView.set(days = 0, hours = input)

        // then
        onHoursView().check(matches(withText("14")))
    }

    @Test
    fun hoursAreSetToZeroIfBelowZero() {

        // given
        val input = -14

        // when
        testView.set(days = 0, hours = input)

        // then
        onHoursView().check(matches(withText("0")))
    }

    @Test
    fun hoursAreSetToMaxIfTooHigh() {

        // given
        val input = 999

        // when
        testView.set(days = 0, hours = input)

        // then
        onHoursView().check(matches(withText("$MAX_HOURS")))
    }

    @Test
    fun hoursAreSetToZeroIfInvalidInput() {

        // given
        val input = "hello"

        // when
        onHoursView().perform(ViewActions.typeText(input))

        // then
        onHoursView().check(matches(withText("0")))
    }

    // Callback
    @Test
    fun callbackIsNotCalledIfSetSameHourOrDay() = runTest {

        // given
        val result = mutableListOf<DaysHoursPair>()
        val expected = listOf(
            DaysHoursPair(1, 1),
            DaysHoursPair(2, 2),
            DaysHoursPair(1, 1)
        )
        val job = launch {
            testView.onChange.toList(result)
        }

        // when
        testView.apply {
            set(1, 1)
            delay(100)
            set(2, 2)
            delay(100)
            set(2, 2)
            delay(100)
            set(2, 2)
            delay(100)
            set(1, 1)
            delay(100)
        }

        // then
        assertEquals(expected, result)
        job.cancel()
    }

    @Test
    fun callbackIsNotCalledForInvalidValues() = runTest {

        // given
        val result = mutableListOf<DaysHoursPair>()
        val expected = listOf(DaysHoursPair(MAX_DAYS, MAX_HOURS))
        val job = launch {
            testView.onChange.toList(result)
        }

        // when
        testView.set(99, 99)
        delay(100)

        // then
        assertEquals(expected, result)
        job.cancel()
    }


    private fun onDaysView(): ViewInteraction =
        onView(withProtonInputEditTextId(R.id.days_and_hours_picker_days_input))

    private fun onHoursView(): ViewInteraction =
        onView(withProtonInputEditTextId(R.id.days_and_hours_picker_hours_input))
}
