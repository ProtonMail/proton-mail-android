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

/**
 * Created by kadrikj on 9/13/18. */
fun String.setDefaultIfEmpty(defaultString: String): String {
    return if (isEmpty()) defaultString
    else this
}

fun String?.notNullOrEmpty(variableName: String = ""): String {
    val result = this.notNull(variableName)
    if (result.isEmpty())
        throw RuntimeException("$variableName is empty")
    return result
}

/**
 * @return [CharSequence]. If receiver [CharSequence] is shorted than the given [maxLength], return
 * itself. Else [CharSequence.substring] from 0 to [maxLength] and add "..." at the end of it.
 * E.g. >
 val hello = "Hello world!"
 println( hello.truncateToLength( 9 ) ) // Hello Wo...
 */
fun CharSequence.truncateToLength( maxLength: Int ) : CharSequence {
    return if ( length <= maxLength ) this
    else "${this[0, maxLength-1]}..."
}

/**
 * `get` operator for call [CharSequence.substring].
 * E.g. `charSequence[4, 10]`
 *
 * @return [CharSequence]
 */
operator fun CharSequence.get( from: Int, to: Int ) = this.subSequence( from, to )

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
fun CharSequence.substring(
        start: String? = null,
        end: String? = null,
        startIndex: Int = 0,
        endIndex: Int = length,
        startInclusive: Boolean = false,
        endInclusive: Boolean = false,
        ignoreCase: Boolean = false
) : CharSequence {
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
        .substring(start, end, startIndex, endIndex, startInclusive, endInclusive, ignoreCase)
        .toString()

fun Editable.removeWhitespaces() = toString().replace("\\s".toRegex(), "")

fun String.compare(other: String) = this == other