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
package ch.protonmail.android.utils

import android.content.Context
import android.content.res.Resources
import ch.protonmail.android.R
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

class DateUtilTest {

    private val resourcesMock = mockk<Resources> {
        every { getString(eq(R.string.expiration_days), any()) } answers {
            val days = secondArg<Array<Any>>()[0]
            "${days}D"
        }
        every { getString(eq(R.string.expiration_hours), any<Int>()) } answers {
            val hours = secondArg<Array<Any>>()[0]
            "${hours}H"
        }
        every { getString(eq(R.string.expiration_minutes), any<Int>()) } answers {
            val minutes = secondArg<Array<Any>>()[0]
            "${minutes}M"
        }
    }
    private val contextMock = mockk<Context> {
        every { resources } returns resourcesMock
    }

    @Test
    fun shouldFormatTheLargestAvailableUnitOnly() {
        // given
        val fiveDaysSixHoursAndSevenMinutes = 454_020L
        val sixHoursAndSevenMinutes = 22_020L
        val sevenMinutes = 420L
        val expectedDaysString = "5D"
        val expectedHoursString = "6H"
        val expectedMinutesString = "7M"

        // when
        val actualDaysString = DateUtil.formatTheLargestAvailableUnitOnly(contextMock, fiveDaysSixHoursAndSevenMinutes)
        val actualHoursString = DateUtil.formatTheLargestAvailableUnitOnly(contextMock, sixHoursAndSevenMinutes)
        val actualMinutesString = DateUtil.formatTheLargestAvailableUnitOnly(contextMock, sevenMinutes)

        // then
        assertEquals(expectedDaysString, actualDaysString)
        assertEquals(expectedHoursString, actualHoursString)
        assertEquals(expectedMinutesString, actualMinutesString)
    }
}
