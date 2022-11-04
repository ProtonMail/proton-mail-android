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
package ch.protonmail.android.utils.extensions

import android.text.Editable
import ch.protonmail.android.domain.entity.EmailAddress
import me.proton.core.util.kotlin.EMPTY_STRING

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

private const val OBFUSCATE_DEFAULT_REPLACEMENT = '*'
private const val OBFUSCATE_DEFAULT_KEEP_FIRST = 0
private const val OBFUSCATE_DEFAULT_KEEP_LAST = 3

/**
 * Obfuscate a range from receiver [String]
 * @param replacement [Char] that will be used as replacement for chars to obfuscate
 *   Default is [OBFUSCATE_DEFAULT_REPLACEMENT]
 * @param keepFirst count of initial chars to do not obfuscate
 *   Default is [OBFUSCATE_DEFAULT_KEEP_FIRST]
 * @param keepLast count of final chars to do not obfuscate
 *   Default is [OBFUSCATE_DEFAULT_KEEP_LAST]
 */
fun String.obfuscate(
    replacement: Char = OBFUSCATE_DEFAULT_REPLACEMENT,
    keepFirst: Int = OBFUSCATE_DEFAULT_KEEP_FIRST,
    keepLast: Int = OBFUSCATE_DEFAULT_KEEP_LAST
) = mapIndexed { i, c ->
    if (i < keepFirst || i > length - keepLast - 1) c
    else replacement
}.joinToString(separator = EMPTY_STRING)

/**
 * Obfuscate a range from receiver **eMail** [String]
 * @throws IllegalArgumentException if receiver [String] is no an email
 * @see obfuscate
 */
fun String.obfuscateEmail(
    replacement: Char = OBFUSCATE_DEFAULT_REPLACEMENT,
    keepFirst: Int = OBFUSCATE_DEFAULT_KEEP_FIRST,
    keepLast: Int = OBFUSCATE_DEFAULT_KEEP_LAST
): String {
    require(matches(EmailAddress.VALIDATION_REGEX)) {
        "String is not an email"
    }
    val (id, host) = split("@")
    return "${id.obfuscate(replacement, keepFirst, keepLast)}@$host"
}

/**
 * Obfuscate a range from receiver **username or eMail** [String]
 * @see obfuscate
 */
fun String.obfuscateUsername(
    replacement: Char = OBFUSCATE_DEFAULT_REPLACEMENT,
    keepFirst: Int = OBFUSCATE_DEFAULT_KEEP_FIRST,
    keepLast: Int = OBFUSCATE_DEFAULT_KEEP_LAST
): String =
    if (matches(EmailAddress.VALIDATION_REGEX))
        obfuscateEmail(replacement, keepFirst, keepLast)
    else
        obfuscate(replacement, keepFirst, keepLast)

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


private const val NORMALIZE_DEFAULT_REPLACEMENT = '�'

/**
 * Obfuscate a range from receiver **username or eMail** [String]
 *
 */
fun String.normalizeString() = run {
    val newString = StringBuilder(this.length)
    this.forEach { character ->
        val codePoint: Int = character.code
        when (Character.getType(codePoint).toByte()) {
            Character.CONTROL,
            Character.FORMAT,
            Character.PRIVATE_USE,
            Character.SURROGATE,
            Character.UNASSIGNED -> newString.append(NORMALIZE_DEFAULT_REPLACEMENT)
            else -> newString.append(Character.toChars(codePoint))
        }
    }
    String(newString)
}
