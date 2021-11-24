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

package ch.protonmail.android.ui.view

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.ui.view.DaysAndHoursPickerView.Companion.MAX_DAYS
import ch.protonmail.android.ui.view.DaysAndHoursPickerView.Companion.MAX_HOURS
import ch.protonmail.android.util.ViewTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test suite for [DaysAndHoursPickerView]
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class DaysAndHoursPickerViewTest : ViewTest<DaysAndHoursPickerView>(::DaysAndHoursPickerView), CoroutinesTest {

    // Callback
    @Test
    fun callbackIsNotCalledIfSetSameHourOrDay() = coroutinesTest {

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
    fun callbackIsNotCalledForInvalidValues() = coroutinesTest {

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

}
