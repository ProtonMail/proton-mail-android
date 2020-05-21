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
package ch.protonmail.android.testKotlin

import kotlin.test.*

/**
 * Asserts that the [expected] value is equal to the [actual] value, with a [message]
 * Note that the [message] is not evaluated lazily, the lambda is only cosmetic
 */
fun <T> assertEquals(expected: T, actual: T, message: () -> String) {
    assertEquals(expected, actual, message())
}

/** Asserts that the expression is `true` with a lazy message */
fun assertTrue(actual: Boolean, lazyMessage: () -> String) {
    asserter.assertTrue(lazyMessage, actual)
}

/**
 * Asserts that [actual] [`is`] [EXPECTED].
 * Ignored if [EXPECTED] is nullable and [actual] is null
 */
inline fun <reified EXPECTED> assertIs(actual: Any?) {
    // If `EXPECTED` is not nullable, assert that `actual` is not null
    if (null !is EXPECTED) assertNotNull(actual)

    if (null !is EXPECTED || actual != null)
        assertTrue(actual is EXPECTED) {
            // Usage of unsafe operator `!!` since if `EXPECTED` is not nullable, we already assert
            // that `actual` is not null
            "Expected to be '${EXPECTED::class.qualifiedName}'. " +
                    "Actual: '${actual!!::class.qualifiedName}'"
        }
}
