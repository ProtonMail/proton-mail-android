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

import assert4k.assert
import assert4k.equals
import assert4k.that
import kotlin.test.Test

/**
 * Test suite for String extensions
 * @author Davide Farella
 */
internal class StringExtensionsTest {

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
        assert that r1 equals "World what a beautiful"

        val r2 = input.substring(
            "world",
            "beautiful",
            startInclusive = false,
            endInclusive = false,
            ignoreCase = true
        )
        assert that r2 equals " what a "

        val r3 = input.substring(
            "world",
            "beautiful",
            startInclusive = true,
            endInclusive = true,
            ignoreCase = false
        )
        assert that r3 equals "Hello World what a beautiful"

        val r4 = input.substring(
            "beautiful",
            startInclusive = true
        )
        assert that r4 equals "beautiful day"
    }

    @Test
    fun obfuscate() {
        val input = "Hello world"

        val r1 = input.obfuscate()
        assert that r1 equals "*******rld"

        val r2 = input.obfuscate(keepFirst = 3, keepLast = 0)
        assert that r2 equals "Hel********"

        val r3 = input.obfuscate(keepFirst = 3, keepLast = 3)
        assert that r3 equals "Hel*****rld"

        val r4 = input.obfuscate('_')
        assert that r4 equals "________rld"
    }
}
