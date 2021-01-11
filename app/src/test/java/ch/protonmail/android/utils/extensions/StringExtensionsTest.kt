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
package ch.protonmail.android.utils.extensions

import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtensionsTest {

    @Test
    fun substring() {
        val input = "Hello World what a beautiful day"

        val r1 = input.substring(
            "world",
            "beautiful",
            startInclusive = true,
            endInclusive = true,
            ignoreCase = true
        )
        assertEquals("World what a beautiful", r1)

        val r2 = input.substring(
            "world",
            "beautiful",
            startInclusive = false,
            endInclusive = false,
            ignoreCase = true
        )
        assertEquals(" what a ", r2)

        val r3 = input.substring(
            "world",
            "beautiful",
            startInclusive = true,
            endInclusive = true,
            ignoreCase = false
        )
        assertEquals("Hello World what a beautiful", r3)

        val r4 = input.substring(
            "beautiful",
            startInclusive = true
        )
        assertEquals("beautiful day", r4)
    }
}
