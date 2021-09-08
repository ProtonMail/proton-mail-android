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
package ch.protonmail.android.utils

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import kotlin.test.assertEquals

class DateUtilTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun shouldFormatTheLargestAvailableUnitOnly() {
        val fiveDaysSixHoursAndSevenMinutes = 454_020L
        val sixHoursAndSevenMinutes = 22_020L
        val sevenMinutes = 420L
        val expectedDaysString = "5D"
        val expectedHoursString = "6H"
        val expectedMinutesString = "7M"

        val actualDaysString = DateUtil.formatTheLargestAvailableUnitOnly(context, fiveDaysSixHoursAndSevenMinutes)
        val actualHoursString = DateUtil.formatTheLargestAvailableUnitOnly(context, sixHoursAndSevenMinutes)
        val actualMinutesString = DateUtil.formatTheLargestAvailableUnitOnly(context, sevenMinutes)

        assertEquals(expectedDaysString, actualDaysString)
        assertEquals(expectedHoursString, actualHoursString)
        assertEquals(expectedMinutesString, actualMinutesString)
    }
}
