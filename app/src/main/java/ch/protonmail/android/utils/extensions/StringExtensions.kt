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

import android.text.Editable
import me.proton.core.util.kotlin.EMPTY_STRING
import me.proton.core.util.kotlin.takeIfNotEmpty

@Deprecated(
    "Should not use this",
    ReplaceWith("takeIfNotEmpty() ?: defaultString", "me.proton.core.util.kotlin.takeIfNotEmpty")
)
fun String.setDefaultIfEmpty(defaultString: String) =
    takeIfNotEmpty() ?: defaultString

@Deprecated(
    "Should use regular Kotlin syntax",
    ReplaceWith("this ?: EMPTY_STRING", "me.proton.core.util.kotlin.EMPTY_STRING")
)
fun String?.notNullOrEmpty(variableName: String = "") =
    this ?: EMPTY_STRING

/**
 * @return [CharSequence]. If receiver [CharSequence] is shorted than the given [maxLength], return
 * itself. Else [CharSequence.substring] from 0 to [maxLength] and add "..." at the end of it.
 * E.g. >
 val hello = "Hello world!"
 println( hello.truncateToLength( 9 ) ) // Hello Wo...
 */
fun CharSequence.truncateToLength(maxLength: Int): CharSequence {
    return if (length <= maxLength) this
    else "${this[0, maxLength - 1]}..."
}

/**
 * `get` operator for call [CharSequence.substring].
 * E.g. `charSequence[4, 10]`
 *
 * @return [CharSequence]
 */
operator fun CharSequence.get(from: Int, to: Int) = this.subSequence(from, to)

/**
 * Subsequence the receiver [CharSequence]
 *
 * @param start [String] where to start substring-ing, optionally define a [startIndex] for exclude
 * matching before the given index
 * If [String] is not found, [startIndex] will be used as start
 * Default is `null`
 *
 * @param end [String] where to stop substring-ing, optionally define an [endIndex] for exclusive
 * matching after the given index
 * If [String] is not found, [endIndex] will be used as end
 *
 * @param startIndex [Int] Default is `0`
 * @param endIndex [Int] Default is [String.lastIndex]
 *
 * @param startInclusive [Boolean] whether the given [start] [String] must be included in the result
 * Default is `false`
 *
 * @param endInclusive [Boolean] whether the given [end] [String] must be included in the result
 * Default is `false`
 *
 * @param ignoreCase [Boolean] whether [start] and [end] must be matched ignoring their case
 * Default is `false`
 */
fun CharSequence.subsequence(
    start: String? = null,
    end: String? = null,
    startIndex: Int = 0,
    endIndex: Int = length,
    startInclusive: Boolean = false,
    endInclusive: Boolean = false,
    ignoreCase: Boolean = false
): CharSequence {
    // Calculate the index where to start substring-ing
    val from = if (start != null) {
        val relative = indexOf(start, startIndex, ignoreCase)
        when {
            relative == -1 -> startIndex
            startInclusive -> relative
            else -> relative + start.length
        }
    } else startIndex

    // Calculate the index where to stop substring-ing
    val to = if (end != null) {
        val trimToEnd = get(0, endIndex)
        val relative = trimToEnd.indexOf(end, from, ignoreCase = ignoreCase)
        when {
            relative == -1 -> endIndex
            endInclusive -> relative + end.length
            else -> relative
        }
    } else endIndex

    return get(from, to.coerceAtLeast(from))
}

/**
 * Remove whitespaces from receiver [Editable]
 * @return [String]
 */
fun Editable.removeWhitespaces(): String =
    toString().replace("\\s".toRegex(), "")

/**
 * Obfuscate a rance from receiver [String]
 * @param replacement [Char] that will be used as replacement for chars to obfuscate
 * @param keepFirst count of initial chars to do not obfuscate
 *   Default is `0`
 * @param keepLast count of final chars to do not obfuscate
 *   Default is `3`
 */
fun String.obfuscate(replacement: Char = '*', keepFirst: Int = 0, keepLast: Int = 3) =
    mapIndexed { i, c ->
        if (i < keepFirst || i > length - keepLast) c
        else replacement
    }.joinToString(separator = EMPTY_STRING)

/**
 * Substring the receiver [CharSequence]
 *
 * @param start [String] where to start substring-ing, optionally define a [startIndex] for exclude
 * matching before the given index
 * If [String] is not found, [startIndex] will be used as start
 * Default is `null`
 *
 * @param end [String] where to stop substring-ing, optionally define an [endIndex] for exclusive
 * matching after the given index
 * If [String] is not found, [endIndex] will be used as end
 *
 * @param startIndex [Int] Default is `0`
 * @param endIndex [Int] Default is [String.lastIndex]
 *
 * @param startInclusive [Boolean] whether the given [start] [String] must be included in the result
 * Default is `false`
 *
 * @param endInclusive [Boolean] whether the given [end] [String] must be included in the result
 * Default is `false`
 *
 * @param ignoreCase [Boolean] whether [start] and [end] must be matched ignoring their case
 * Default is `false`
 */
fun String.substring(
    start: String? = null,
    end: String? = null,
    startIndex: Int = 0,
    endIndex: Int = length,
    startInclusive: Boolean = false,
    endInclusive: Boolean = false,
    ignoreCase: Boolean = false
) = (this as CharSequence)
        .subsequence(start, end, startIndex, endIndex, startInclusive, endInclusive, ignoreCase)
        .toString()
